package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.home.RcmdIndexData
import dev.frost819.newbv.biliapi.http.entity.home.RcmdTopData
import dev.frost819.newbv.biliapi.http.entity.video.Dimension
import dev.frost819.newbv.biliapi.http.entity.video.PopularVideoData
import dev.frost819.newbv.biliapi.http.entity.video.VideoInfo
import dev.frost819.newbv.biliapi.http.entity.video.VideoOwner
import dev.frost819.newbv.biliapi.http.entity.video.VideoRights
import dev.frost819.newbv.biliapi.http.entity.video.VideoStat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [RecommendVideoRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证热门视频与推荐视频获取的
 * Web 路径参数传递、分页递增与数据转换。不依赖真实网络。
 */
class RecommendVideoRepositoryUnitTest {
    private lateinit var repository: RecommendVideoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var channelRepository: ChannelRepository

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        channelRepository = ChannelRepository()
        repository = RecommendVideoRepository(authRepository, channelRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // getPopularVideos (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getPopularVideos Web returns mapped items with correct nextPage`() =
        runTest {
            val popularData =
                PopularVideoData(
                    list = listOf(fakeVideoInfo(aid = 1L, title = "v1"), fakeVideoInfo(aid = 2L, title = "v2")),
                    noMore = false,
                )
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = popularData)

            val result =
                repository.getPopularVideos(
                    page = PopularVideoPage(nextWebPageSize = 20, nextWebPageNumber = 1),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.list).hasSize(2)
            assertThat(result.list[0].aid).isEqualTo(1L)
            assertThat(result.list[1].aid).isEqualTo(2L)
            assertThat(result.nextPage.nextWebPageNumber).isEqualTo(2)
            assertThat(result.noMore).isFalse()
        }

    @Test
    fun `getPopularVideos Web passes page parameters correctly`() =
        runTest {
            val popularData = PopularVideoData(list = emptyList(), noMore = true)
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = popularData)

            repository.getPopularVideos(
                page = PopularVideoPage(nextWebPageSize = 30, nextWebPageNumber = 5),
                preferApiType = ApiType.Web,
            )

            coVerify { BiliHttpApi.getPopularVideoData(pageSize = 30, pageNumber = 5) }
        }

    @Test
    fun `getPopularVideos Web with noMore true reflects in result`() =
        runTest {
            val popularData = PopularVideoData(list = emptyList(), noMore = true)
            coEvery { BiliHttpApi.getPopularVideoData(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = popularData)

            val result =
                repository.getPopularVideos(
                    page = PopularVideoPage(),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.noMore).isTrue()
        }

    // ------------------------------------------------------------------
    // getRecommendVideos (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getRecommendVideos Web returns mapped items with incremented nextWebIdx`() =
        runTest {
            val rcmdData =
                RcmdTopData(
                    item = listOf(fakeRcmdItem(id = 10L), fakeRcmdItem(id = 20L)),
                    mid = 1L,
                    preloadExposePct = 0.0,
                    preloadFloorExposePct = 0.0,
                )
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdData)

            val result =
                repository.getRecommendVideos(
                    page = RecommendPage(nextWebIdx = 3),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.items).hasSize(2)
            assertThat(result.items[0].aid).isEqualTo(10L)
            assertThat(result.items[1].aid).isEqualTo(20L)
            assertThat(result.nextPage.nextWebIdx).isEqualTo(4)
            assertThat(result.hasMore).isTrue()
        }

    @Test
    fun `getRecommendVideos Web passes idx parameter`() =
        runTest {
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdDataEmpty())

            repository.getRecommendVideos(page = RecommendPage(nextWebIdx = 7), preferApiType = ApiType.Web)

            coVerify { BiliHttpApi.getFeedRcmd(any(), any(), eq(7)) }
        }

    @Test
    fun `getRecommendVideos Web with empty item list returns empty items`() =
        runTest {
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdDataEmpty())

            val result = repository.getRecommendVideos(page = RecommendPage(), preferApiType = ApiType.Web)

            assertThat(result.items).isEmpty()
            assertThat(result.nextPage.nextWebIdx).isEqualTo(2)
        }

    @Test
    fun `getRecommendVideos Web with empty items infers hasMore false`() =
        runTest {
            coEvery { BiliHttpApi.getFeedRcmd(any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdDataEmpty())

            val result = repository.getRecommendVideos(page = RecommendPage(), preferApiType = ApiType.Web)

            assertThat(result.hasMore).isFalse()
            assertThat(result.items).isEmpty()
        }

    // ------------------------------------------------------------------
    // getPopularVideos (App)
    // ------------------------------------------------------------------

    @Test
    fun `getPopularVideos App throws when gRPC stub is null`() =
        runTest {
            // 通道未就绪时不回退 Web，也不静默返回空列表，而是抛明确异常供上层报错重试
            val error =
                runCatching {
                    repository.getPopularVideos(
                        page = PopularVideoPage(),
                        preferApiType = ApiType.App,
                    )
                }.exceptionOrNull()

            assertThat(error).isInstanceOf(IllegalStateException::class.java)
        }

    // ------------------------------------------------------------------
    // getRecommendVideos (App)
    // ------------------------------------------------------------------

    @Test
    fun `getRecommendVideos App calls getFeedIndex with accessKey`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeRcmdIndexData(emptyList()))

            repository.getRecommendVideos(page = RecommendPage(), preferApiType = ApiType.App)

            coVerify { BiliHttpApi.getFeedIndex(any(), eq("test-access-token")) }
        }

    @Test
    fun `getRecommendVideos App maps items and advances nextPage`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            val rcmdData = fakeRcmdIndexData(listOf(fakeAppRcmdItem(idx = 1), fakeAppRcmdItem(idx = 2)))
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdData)

            val result =
                repository.getRecommendVideos(
                    page = RecommendPage(nextAppIdx = 1),
                    preferApiType = ApiType.App,
                )

            assertThat(result.items).hasSize(2)
            assertThat(result.items[0].title).isEqualTo("app-rcmd-1")
            assertThat(result.items[1].title).isEqualTo("app-rcmd-2")
            assertThat(result.nextPage.nextAppIdx).isEqualTo(3)
            assertThat(result.hasMore).isTrue()
        }

    @Test
    fun `getRecommendVideos App advances cursor from raw items when all filtered out`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            // 两个卡片都不是 av，会被过滤掉；游标应基于原始列表的最后 idx(9) 推进到 10，
            // 而不是停留原地导致重复请求同一页
            val raw =
                listOf(
                    fakeAppRcmdItem(idx = 5).copy(cardGoto = "live"),
                    fakeAppRcmdItem(idx = 9).copy(cardGoto = "bangumi"),
                )
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = fakeRcmdIndexData(raw))

            val result =
                repository.getRecommendVideos(
                    page = RecommendPage(nextAppIdx = 1),
                    preferApiType = ApiType.App,
                )

            assertThat(result.items).isEmpty()
            assertThat(result.nextPage.nextAppIdx).isEqualTo(10)
        }

    @Test
    fun `getRecommendVideos App propagates hasMore false from remote response`() =
        runTest {
            authRepository.accessToken = "test-access-token"
            val rcmdData = fakeRcmdIndexData(emptyList()).copy(hasMore = false)
            coEvery { BiliHttpApi.getFeedIndex(any(), any()) } returns
                BiliResponse(code = 0, message = "", data = rcmdData)

            val result = repository.getRecommendVideos(page = RecommendPage(), preferApiType = ApiType.App)

            assertThat(result.hasMore).isFalse()
        }

    @Test
    fun `RcmdTopData parses standard json fixture without has_more as null`() {
        val json = Json { ignoreUnknownKeys = true }
        // 真实 Web 推荐接口没有 has_more 字段
        val rawJson =
            """
            {
                "item": [],
                "mid": 12345,
                "preload_expose_pct": 0.0,
                "preload_floor_expose_pct": 0.0
            }
            """.trimIndent()
        val parsed = json.decodeFromString<RcmdTopData>(rawJson)
        assertThat(parsed.hasMore).isNull()
    }

    @Test
    fun `RcmdIndexData parses integer and boolean has_more correctly`() {
        val json = Json { ignoreUnknownKeys = true }

        fun makeJson(hasMoreValue: String?) =
            buildString {
                append(
                    """{"config":{"auto_refresh_time":0,"auto_refresh_time_by_active":0,"auto_refresh_time_by_appear":0,"autoplay_card":0,"card_density_exp":0,"column":0,"enable_rcmd_guide":false,"feed_clean_abtest":0,"home_transfer_test":0,"inline_sound":0,"is_back_to_homepage":false,"show_inline_danmaku":0,"story_mode_v2_guide_exp":0,"toast":null,"visible_area":0},"items":[]""",
                )
                if (hasMoreValue != null) {
                    append(""", "has_more": $hasMoreValue""")
                }
                append("}")
            }

        // 验证整数 0 解析为 false
        val parsedInt0 = json.decodeFromString<RcmdIndexData>(makeJson("0"))
        assertThat(parsedInt0.hasMore).isFalse()

        // 验证整数 1 解析为 true
        val parsedInt1 = json.decodeFromString<RcmdIndexData>(makeJson("1"))
        assertThat(parsedInt1.hasMore).isTrue()

        // 验证布尔 false 解析为 false
        val parsedBool = json.decodeFromString<RcmdIndexData>(makeJson("false"))
        assertThat(parsedBool.hasMore).isFalse()

        // 验证字段省略时默认值为 true
        val parsedOmitted = json.decodeFromString<RcmdIndexData>(makeJson(null))
        assertThat(parsedOmitted.hasMore).isTrue()
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun fakeVideoInfo(
        aid: Long = 1L,
        title: String = "title",
    ) = VideoInfo(
        bvid = "BV$aid",
        aid = aid,
        videos = 1,
        tid = 0,
        tname = "test",
        copyright = 1,
        pic = "http://pic.test/$aid",
        title = title,
        pubdate = 1700000000,
        desc = "desc",
        state = 0,
        duration = 120,
        rights = VideoRights(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, arcPay = 0),
        owner = VideoOwner(mid = 1L, name = "up", face = "http://face.test"),
        stat = VideoStat(aid = aid, _view = 1000L, danmaku = 50, like = 10),
        dynamic = "",
        cid = aid * 10,
        dimension = Dimension(1920, 1080, 0),
    )

    private fun fakeRcmdItem(id: Long = 1L) =
        RcmdTopData.RcmdItem(
            businessInfo = null,
            bvid = "BV$id",
            cid = id * 10,
            duration = 120,
            enableVt = 0,
            goto = "av",
            id = id,
            isFollowed = 0,
            isStock = 0,
            pic = "http://pic.test/$id",
            pos = 0,
            pubdate = 1700000000,
            showInfo = 0,
            title = "rcmd-$id",
            trackId = "t$id",
            uri = "uri$id",
            stat = RcmdTopData.RcmdItem.Stat(danmaku = 30, like = 10, view = 5000, vt = 0),
            owner = RcmdTopData.RcmdItem.Owner(face = "http://face.test", mid = 1L, name = "up"),
        )

    private fun rcmdDataEmpty() =
        RcmdTopData(
            item = emptyList(),
            mid = 1L,
            preloadExposePct = 0.0,
            preloadFloorExposePct = 0.0,
        )

    private fun fakeRcmdIndexData(items: List<RcmdIndexData.RcmdItem>) =
        RcmdIndexData(
            config =
                RcmdIndexData.Config(
                    autoRefreshTime = 0,
                    autoRefreshTimeByActive = 0,
                    autoRefreshTimeByAppear = 0,
                    autoplayCard = 0,
                    cardDensityExp = 0,
                    column = 0,
                    enableRcmdGuide = false,
                    feedCleanAbtest = 0,
                    homeTransferTest = 0,
                    inlineSound = 0,
                    isBackToHomepage = false,
                    showInlineDanmaku = 0,
                    storyModeV2GuideExp = 0,
                    toast = kotlinx.serialization.json.JsonNull,
                    visibleArea = 0,
                ),
            items = items,
        )

    private fun fakeAppRcmdItem(idx: Int = 1) =
        RcmdIndexData.RcmdItem(
            args =
                RcmdIndexData.RcmdItem.Args(
                    aid = (idx * 10).toLong(),
                    rid = 0,
                    rname = "",
                    tid = 0,
                    tname = "",
                    upId = 1L,
                    upName = "up",
                ),
            cardGoto = "av",
            cardType = "small_cover_v5",
            cover = "http://cover.test/$idx",
            coverLeftText1 = "5000",
            coverLeftText2 = "10",
            coverRightText = "5:00",
            idx = idx,
            param = "${idx * 10}",
            title = "app-rcmd-$idx",
            uri = "bilibili://video/${idx * 10}",
            threePointV2 = emptyList(),
        )
}
