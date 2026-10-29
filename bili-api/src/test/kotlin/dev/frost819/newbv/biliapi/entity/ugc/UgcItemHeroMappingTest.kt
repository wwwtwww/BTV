package dev.frost819.newbv.biliapi.entity.ugc

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.home.VideoBriefMeta
import dev.frost819.newbv.biliapi.http.entity.video.VideoInfo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [UgcItem] 与 [VideoBriefMeta] 的 Hero 元数据映射测试。
 */
class UgcItemHeroMappingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fromVideoInfo maps bvid tagName rcmdReason and description from json`() {
        val rawJson =
            """
            {
              "bvid": "BV1heroTest",
              "aid": 9527,
              "videos": 1,
              "tid": 95,
              "tname": "数码",
              "copyright": 1,
              "pic": "https://i0.hdslb.com/bfs/archive/test.jpg",
              "title": "热门测试视频",
              "pubdate": 1700000000,
              "ctime": 1700000000,
              "desc": "热门接口直接返回的简介文案",
              "state": 0,
              "duration": 360,
              "rights": {
                "bp": 0,
                "elec": 0,
                "download": 1,
                "movie": 0,
                "pay": 0,
                "hd5": 1,
                "no_reprint": 1,
                "autoplay": 1,
                "ugc_pay": 0,
                "is_cooperation": 0,
                "ugc_pay_preview": 0,
                "arc_pay": 0
              },
              "owner": {
                "mid": 1001,
                "name": "科技UP",
                "face": "https://i0.hdslb.com/bfs/face/member.jpg"
              },
              "stat": {
                "aid": 9527,
                "view": 120000,
                "danmaku": 800,
                "reply": 100,
                "favorite": 500,
                "coin": 600,
                "share": 90,
                "now_rank": 0,
                "his_rank": 1,
                "like": 5000,
                "dislike": 0,
                "evaluation": ""
              },
              "dynamic": "",
              "cid": 95270,
              "dimension": {
                "width": 1920,
                "height": 1080,
                "rotate": 0
              },
              "rcmd_reason": {
                "content": "百万播放",
                "corner_mark": 1
              }
            }
            """.trimIndent()

        val videoInfo = json.decodeFromString<VideoInfo>(rawJson)
        val ugcItem = UgcItem.fromVideoInfo(videoInfo)

        assertThat(ugcItem.aid).isEqualTo(9527L)
        assertThat(ugcItem.bvid).isEqualTo("BV1heroTest")
        assertThat(ugcItem.tagName).isEqualTo("数码")
        assertThat(ugcItem.rcmdReason).isEqualTo("百万播放")
        assertThat(ugcItem.description).isEqualTo("热门接口直接返回的简介文案")

        val briefMeta =
            VideoBriefMeta(
                aid = ugcItem.aid,
                tagName = ugcItem.tagName,
                description = ugcItem.description,
                rcmdReason = ugcItem.rcmdReason,
            )
        assertThat(briefMeta.tagName).isEqualTo("数码")
        assertThat(briefMeta.description).isEqualTo("热门接口直接返回的简介文案")
    }
}
