package dev.frost819.newbv.app.ui.component.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [HomeHeroBanner] 首页沉浸看板的 Compose UI 插桩测试。
 *
 * 验证完整动态模式、低配省流静态降级模式及关闭模式下的组件渲染与显存保护行为。
 */
@RunWith(AndroidJUnit4::class)
class HomeHeroBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleHero(description: String? = "这是一段用于电视大屏展示的双行精选视频简介") =
        HeroShowcaseData(
            avid = 10086L,
            bvid = "BV10086",
            title = "4K HDR 沉浸式大屏体验深度评测",
            cover = "https://i0.hdslb.com/bfs/archive/sample.jpg",
            upName = "极客放映室",
            playString = "36.8万",
            danmakuString = "2400",
            timeString = "18:45",
            pubTime = "2小时前",
            tagName = "科技数码",
            rcmdReason = "飙升",
            description = description,
            sourceShelfTitle = "为你推荐",
        )

    @Test
    fun renders_nothing_when_mode_is_off_or_showcase_is_null() {
        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                HomeHeroBanner(
                    showcase = sampleHero(),
                    effectiveMode = EffectiveHeroMode.Off,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HOME_HERO_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun renders_full_dynamic_poster_and_metadata_in_dark_theme() {
        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                HomeHeroBanner(
                    showcase = sampleHero(),
                    effectiveMode = EffectiveHeroMode.FullDynamic,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HOME_HERO_BANNER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_HERO_POSTER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_HERO_CATEGORY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_HERO_STATIC_BADGE_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("为你推荐").assertIsDisplayed()
        composeRule.onNodeWithText("科技数码").assertIsDisplayed()
        composeRule.onNodeWithText("飙升").assertIsDisplayed()
        composeRule.onNodeWithText("4K HDR 沉浸式大屏体验深度评测").assertIsDisplayed()
        composeRule.onNodeWithText("极客放映室").assertIsDisplayed()
        composeRule.onNodeWithText("这是一段用于电视大屏展示的双行精选视频简介").assertIsDisplayed()
    }

    @Test
    fun omits_poster_bitmap_and_shows_static_badge_in_static_degraded_mode() {
        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Light) {
                HomeHeroBanner(
                    showcase = sampleHero(description = null),
                    effectiveMode = EffectiveHeroMode.StaticDegraded,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HOME_HERO_BANNER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_HERO_STATIC_BACKDROP_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_HERO_STATIC_BADGE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_HERO_POSTER_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("4K HDR 沉浸式大屏体验深度评测").assertIsDisplayed()
        composeRule.onNodeWithText("来自 极客放映室 的精选投稿 · 按确认键即刻播放，按菜单键打开快捷菜单").assertIsDisplayed()
    }
}
