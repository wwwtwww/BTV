package dev.frost819.newbv.app.ui.navigation

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.component.videocard.isPgc
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.user.DynamicVideo
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
import org.junit.jupiter.api.Test

/**
 * [VideoCardData] 的单元测试。
 *
 * 验证 [isPgc]、[VideoCardData.fromRelatedVideo]、[VideoCardData.fromUgcItem]
 * 以及 [VideoCardData.fromDynamicVideo] 的字段映射与边界值转换逻辑。
 */
class VideoCardDataTest {
    @Test
    fun `isPgc is true only for positive epid`() {
        assertThat(videoCard(epid = null).isPgc).isFalse()
        assertThat(videoCard(epid = 0).isPgc).isFalse()
        assertThat(videoCard(epid = 123).isPgc).isTrue()
    }

    @Test
    fun `fromRelatedVideo keeps epid only when jumpToSeason`() {
        val pgc = VideoCardData.fromRelatedVideo(related(epid = 123, jumpToSeason = true))
        assertThat(pgc.epid).isEqualTo(123)
        assertThat(pgc.isPgc).isTrue()

        val ugc = VideoCardData.fromRelatedVideo(related(epid = 123, jumpToSeason = false))
        assertThat(ugc.epid).isNull()
        assertThat(ugc.cid).isEqualTo(999L)
    }

    @Test
    fun `fromUgcItem maps all fields correctly with valid play and danmaku`() {
        val ugcItem =
            UgcItem(
                aid = 123456L,
                bvid = "BV1xx411c7mD",
                title = "测试视频标题",
                cover = "https://example.com/cover.jpg",
                author = "测试UP主",
                authorMid = 987654L,
                play = 12500,
                danmaku = 300,
                duration = 125, // 2分05秒
                pubTime = "2026-03-20",
            )

        val card = VideoCardData.fromUgcItem(ugcItem)

        assertThat(card.avid).isEqualTo(123456L)
        assertThat(card.bvid).isEqualTo("BV1xx411c7mD")
        assertThat(card.title).isEqualTo("测试视频标题")
        assertThat(card.cover).isEqualTo("https://example.com/cover.jpg")
        assertThat(card.upName).isEqualTo("测试UP主")
        assertThat(card.upMid).isEqualTo(987654L)
        assertThat(card.playString).isEqualTo("1.2万") // (12500 / 1000) / 10f -> 1.2万
        assertThat(card.danmakuString).isEqualTo("300")
        assertThat(card.timeString).isEqualTo("02:05")
        assertThat(card.pubTime).isEqualTo("2026-03-20")
    }

    @Test
    fun `fromUgcItem handles negative play and danmaku as empty string`() {
        val ugcItem =
            UgcItem(
                aid = 111L,
                bvid = "BV1test",
                title = "未知播放数据",
                cover = "https://example.com/c.jpg",
                author = "UP",
                authorMid = null,
                play = -1,
                danmaku = -1,
                duration = 0,
                pubTime = null,
            )

        val card = VideoCardData.fromUgcItem(ugcItem)

        assertThat(card.playString).isEmpty()
        assertThat(card.danmakuString).isEmpty()
        assertThat(card.timeString).isEqualTo("00:00")
        assertThat(card.upMid).isNull()
        assertThat(card.pubTime).isNull()
    }

    @Test
    fun `fromDynamicVideo maps all fields correctly including epid and seasonId`() {
        val dynamicVideo =
            DynamicVideo(
                aid = 88888L,
                bvid = "BV1Dynamic",
                cid = 10001L,
                epid = 3344,
                seasonId = 5566,
                title = "番剧动态",
                cover = "https://example.com/anime.jpg",
                author = "番剧出差",
                authorMid = 10086L,
                duration = 1440, // 24分00秒
                play = 50000,
                danmaku = 1200,
                pubTime = "昨天",
            )

        val card = VideoCardData.fromDynamicVideo(dynamicVideo)

        assertThat(card.avid).isEqualTo(88888L)
        assertThat(card.bvid).isEqualTo("BV1Dynamic")
        assertThat(card.cid).isEqualTo(10001L)
        assertThat(card.epid).isEqualTo(3344)
        assertThat(card.seasonId).isEqualTo(5566)
        assertThat(card.isPgc).isTrue()
        assertThat(card.title).isEqualTo("番剧动态")
        assertThat(card.cover).isEqualTo("https://example.com/anime.jpg")
        assertThat(card.upName).isEqualTo("番剧出差")
        assertThat(card.upMid).isEqualTo(10086L)
        assertThat(card.playString).isEqualTo("5.0万")
        assertThat(card.danmakuString).isEqualTo("1200")
        assertThat(card.timeString).isEqualTo("24:00")
        assertThat(card.pubTime).isEqualTo("昨天")
    }

    @Test
    fun `fromDynamicVideo handles null bvid and negative play counts`() {
        val dynamicVideo =
            DynamicVideo(
                aid = 99999L,
                bvid = null,
                cid = 20002L,
                epid = null,
                title = "无BV号动态",
                cover = "https://example.com/ugc.jpg",
                author = "动态作者",
                authorMid = 20000L,
                duration = 3661, // 1小时01分01秒 -> 01:01:01
                play = -1,
                danmaku = -1,
                pubTime = null,
            )

        val card = VideoCardData.fromDynamicVideo(dynamicVideo)

        assertThat(card.bvid).isEmpty()
        assertThat(card.epid).isNull()
        assertThat(card.isPgc).isFalse()
        assertThat(card.playString).isEmpty()
        assertThat(card.danmakuString).isEmpty()
        assertThat(card.timeString).isEqualTo("01:01:01")
        assertThat(card.pubTime).isNull()
    }

    private fun videoCard(epid: Int?): VideoCardData =
        VideoCardData(
            avid = 1L,
            title = "t",
            cover = "",
            upName = "",
            epid = epid,
        )

    private fun related(
        epid: Int?,
        jumpToSeason: Boolean,
    ): RelatedVideo =
        RelatedVideo(
            aid = 1L,
            cid = 999L,
            cover = "",
            title = "t",
            duration = 0,
            author = null,
            jumpToSeason = jumpToSeason,
            epid = epid,
            view = 0,
            danmaku = 0,
        )
}
