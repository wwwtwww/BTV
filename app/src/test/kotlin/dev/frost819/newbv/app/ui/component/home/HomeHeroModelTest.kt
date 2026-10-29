package dev.frost819.newbv.app.ui.component.home

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.biliapi.entity.home.VideoBriefMeta
import dev.frost819.newbv.data.datastore.HomeHeroMode
import org.junit.jupiter.api.Test

/**
 * [HomeHeroBanner] 纯逻辑模型与辅助函数的单元测试。
 *
 * 验证低内存设备降级策略解析、B 站封面 URL 显存优化裁剪、多行简介清洗及元数据合并逻辑。
 */
class HomeHeroModelTest {
    @Test
    fun `resolveEffectiveHeroMode maps Auto based on low spec flag`() {
        assertThat(resolveEffectiveHeroMode(HomeHeroMode.Auto, isLowSpecDevice = false))
            .isEqualTo(EffectiveHeroMode.FullDynamic)
        assertThat(resolveEffectiveHeroMode(HomeHeroMode.Auto, isLowSpecDevice = true))
            .isEqualTo(EffectiveHeroMode.StaticDegraded)
    }

    @Test
    fun `resolveEffectiveHeroMode honors explicit user overrides`() {
        assertThat(resolveEffectiveHeroMode(HomeHeroMode.Full, isLowSpecDevice = true))
            .isEqualTo(EffectiveHeroMode.FullDynamic)
        assertThat(resolveEffectiveHeroMode(HomeHeroMode.StaticLowSpec, isLowSpecDevice = false))
            .isEqualTo(EffectiveHeroMode.StaticDegraded)
        assertThat(resolveEffectiveHeroMode(HomeHeroMode.Off, isLowSpecDevice = false))
            .isEqualTo(EffectiveHeroMode.Off)
        assertThat(resolveEffectiveHeroMode(HomeHeroMode.Off, isLowSpecDevice = true))
            .isEqualTo(EffectiveHeroMode.Off)
    }

    @Test
    fun `optimizeBiliHeroCoverUrl upgrades http and appends 960x540 webp suffix`() {
        val raw = "http://i0.hdslb.com/bfs/archive/sample_cover.jpg"
        assertThat(optimizeBiliHeroCoverUrl(raw))
            .isEqualTo("https://i0.hdslb.com/bfs/archive/sample_cover.jpg@960w_540h_1c.webp")
    }

    @Test
    fun `optimizeBiliHeroCoverUrl preserves query parameters and existing resize suffix`() {
        val withQuery = "//i2.hdslb.com/bfs/archive/abc.png?token=123"
        assertThat(optimizeBiliHeroCoverUrl(withQuery))
            .isEqualTo("https://i2.hdslb.com/bfs/archive/abc.png@960w_540h_1c.webp?token=123")

        val alreadySized = "https://i1.hdslb.com/bfs/archive/abc.jpg@672w_378h_1c.webp"
        assertThat(optimizeBiliHeroCoverUrl(alreadySized))
            .isEqualTo(alreadySized)
    }

    @Test
    fun `optimizeBiliHeroCoverUrl handles blank and non-bilibili urls safely`() {
        assertThat(optimizeBiliHeroCoverUrl("   ")).isEmpty()
        assertThat(optimizeBiliHeroCoverUrl("http://example.com/poster.jpg"))
            .isEqualTo("https://example.com/poster.jpg")
    }

    @Test
    fun `normalizeHeroDescription filters empty placeholder and collapses multiline text`() {
        assertThat(normalizeHeroDescription(null)).isNull()
        assertThat(normalizeHeroDescription("")).isNull()
        assertThat(normalizeHeroDescription("  -  ")).isNull()
        assertThat(normalizeHeroDescription("第一行核心看点\n-\n  第二行补充说明   详情  "))
            .isEqualTo("第一行核心看点 · 第二行补充说明 详情")
    }

    @Test
    fun `fromVideoCardData and mergeBriefMeta preserve existing fields and fill missing ones`() {
        val card =
            VideoCardData(
                avid = 42L,
                bvid = "BV1xx411c7mD",
                title = "  大屏沉浸式体验实测  ",
                cover = "https://i0.hdslb.com/bfs/archive/hero.jpg",
                upName = "测试UP主",
                playString = "25.6万",
                danmakuString = "1820",
                timeString = "12:34",
                rcmdReason = "百万播放",
            )

        val initial = HeroShowcaseData.fromVideoCardData(card, shelfTitle = "为你推荐")
        assertThat(initial.title).isEqualTo("大屏沉浸式体验实测")
        assertThat(initial.tagName).isNull()
        assertThat(initial.description).isNull()
        assertThat(initial.displaySummary).contains("测试UP主")

        val enriched =
            initial.mergeBriefMeta(
                VideoBriefMeta(
                    aid = 42L,
                    tagName = "数码",
                    description = "深度体验 4K 电视端流体交互\n附性能实测",
                    rcmdReason = "普通推荐",
                ),
            )
        assertThat(enriched.tagName).isEqualTo("数码")
        assertThat(enriched.rcmdReason).isEqualTo("百万播放")
        assertThat(enriched.description).isEqualTo("深度体验 4K 电视端流体交互 · 附性能实测")
        assertThat(enriched.displaySummary).isEqualTo("深度体验 4K 电视端流体交互 · 附性能实测")
    }

    @Test
    fun `fromVideoCardData assigns bangumi default tag for pgc cards`() {
        val pgcCard =
            VideoCardData(
                avid = 88L,
                epid = 1024,
                title = "番剧第 12 话",
                cover = "https://i0.hdslb.com/bfs/bangumi/ep12.jpg",
                upName = "",
            )

        val hero = HeroShowcaseData.fromVideoCardData(pgcCard, shelfTitle = "关注动态")
        assertThat(hero.isPgc).isTrue()
        assertThat(hero.tagName).isEqualTo("番剧影视")
        assertThat(hero.displaySummary).contains("番剧精选内容")
    }
}
