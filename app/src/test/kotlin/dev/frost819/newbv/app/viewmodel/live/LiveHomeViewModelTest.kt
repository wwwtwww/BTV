package dev.frost819.newbv.app.viewmodel.live

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveResponse
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveRoom
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveRecommendResponse
import dev.frost819.newbv.biliapi.http.entity.live.LiveRoomItem
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [LiveHomeViewModel] 的单元测试。
 *
 * 覆盖三个核心加载逻辑（关注直播、分区列表、推荐列表）的成功/失败路径，
 * 以及 refresh 时旧 Job 取消、loadMoreRecommend 去重和守卫条件。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LiveHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var liveRepository: LiveRepository
    private lateinit var viewModel: LiveHomeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        liveRepository = mockk(relaxed = true)

        // init 会调用三个加载方法，提供默认空返回值
        coEvery { liveRepository.getFollowLive() } returns FollowLiveResponse()
        coEvery { liveRepository.getLiveAreaList() } returns emptyList()
        coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse()
    }

    @AfterEach
    fun tearDown() {
        runCatching { viewModel.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    // ── init 加载 ──────────────────────────────────────────

    @Test
    fun `init loads follow live, area list, and recommend`() =
        runTest(testDispatcher) {
            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { liveRepository.getFollowLive() }
            coVerify(exactly = 1) { liveRepository.getLiveAreaList() }
            coVerify(exactly = 1) { liveRepository.getLiveRecommend() }

            assertThat(viewModel.uiState.value.followLoading).isFalse()
            assertThat(viewModel.uiState.value.areaLoading).isFalse()
            assertThat(viewModel.uiState.value.recommendLoading).isFalse()
        }

    // ── loadFollowLive ──────────────────────────────────────

    @Test
    fun `loadFollowLive success filters living rooms and maps to card data`() =
        runTest(testDispatcher) {
            // Given: 3 个房间，只有 2 个 liveStatus == 1
            val rooms = listOf(
                FollowLiveRoom(
                    roomId = 100,
                    title = "直播中A",
                    uid = 1,
                    online = 5000,
                    liveStatus = 1,
                    uname = "主播A",
                    face = "face_a",
                    coverFromUser = "cover_a",
                    keyframe = "kf_a",
                    areaV2Name = "分区A",
                    areaV2ParentName = "父分区A",
                ),
                FollowLiveRoom(
                    roomId = 101,
                    title = "未开播",
                    uid = 2,
                    online = 0,
                    liveStatus = 0,
                    uname = "主播B",
                ),
                FollowLiveRoom(
                    roomId = 102,
                    title = "直播中C",
                    uid = 3,
                    online = 12000,
                    liveStatus = 1,
                    uname = "主播C",
                    face = "face_c",
                    coverFromUser = "",
                    keyframe = "kf_c",
                    areaV2Name = "分区C",
                    areaV2ParentName = "父分区C",
                    nickname = "昵称C",
                ),
            )
            coEvery { liveRepository.getFollowLive() } returns FollowLiveResponse(rooms = rooms)

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then: 只保留 liveStatus == 1 的 2 个房间
            assertThat(viewModel.uiState.value.followItems).hasSize(2)
            assertThat(viewModel.uiState.value.followItems[0].roomId).isEqualTo(100L)
            assertThat(viewModel.uiState.value.followItems[1].roomId).isEqualTo(102L)
            assertThat(viewModel.uiState.value.followError).isFalse()
        }

    @Test
    fun `loadFollowLive error sets followError true`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getFollowLive() } throws RuntimeException("网络错误")

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.followLoading).isFalse()
            assertThat(viewModel.uiState.value.followError).isTrue()
            assertThat(viewModel.uiState.value.followItems).isEmpty()
        }

    @Test
    fun `loadFollowLive cancels previous job on refresh`() =
        runTest(testDispatcher) {
            // Given: 第一次正常加载
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // When: 再次调用 loadFollowLive
            coEvery { liveRepository.getFollowLive() } returns FollowLiveResponse(
                rooms = listOf(
                    FollowLiveRoom(roomId = 200, title = "新直播", liveStatus = 1, uname = "新主播"),
                ),
            )
            viewModel.loadFollowLive()
            advanceUntilIdle()

            // Then: 第二次结果覆盖第一次
            assertThat(viewModel.uiState.value.followItems).hasSize(1)
            assertThat(viewModel.uiState.value.followItems[0].roomId).isEqualTo(200L)
        }

    // ── loadAreaList ────────────────────────────────────────

    @Test
    fun `loadAreaList success populates area list`() =
        runTest(testDispatcher) {
            val areas = listOf(
                LiveAreaParent(id = 1, name = "娱乐"),
                LiveAreaParent(id = 2, name = "游戏"),
            )
            coEvery { liveRepository.getLiveAreaList() } returns areas

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.areaList).hasSize(2)
            assertThat(viewModel.uiState.value.areaList[0].name).isEqualTo("娱乐")
            assertThat(viewModel.uiState.value.areaError).isFalse()
        }

    @Test
    fun `loadAreaList error sets areaError true`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getLiveAreaList() } throws RuntimeException("超时")

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.areaLoading).isFalse()
            assertThat(viewModel.uiState.value.areaError).isTrue()
            assertThat(viewModel.uiState.value.areaList).isEmpty()
        }

    @Test
    fun `loadAreaList skips when already loading`() =
        runTest(testDispatcher) {
            // Given: 构造 areaLoading = true 的状态
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // 通过反射设置 areaLoading = true，模拟正在加载
            setUiStateField { it.copy(areaLoading = true) }

            // When: 再次调用 loadAreaList
            viewModel.loadAreaList()
            advanceUntilIdle()

            // Then: getLiveAreaList 只在 init 时调用了一次
            coVerify(exactly = 1) { liveRepository.getLiveAreaList() }
        }

    // ── loadRecommend ───────────────────────────────────────

    @Test
    fun `loadRecommend success populates recommend items`() =
        runTest(testDispatcher) {
            val items = listOf(
                LiveRoomItem(
                    roomId = 300,
                    uid = 10,
                    title = "推荐直播A",
                    uname = "推荐主播A",
                    online = 8000,
                    cover = "cover_a",
                    face = "face_a",
                    areaV2Name = "分区A",
                    areaV2ParentName = "父分区A",
                ),
                LiveRoomItem(
                    roomId = 301,
                    uid = 11,
                    title = "推荐直播B",
                    uname = "推荐主播B",
                    online = 3000,
                    cover = "",
                    keyframe = "kf_b",
                    face = "face_b",
                    areaV2Name = "分区B",
                    areaV2ParentName = "父分区B",
                ),
            )
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = items)

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.recommendItems).hasSize(2)
            assertThat(viewModel.uiState.value.recommendItems[0].roomId).isEqualTo(300L)
            assertThat(viewModel.uiState.value.recommendItems[1].roomId).isEqualTo(301L)
            assertThat(viewModel.uiState.value.recommendHasMore).isTrue()
            assertThat(viewModel.uiState.value.recommendError).isFalse()
        }

    @Test
    fun `loadRecommend empty result sets hasMore false`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = emptyList())

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.recommendItems).isEmpty()
            assertThat(viewModel.uiState.value.recommendHasMore).isFalse()
        }

    @Test
    fun `loadRecommend error sets recommendError true`() =
        runTest(testDispatcher) {
            coEvery { liveRepository.getLiveRecommend() } throws RuntimeException("服务器错误")

            // When
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.recommendLoading).isFalse()
            assertThat(viewModel.uiState.value.recommendError).isTrue()
        }

    // ── loadMoreRecommend ───────────────────────────────────

    @Test
    fun `loadMoreRecommend skips when already loading`() =
        runTest(testDispatcher) {
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // 设置 recommendLoading = true
            setUiStateField { it.copy(recommendLoading = true) }

            // When
            viewModel.loadMoreRecommend()
            advanceUntilIdle()

            // Then: getLiveRecommend 只在 init 的 loadRecommend 中调用了一次
            coVerify(exactly = 1) { liveRepository.getLiveRecommend() }
        }

    @Test
    fun `loadMoreRecommend skips when hasMore is false`() =
        runTest(testDispatcher) {
            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // 设置 recommendHasMore = false
            setUiStateField { it.copy(recommendHasMore = false) }

            // When
            viewModel.loadMoreRecommend()
            advanceUntilIdle()

            // Then: getLiveRecommend 只在 init 的 loadRecommend 中调用了一次
            coVerify(exactly = 1) { liveRepository.getLiveRecommend() }
        }

    @Test
    fun `loadMoreRecommend deduplicates by roomId`() =
        runTest(testDispatcher) {
            // Given: init 加载了 roomId=300
            val initItems = listOf(
                LiveRoomItem(
                    roomId = 300, uid = 10, title = "已有", uname = "主播",
                    online = 100, cover = "", face = "",
                ),
            )
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = initItems)

            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.recommendItems).hasSize(1)

            // When: loadMore 返回一个已有的 + 一个新的
            val newBatch = listOf(
                LiveRoomItem(
                    roomId = 300, uid = 10, title = "重复", uname = "主播",
                    online = 100, cover = "", face = "",
                ),
                LiveRoomItem(
                    roomId = 301, uid = 11, title = "新的", uname = "新主播",
                    online = 200, cover = "", face = "",
                ),
            )
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = newBatch)

            viewModel.loadMoreRecommend()
            advanceUntilIdle()

            // Then: 去重后只有 2 个（300 + 301）
            assertThat(viewModel.uiState.value.recommendItems).hasSize(2)
            assertThat(viewModel.uiState.value.recommendItems.map { it.roomId }).containsExactly(300L, 301L)
        }

    @Test
    fun `loadMoreRecommend stops after 3 attempts when all duplicates`() =
        runTest(testDispatcher) {
            // Given: init 加载了 roomId=300
            val initItems = listOf(
                LiveRoomItem(
                    roomId = 300, uid = 10, title = "已有", uname = "主播",
                    online = 100, cover = "", face = "",
                ),
            )
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = initItems)

            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // When: loadMore 每次都只返回已有的
            val duplicateBatch = listOf(
                LiveRoomItem(
                    roomId = 300, uid = 10, title = "重复", uname = "主播",
                    online = 100, cover = "", face = "",
                ),
            )
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = duplicateBatch)

            viewModel.loadMoreRecommend()
            advanceUntilIdle()

            // Then: 最多重试 3 次后停止，hasMore = false
            assertThat(viewModel.uiState.value.recommendHasMore).isFalse()
            // init 1 次 + loadMore 最多 3 次
            coVerify(atMost = 4) { liveRepository.getLiveRecommend() }
        }

    @Test
    fun `loadMoreRecommend error sets recommendError true`() =
        runTest(testDispatcher) {
            // Given: init 返回至少一项，确保 recommendHasMore = true
            val initItems = listOf(
                LiveRoomItem(
                    roomId = 100, uid = 1, title = "初始", uname = "主播",
                    online = 100, cover = "", face = "",
                ),
            )
            coEvery { liveRepository.getLiveRecommend() } returns LiveRecommendResponse(list = initItems)

            viewModel = LiveHomeViewModel(liveRepository)
            advanceUntilIdle()

            // When: loadMore 抛出异常
            coEvery { liveRepository.getLiveRecommend() } throws RuntimeException("网络错误")

            viewModel.loadMoreRecommend()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.uiState.value.recommendLoading).isFalse()
            assertThat(viewModel.uiState.value.recommendError).isTrue()
        }

    // ── Helpers ──────────────────────────────────────────────

    private fun setUiStateField(transform: (LiveHomeUiState) -> LiveHomeUiState) {
        val field = LiveHomeViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<LiveHomeUiState>
        stateFlow.value = transform(stateFlow.value)
    }
}
