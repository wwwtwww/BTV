package dev.frost819.newbv.app.viewmodel.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.state.search.SearchResultItem
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.biliapi.repositories.SearchType
import dev.frost819.newbv.biliapi.repositories.SearchTypePage
import dev.frost819.newbv.biliapi.repositories.SearchTypeResult
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

/**
 * [SearchResultViewModel] 的单元测试。
 *
 * 验证按需单分类加载、TopNav 切换防抖懒加载与缓存命中、单分类强制刷新、分页去重及筛选逻辑。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchResultViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var searchRepo: SearchRepository
    private lateinit var viewModel: SearchResultViewModel

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_search_result_vm", ".preferences_pb")
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
            // Leave Prefs initialized
        }
    }

    private fun fakeVideoResult(aid: Long) =
        SearchTypeResult.Video(
            aid = aid,
            bvid = "BV$aid",
            title = "video $aid",
            cover = "http://example.com/cover.jpg",
            author = "up",
            mid = 100L,
            duration = 120,
            play = 10000,
            danmaku = 500,
        )

    private fun fakePgcResult(seasonId: Int) =
        SearchTypeResult.Pgc(
            title = "番剧 $seasonId",
            cover = "http://example.com/cover.jpg",
            star = 9.0f,
            seasonId = seasonId,
        )

    private fun fakeUserResult(mid: Long) =
        SearchTypeResult.User(
            mid = mid,
            name = "user $mid",
            avatar = "http://example.com/avatar.jpg",
            sign = "签名",
        )

    private fun fakeLiveRoomResult(roomId: Long) =
        SearchTypeResult.LiveRoom(
            roomId = roomId,
            title = "直播间 $roomId",
            uname = "主播",
            uid = 100L,
            cover = "http://example.com/live.jpg",
            userCover = "http://example.com/user-cover.jpg",
            face = "http://example.com/avatar.jpg",
            areaName = "测试分区",
            online = 1000,
            liveStatus = 1,
        )

    private fun fakeVideoList(count: Int): List<SearchTypeResult.Video> =
        (1..count).map { fakeVideoResult(it.toLong()) }

    private fun fakeVideoSearchResult(
        videos: List<SearchTypeResult.Video>,
        hasMore: Boolean = true,
    ) = SearchTypeResult(
        videos = videos,
        page = SearchTypePage(nextPageForWeb = 2),
        hasMore = hasMore,
    )

    private fun fakePgcSearchResult(
        pgcs: List<SearchTypeResult.Pgc>,
        hasMore: Boolean = true,
    ) = SearchTypeResult(
        pgcs = pgcs,
        page = SearchTypePage(nextPageForWeb = 2),
        hasMore = hasMore,
    )

    private fun fakeUserSearchResult(
        users: List<SearchTypeResult.User>,
        hasMore: Boolean = true,
    ) = SearchTypeResult(
        users = users,
        page = SearchTypePage(nextPageForWeb = 2),
        hasMore = hasMore,
    )

    private fun fakeLiveRoomSearchResult(
        rooms: List<SearchTypeResult.LiveRoom>,
        hasMore: Boolean = true,
    ) = SearchTypeResult(
        liveRooms = rooms,
        page = SearchTypePage(nextPageForWeb = 2),
        hasMore = hasMore,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { Prefs.clear() }

        searchRepo = mockk()

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.Video,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakeVideoSearchResult(fakeVideoList(20))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.MediaBangumi,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakePgcSearchResult(listOf(fakePgcResult(101)))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.MediaFt,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakePgcSearchResult(listOf(fakePgcResult(201)))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.BiliUser,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakeUserSearchResult(listOf(fakeUserResult(301)))

        coEvery {
            searchRepo.searchType(
                keyword = any(),
                type = SearchType.LiveRoom,
                tid = any(),
                order = any(),
                duration = any(),
                page = any(),
                preferApiType = any(),
            )
        } returns fakeLiveRoomSearchResult(listOf(fakeLiveRoomResult(1718159119L)))

        viewModel = SearchResultViewModel(searchRepo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search loads only activeType immediately to avoid 5 parallel WBI requests`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.keyword).isEqualTo("测试")

            val videoResult = requireNotNull(state.results[SearchType.Video])
            assertThat(videoResult.items).hasSize(20)
            assertThat(videoResult.isLoading).isFalse()
            assertThat(videoResult.error).isFalse()

            // Inactive tabs stay unloaded until focused/selected
            assertThat(requireNotNull(state.results[SearchType.MediaBangumi]).items).isEmpty()
            assertThat(requireNotNull(state.results[SearchType.MediaFt]).items).isEmpty()
            assertThat(requireNotNull(state.results[SearchType.BiliUser]).items).isEmpty()
            assertThat(requireNotNull(state.results[SearchType.LiveRoom]).items).isEmpty()

            coVerify(exactly = 1) {
                searchRepo.searchType(any(), any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `switchType lazy-loads uncached category after debounce`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            viewModel.switchType(SearchType.BiliUser)
            assertThat(viewModel.uiState.value.activeType).isEqualTo(SearchType.BiliUser)

            // Before 150ms debounce elapses, no request is sent yet
            advanceTimeBy(100)
            coVerify(exactly = 0) {
                searchRepo.searchType(any(), SearchType.BiliUser, any(), any(), any(), any(), any())
            }

            // After debounce elapses, BiliUser loads automatically
            advanceTimeBy(60)
            advanceUntilIdle()

            val userItems = requireNotNull(viewModel.uiState.value.results[SearchType.BiliUser]).items
            assertThat(userItems).hasSize(1)
            val first = userItems[0] as SearchResultItem.UserItem
            assertThat(first.user.mid).isEqualTo(301L)
        }

    @Test
    fun `switchType serves cached category immediately without redundant network request`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            viewModel.switchType(SearchType.BiliUser)
            advanceUntilIdle()
            assertThat(requireNotNull(viewModel.uiState.value.results[SearchType.BiliUser]).items).hasSize(1)

            // Switch back to already-cached Video tab
            viewModel.switchType(SearchType.Video)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.activeType).isEqualTo(SearchType.Video)
            assertThat(requireNotNull(viewModel.uiState.value.results[SearchType.Video]).items).hasSize(20)
            coVerify(exactly = 1) {
                searchRepo.searchType(any(), SearchType.Video, any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `rapid switchType sweep cancels intermediate uncached tab requests`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            // Simulate user rapidly sweeping D-Pad Right across TopNav tabs
            viewModel.switchType(SearchType.MediaBangumi)
            advanceTimeBy(50)
            viewModel.switchType(SearchType.MediaFt)
            advanceTimeBy(50)
            viewModel.switchType(SearchType.BiliUser)
            advanceTimeBy(50)
            viewModel.switchType(SearchType.LiveRoom)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                searchRepo.searchType(any(), SearchType.MediaBangumi, any(), any(), any(), any(), any())
            }
            coVerify(exactly = 0) {
                searchRepo.searchType(any(), SearchType.MediaFt, any(), any(), any(), any(), any())
            }
            coVerify(exactly = 0) {
                searchRepo.searchType(any(), SearchType.BiliUser, any(), any(), any(), any(), any())
            }
            coVerify(exactly = 1) {
                searchRepo.searchType(any(), SearchType.LiveRoom, any(), any(), any(), any(), any())
            }

            val liveItems = requireNotNull(viewModel.uiState.value.results[SearchType.LiveRoom]).items
            assertThat(liveItems).hasSize(1)
            assertThat(liveItems.single()).isInstanceOf(SearchResultItem.LiveRoomItem::class.java)
        }

    @Test
    fun `refreshType reloads target category and preserves other cached categories`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()
            viewModel.switchType(SearchType.MediaBangumi)
            advanceUntilIdle()

            coEvery {
                searchRepo.searchType(
                    keyword = "测试",
                    type = SearchType.MediaBangumi,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            } returns fakePgcSearchResult(listOf(fakePgcResult(888)))

            viewModel.refreshType(SearchType.MediaBangumi)
            advanceUntilIdle()

            val bangumiItems = requireNotNull(viewModel.uiState.value.results[SearchType.MediaBangumi]).items
            assertThat(bangumiItems).hasSize(1)
            assertThat((bangumiItems[0] as SearchResultItem.PgcItem).pgc.seasonId).isEqualTo(888)
            // Cached Video tab remains intact
            assertThat(requireNotNull(viewModel.uiState.value.results[SearchType.Video]).items).hasSize(20)
        }

    @Test
    fun `refreshType with blank keyword does nothing`() =
        runTest(testDispatcher) {
            viewModel.refreshType(SearchType.Video)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                searchRepo.searchType(any(), any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `search resets all previous cached results and reloads activeType`() =
        runTest(testDispatcher) {
            viewModel.search("第一次")
            advanceUntilIdle()
            viewModel.switchType(SearchType.BiliUser)
            advanceUntilIdle()
            assertThat(requireNotNull(viewModel.uiState.value.results[SearchType.Video]).items).hasSize(20)

            // Now activeType is BiliUser; a new search resets Video cache and loads BiliUser
            viewModel.search("第二次")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.keyword).isEqualTo("第二次")
            assertThat(requireNotNull(state.results[SearchType.Video]).items).isEmpty()
            assertThat(requireNotNull(state.results[SearchType.BiliUser]).items).hasSize(1)
        }

    @Test
    fun `loadMore appends items to existing results`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            val firstPageCount =
                requireNotNull(viewModel.uiState.value.results[SearchType.Video]).items.size

            coEvery {
                searchRepo.searchType(
                    keyword = any(),
                    type = SearchType.Video,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            } returns fakeVideoSearchResult((21..40).map { fakeVideoResult(it.toLong()) })

            viewModel.loadMore(SearchType.Video)
            advanceUntilIdle()

            val result = requireNotNull(viewModel.uiState.value.results[SearchType.Video])
            assertThat(result.items).hasSize(firstPageCount + 20)
        }

    @Test
    fun `loadMore skips when already loading`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            viewModel.loadMore(SearchType.Video)
            viewModel.loadMore(SearchType.Video)
            advanceUntilIdle()

            coVerify(exactly = 2) {
                searchRepo.searchType(
                    keyword = any(),
                    type = SearchType.Video,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            }
        }

    @Test
    fun `loadMore skips when no more data`() =
        runTest(testDispatcher) {
            coEvery {
                searchRepo.searchType(
                    keyword = any(),
                    type = SearchType.Video,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            } returns
                SearchTypeResult(
                    videos = emptyList(),
                    page = SearchTypePage(),
                    hasMore = false,
                )

            viewModel.search("测试")
            advanceUntilIdle()

            val result = requireNotNull(viewModel.uiState.value.results[SearchType.Video])
            assertThat(result.hasMore).isFalse()
            assertThat(result.items).isEmpty()

            viewModel.loadMore(SearchType.Video)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                searchRepo.searchType(
                    keyword = any(),
                    type = SearchType.Video,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            }
        }

    @Test
    fun `loadMore failure sets error flag`() =
        runTest(testDispatcher) {
            coEvery {
                searchRepo.searchType(
                    keyword = any(),
                    type = SearchType.Video,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            } throws RuntimeException("network error")

            viewModel.search("测试")
            advanceUntilIdle()

            val result = requireNotNull(viewModel.uiState.value.results[SearchType.Video])
            assertThat(result.error).isTrue()
            assertThat(result.isLoading).isFalse()
        }

    @Test
    fun `updateFilter updates order and duration then re-searches`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            viewModel.updateFilter(
                SearchFilterOrderType.LatestPublish,
                SearchFilterDuration.LessThan10Minutes,
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.selectedOrder).isEqualTo(SearchFilterOrderType.LatestPublish)
            assertThat(state.selectedDuration).isEqualTo(SearchFilterDuration.LessThan10Minutes)
            assertThat(state.showFilter).isFalse()

            coVerify(exactly = 1) {
                searchRepo.searchType(
                    keyword = "测试",
                    type = SearchType.Video,
                    tid = null,
                    order = SearchFilterOrderType.LatestPublish,
                    duration = SearchFilterDuration.LessThan10Minutes,
                    page = any(),
                    preferApiType = any(),
                )
            }
        }

    @Test
    fun `toggleFilter shows and hides filter`() =
        runTest(testDispatcher) {
            assertThat(viewModel.uiState.value.showFilter).isFalse()

            viewModel.toggleFilter(true)
            assertThat(viewModel.uiState.value.showFilter).isTrue()

            viewModel.toggleFilter(false)
            assertThat(viewModel.uiState.value.showFilter).isFalse()
        }

    @Test
    fun `search with blank keyword does nothing`() =
        runTest(testDispatcher) {
            viewModel.search("")
            advanceUntilIdle()

            coVerify(exactly = 0) {
                searchRepo.searchType(any(), any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `video items are correctly mapped to VideoItem`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            val items = requireNotNull(viewModel.uiState.value.results[SearchType.Video]).items
            assertThat(items).hasSize(20)
            val first = items[0] as SearchResultItem.VideoItem
            assertThat(first.video.aid).isEqualTo(1)
            assertThat(first.video.title).isEqualTo("video 1")
        }

    @Test
    fun `pgc items are correctly mapped to PgcItem when switching to MediaFt`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            viewModel.switchType(SearchType.MediaFt)
            advanceUntilIdle()

            val items = requireNotNull(viewModel.uiState.value.results[SearchType.MediaFt]).items
            assertThat(items).hasSize(1)
            val first = items[0] as SearchResultItem.PgcItem
            assertThat(first.pgc.seasonId).isEqualTo(201)
        }

    @Test
    fun `loadMore deduplicates items with same aid`() =
        runTest(testDispatcher) {
            viewModel.search("测试")
            advanceUntilIdle()

            coEvery {
                searchRepo.searchType(
                    keyword = any(),
                    type = SearchType.Video,
                    tid = any(),
                    order = any(),
                    duration = any(),
                    page = any(),
                    preferApiType = any(),
                )
            } returns fakeVideoSearchResult(fakeVideoList(20))

            viewModel.loadMore(SearchType.Video)
            advanceUntilIdle()

            val result = requireNotNull(viewModel.uiState.value.results[SearchType.Video])
            assertThat(result.items.size).isEqualTo(20)
        }

    @Test
    fun `loadMore with blank keyword does nothing`() =
        runTest(testDispatcher) {
            viewModel.loadMore(SearchType.Video)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                searchRepo.searchType(any(), any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `new search cancels ongoing search jobs and updates to latest keyword`() =
        runTest(testDispatcher) {
            viewModel.search("first")
            viewModel.search("second")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.keyword).isEqualTo("second")
        }
}
