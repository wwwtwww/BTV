package dev.frost819.newbv.app.viewmodel.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.app.data.AccountUiState
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.biliapi.entity.home.RecommendData
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.home.VideoBriefMeta
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoData
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.user.DynamicVideo
import dev.frost819.newbv.biliapi.entity.user.DynamicVideoData
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.RecommendVideoRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.io.IOException

/**
 * [HomeViewModel] 的单元测试。
 *
 * 验证推荐/热门/动态数据加载、分页、刷新逻辑，以及顶部 Hero Showcase
 * 的首屏种子、400ms 获焦防抖、快速游走取消、LRU 缓存与低配静态模式跳过网络补全逻辑。
 * 使用 MockK mock [RecommendVideoRepository] 和 [UserRepository]。
 * Prefs 初始化一次，每个测试前 clear 重置。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var recommendRepo: RecommendVideoRepository
    private lateinit var userRepo: UserRepository
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var viewModel: HomeViewModel

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_home_vm", ".preferences_pb")
            file.deleteOnExit()
            testDataStore =
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = { file },
                )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Leave Prefs initialized to avoid async write exceptions
        }
    }

    private fun fakeUgcItem(aid: Long) =
        UgcItem(
            aid = aid,
            bvid = "BV$aid",
            title = "video $aid",
            cover = "http://example.com/cover.jpg",
            author = "up",
            authorMid = 100L,
            play = 10000,
            danmaku = 500,
            duration = 120,
        )

    private fun fakeDynamicVideo(aid: Long) =
        DynamicVideo(
            aid = aid,
            cid = aid * 10,
            title = "dynamic $aid",
            cover = "http://example.com/cover.jpg",
            author = "up",
            authorMid = 100L,
            duration = 120,
            play = 10000,
            danmaku = 500,
        )

    private fun fakeCard(
        aid: Long,
        epid: Int? = null,
        tagName: String? = null,
        description: String? = null,
    ) = VideoCardData(
        avid = aid,
        bvid = "BV$aid",
        epid = epid,
        title = "card $aid",
        cover = "https://i0.hdslb.com/bfs/archive/$aid.jpg",
        upName = "UP_$aid",
        playString = "1.2万",
        danmakuString = "233",
        timeString = "05:20",
        tagName = tagName,
        description = description,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { Prefs.clear() }

        recommendRepo = mockk()
        userRepo = mockk()
        accountRepo = mockk()
        every { accountRepo.uiState } returns MutableStateFlow(AccountUiState())

        coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
            RecommendData(
                items = listOf(fakeUgcItem(1), fakeUgcItem(2)),
                nextPage = RecommendPage(),
            )
        coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
            PopularVideoData(
                list = listOf(fakeUgcItem(3), fakeUgcItem(4)),
                nextPage = PopularVideoPage(),
                noMore = false,
            )
        coEvery { recommendRepo.getVideoBriefMeta(any(), any()) } returns null
        coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
            DynamicVideoData(
                videos = listOf(fakeDynamicVideo(5), fakeDynamicVideo(6)),
                hasMore = true,
                historyOffset = "offset1",
                updateBaseline = "baseline1",
            )
        BiliHttpApi.wbiImgKey = "7cd084941338484aae1ad9425b81277e"
        BiliHttpApi.wbiSubKey = "433010b9f1d0442eaec4b4c7c8c8371a"
    }

    private fun createViewModel() = HomeViewModel(recommendRepo, userRepo, accountRepo)

    private fun currentHero() = viewModel.uiState.value.heroShowcase

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        BiliHttpApi.wbiImgKey = null
        BiliHttpApi.wbiSubKey = null
    }

    @Test
    fun `init loads recommend and popular and seeds initial heroShowcase`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.popularItems).isNotEmpty()
            assertThat(state.heroShowcase).isNotNull()
            assertThat(state.heroShowcase?.avid).isEqualTo(1L)
            assertThat(state.heroShowcase?.sourceShelfTitle).isEqualTo("为你推荐")
        }

    @Test
    fun `init seeds heroShowcase from popular when recommend fails`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws IOException("offline")

            viewModel = createViewModel()
            advanceUntilIdle()

            val hero = viewModel.uiState.value.heroShowcase
            assertThat(hero).isNotNull()
            assertThat(hero?.avid).isEqualTo(3L)
            assertThat(hero?.sourceShelfTitle).isEqualTo("今日热门")
        }

    @Test
    fun `onHeroCandidateFocused enforces 400ms debounce before switching`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(currentHero()?.avid).isEqualTo(1L)

            viewModel.onHeroCandidateFocused(fakeCard(aid = 10L), shelfTitle = "为你推荐")

            // 399ms 内不应提前切换背景看板
            advanceTimeBy(HERO_FOCUS_DEBOUNCE_MS - 1L)
            runCurrent()
            assertThat(currentHero()?.avid).isEqualTo(1L)

            // 满 400ms 后正式切换
            advanceTimeBy(1L)
            runCurrent()
            assertThat(currentHero()?.avid).isEqualTo(10L)
        }

    @Test
    fun `rapid focus moves cancel intermediate hero updates and only enrich final card`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getVideoBriefMeta(12L, "BV12") } returns
                VideoBriefMeta(
                    aid = 12L,
                    tagName = "科技数码",
                    description = "最终停留卡片的详细简介",
                )

            viewModel = createViewModel()
            advanceUntilIdle()

            // 模拟遥控器每隔 150ms 快速横移 3 张卡片
            viewModel.onHeroCandidateFocused(fakeCard(aid = 10L), shelfTitle = "为你推荐")
            advanceTimeBy(150L)
            runCurrent()
            viewModel.onHeroCandidateFocused(fakeCard(aid = 11L), shelfTitle = "为你推荐")
            advanceTimeBy(150L)
            runCurrent()
            viewModel.onHeroCandidateFocused(fakeCard(aid = 12L), shelfTitle = "今日热门")

            // 中途从未闪烁到 10 或 11
            assertThat(currentHero()?.avid).isEqualTo(1L)

            advanceTimeBy(HERO_FOCUS_DEBOUNCE_MS)
            advanceUntilIdle()

            val hero = currentHero()
            assertThat(hero?.avid).isEqualTo(12L)
            assertThat(hero?.sourceShelfTitle).isEqualTo("今日热门")
            assertThat(hero?.tagName).isEqualTo("科技数码")
            assertThat(hero?.description).isEqualTo("最终停留卡片的详细简介")
            coVerify(exactly = 0) { recommendRepo.getVideoBriefMeta(10L, any()) }
            coVerify(exactly = 0) { recommendRepo.getVideoBriefMeta(11L, any()) }
            coVerify(exactly = 1) { recommendRepo.getVideoBriefMeta(12L, "BV12") }
        }

    @Test
    fun `onHeroCandidateFocused reuses LRU brief meta cache without duplicate network call`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getVideoBriefMeta(21L, "BV21") } returns
                VideoBriefMeta(
                    aid = 21L,
                    tagName = "单机游戏",
                    description = "缓存测试简介",
                )

            viewModel = createViewModel()
            advanceUntilIdle()

            // 首次停留 21L：触发一次网络补全
            viewModel.onHeroCandidateFocused(fakeCard(aid = 21L), shelfTitle = "为你推荐")
            advanceUntilIdle()
            assertThat(currentHero()?.tagName).isEqualTo("单机游戏")

            // 移走焦点到已带完整元数据的 22L
            viewModel.onHeroCandidateFocused(
                fakeCard(aid = 22L, tagName = "音乐", description = "自带完整简介"),
                shelfTitle = "今日热门",
            )
            advanceUntilIdle()
            assertThat(currentHero()?.avid).isEqualTo(22L)

            // 再次回到 21L：命中内存 LRU 缓存，不重复请求 getVideoBriefMeta
            viewModel.onHeroCandidateFocused(fakeCard(aid = 21L), shelfTitle = "为你推荐")
            advanceUntilIdle()

            val hero = currentHero()
            assertThat(hero?.avid).isEqualTo(21L)
            assertThat(hero?.tagName).isEqualTo("单机游戏")
            assertThat(hero?.description).isEqualTo("缓存测试简介")
            coVerify(exactly = 1) { recommendRepo.getVideoBriefMeta(21L, "BV21") }
            coVerify(exactly = 0) { recommendRepo.getVideoBriefMeta(22L, any()) }
        }

    @Test
    fun `onHeroCandidateFocused skips network enrich in static degraded mode and for pgc cards`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            // 低配静态降级模式：allowNetworkEnrich = false
            viewModel.onHeroCandidateFocused(
                card = fakeCard(aid = 30L),
                shelfTitle = "为你推荐",
                allowNetworkEnrich = false,
            )
            advanceUntilIdle()
            assertThat(currentHero()?.avid).isEqualTo(30L)
            coVerify(exactly = 0) { recommendRepo.getVideoBriefMeta(30L, any()) }

            // PGC 番剧动态卡片：不请求 UGC 详情接口
            viewModel.onHeroCandidateFocused(
                card = fakeCard(aid = 31L, epid = 5001),
                shelfTitle = "关注动态",
                allowNetworkEnrich = true,
            )
            advanceUntilIdle()
            assertThat(currentHero()?.avid).isEqualTo(31L)
            assertThat(currentHero()?.tagName).isEqualTo("番剧影视")
            coVerify(exactly = 0) { recommendRepo.getVideoBriefMeta(31L, any()) }
        }

    @Test
    fun `init does not load dynamics when not logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.dynamicItems).isEmpty()
            assertThat(state.isLogin).isFalse()
        }

    @Test
    fun `refreshRecommend clears and reloads`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(100)),
                    nextPage = RecommendPage(),
                )

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // loadRecommend loops until 24 items or 3 loads, so 3 × 1 = 3 items
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.recommendItems[0].aid).isEqualTo(100)
            assertThat(state.heroShowcase?.avid).isEqualTo(100L)
        }

    @Test
    fun `refreshRecommend cancels in-flight load and reloads fresh data`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } coAnswers {
                kotlinx.coroutines.delay(1000)
                RecommendData(
                    items = listOf(fakeUgcItem(999)),
                    nextPage = RecommendPage(),
                )
            }

            viewModel.loadRecommend()
            assertThat(viewModel.uiState.value.recommendLoading).isTrue()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(111)),
                    nextPage = RecommendPage(),
                )

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.recommendItems.none { it.aid == 999L }).isTrue()
            assertThat(state.recommendItems.first().aid).isEqualTo(111L)
        }

    @Test
    fun `refreshPopular clears and reloads`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(200)),
                    nextPage = PopularVideoPage(),
                    noMore = true,
                )

            viewModel.refreshPopular()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularItems).hasSize(1)
            assertThat(state.popularItems[0].aid).isEqualTo(200)
            assertThat(state.popularHasMore).isFalse()
        }

    @Test
    fun `loadRecommend on error sets loading false and error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws RuntimeException("Network error")

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendLoading).isFalse()
            assertThat(state.recommendError).isTrue()
        }

    @Test
    fun `loadPopular on error sets loading false and error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } throws RuntimeException("Network error")

            viewModel.refreshPopular()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularLoading).isFalse()
            assertThat(state.popularError).isTrue()
        }

    @Test
    fun `loadRecommend on timeout sets error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws IOException("timeout")

            viewModel.refreshRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendLoading).isFalse()
            assertThat(state.recommendError).isTrue()
        }

    @Test
    fun `loadPopular on timeout sets error true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } throws IOException("timeout")

            viewModel.refreshPopular()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularLoading).isFalse()
            assertThat(state.popularError).isTrue()
        }

    @Test
    fun `refreshRecommend clears error`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws RuntimeException("error")
            viewModel.refreshRecommend()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.recommendError).isTrue()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(1)),
                    nextPage = RecommendPage(),
                )
            viewModel.refreshRecommend()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendError).isFalse()
            assertThat(viewModel.uiState.value.recommendItems).isNotEmpty()
        }

    @Test
    fun `refreshDynamic does nothing when not logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refreshDynamic()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.dynamicItems).isEmpty()
        }

    @Test
    fun `updateLoginState to true triggers dynamic load`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLogin).isTrue()
            assertThat(state.dynamicItems).isNotEmpty()
        }

    @Test
    fun `updateLoginState to false clears dynamics`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.updateLoginState(true)
            advanceUntilIdle()
            viewModel.updateLoginState(false)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLogin).isFalse()
            assertThat(state.dynamicItems).isEmpty()
        }

    @Test
    fun `refresh dispatches correct tab`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(999)),
                    nextPage = RecommendPage(),
                )

            viewModel.refresh(dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.recommendItems[0]
                    .aid,
            ).isEqualTo(999)
        }

    @Test
    fun `loadMore dispatches correct tab`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(888)),
                    nextPage = PopularVideoPage(),
                    noMore = false,
                )

            viewModel.loadMore(dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.popularItems.any { it.aid == 888L }).isTrue()
        }

    @Test
    fun `loadDynamic loads items when logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10), fakeDynamicVideo(11)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLogin).isTrue()
            assertThat(state.dynamicItems).hasSize(2)
            assertThat(state.dynamicLoading).isFalse()
            assertThat(state.dynamicHasMore).isFalse()
        }

    @Test
    fun `loadDynamic sets error on failure`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } throws RuntimeException("network error")

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.dynamicError).isTrue()
            assertThat(state.dynamicLoading).isFalse()
        }

    @Test
    fun `loadDynamic is no-op when hasMore is false`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)
            assertThat(viewModel.uiState.value.dynamicHasMore).isFalse()

            viewModel.loadDynamic()
            advanceUntilIdle()

            coVerify(exactly = 1) { userRepo.getDynamicVideos(any(), any(), any(), any()) }
        }

    @Test
    fun `loadRecommend loops until 24 items`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = (1..10).map { fakeUgcItem(it.toLong()) },
                    nextPage = RecommendPage(),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendItems.size).isAtLeast(20)
        }

    @Test
    fun `loadRecommend with existing items appends exactly one more page`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            // 首次加载：列表为空，最多补齐 3 页（每页 2 条），共 6 条
            assertThat(viewModel.uiState.value.recommendItems).hasSize(6)
            coVerify(exactly = 3) { recommendRepo.getRecommendVideos(any(), any()) }

            viewModel.loadRecommend()
            advanceUntilIdle()

            // 已有数据时「加载更多」只追加一页，而非空转（issue #286）
            assertThat(viewModel.uiState.value.recommendItems).hasSize(8)
            coVerify(exactly = 4) { recommendRepo.getRecommendVideos(any(), any()) }
        }

    @Test
    fun `loadRecommend does not advance cursor on transient empty page allowing retry`() =
        runTest(testDispatcher) {
            val page1 = RecommendPage(nextWebIdx = 1)
            val page2 = RecommendPage(nextWebIdx = 2)

            // 首次请求：返回空数据（transient empty）
            coEvery { recommendRepo.getRecommendVideos(page1, any()) } returns
                RecommendData(
                    items = emptyList(),
                    nextPage = page2,
                )
            coEvery { recommendRepo.getRecommendVideos(page2, any()) } returns
                RecommendData(
                    items = emptyList(),
                    nextPage = page2,
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendItems).isEmpty()

            // 再次请求：游标应保持 page1，而不是推进到 page2
            coEvery { recommendRepo.getRecommendVideos(page1, any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(100)),
                    nextPage = page2,
                )
            viewModel.loadRecommend()
            advanceUntilIdle()

            // 重试成功追加数据，且证明前两次请求均发往 page1
            assertThat(viewModel.uiState.value.recommendItems).hasSize(1)
            assertThat(
                viewModel.uiState.value.recommendItems
                    .first()
                    .aid,
            ).isEqualTo(100L)
            coVerify(exactly = 2) { recommendRepo.getRecommendVideos(page1, any()) }
        }

    @Test
    fun `loadRecommend rethrows CancellationException and does not mark error`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws
                CancellationException("Explicit cancellation")

            viewModel = createViewModel()
            advanceUntilIdle()

            // CancellationException 不应被吞并当作普通失败标记 recommendError
            assertThat(viewModel.uiState.value.recommendError).isFalse()
            assertThat(viewModel.uiState.value.recommendLoading).isFalse()
        }

    @Test
    fun `loadRecommend keeps partial data and no error when a later page fails`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(1)),
                    nextPage = RecommendPage(),
                ) andThenThrows RuntimeException("boom")

            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 第 2 页失败：保留第 1 页数据，不整块报错
            assertThat(state.recommendItems).hasSize(1)
            assertThat(state.recommendError).isFalse()
            assertThat(state.recommendLoading).isFalse()
        }

    @Test
    fun `loadRecommend load-more failure sets error but keeps items`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.recommendItems).isNotEmpty()

            coEvery { recommendRepo.getRecommendVideos(any(), any()) } throws RuntimeException("boom")
            viewModel.loadRecommend()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.recommendItems).isNotEmpty()
            assertThat(state.recommendError).isTrue()
        }

    @Test
    fun `refresh dispatches Popular tab`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(777)),
                    nextPage = PopularVideoPage(),
                    noMore = true,
                )

            viewModel.refresh(dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.popularItems[0]
                    .aid,
            ).isEqualTo(777)
            assertThat(viewModel.uiState.value.popularHasMore).isFalse()
        }

    @Test
    fun `refresh dispatches Dynamics tab when logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "o",
                    updateBaseline = "b",
                )
            viewModel.updateLoginState(true)
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(66)),
                    hasMore = false,
                    historyOffset = "o2",
                    updateBaseline = "b2",
                )

            viewModel.refresh(dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics)
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.dynamicItems[0]
                    .aid,
            ).isEqualTo(66)
        }

    @Test
    fun `loadMore dispatches Dynamics tab when logged in`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = true,
                    historyOffset = "offset1",
                    updateBaseline = "baseline1",
                )
            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(20)),
                    hasMore = false,
                    historyOffset = "offset2",
                    updateBaseline = "baseline2",
                )

            viewModel.loadMore(dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(2)
            assertThat(
                viewModel.uiState.value.dynamicItems[1]
                    .aid,
            ).isEqualTo(20)
        }

    @Test
    fun `updateLoginState to true with existing dynamics does not duplicate load`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { userRepo.getDynamicVideos(any(), any(), any(), any()) } returns
                DynamicVideoData(
                    videos = listOf(fakeDynamicVideo(10)),
                    hasMore = false,
                    historyOffset = "offset",
                    updateBaseline = "baseline",
                )
            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)

            viewModel.updateLoginState(true)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.dynamicItems).hasSize(1)
        }

    @Test
    fun `refreshPopular clears error`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } throws RuntimeException("error")
            viewModel.refreshPopular()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.popularError).isTrue()

            coEvery { recommendRepo.getPopularVideos(any(), any()) } returns
                PopularVideoData(
                    list = listOf(fakeUgcItem(1)),
                    nextPage = PopularVideoPage(),
                    noMore = false,
                )
            viewModel.refreshPopular()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.popularError).isFalse()
            assertThat(viewModel.uiState.value.popularItems).isNotEmpty()
        }

    @Test
    fun `loadRecommend updates recommendHasMore from data hasMore`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = listOf(fakeUgcItem(1)),
                    nextPage = RecommendPage(),
                    hasMore = false,
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendHasMore).isFalse()
            assertThat(viewModel.uiState.value.recommendItems).hasSize(1)
        }

    @Test
    fun `loadRecommend with empty items and hasMore false sets recommendHasMore to false`() =
        runTest(testDispatcher) {
            coEvery { recommendRepo.getRecommendVideos(any(), any()) } returns
                RecommendData(
                    items = emptyList(),
                    nextPage = RecommendPage(),
                    hasMore = false,
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recommendHasMore).isFalse()
        }
}
