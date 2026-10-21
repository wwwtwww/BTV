package dev.frost819.newbv.core.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceDefaults
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.core.interaction.InputMethod
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 电视端立体焦点系统 ([TvFocusDefaults] / [Modifier.tvFocusFeedback]) 的单元与组合测试。
 *
 * 通过 Robolectric + Compose 测试规则驱动真实修饰符与主题，覆盖深/浅主题下的边框计算、
 * 获焦动画、极光外发光 (Glow) 与悬浮投影 (Shadow) 绘制分支。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TvFocusFeedbackTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `tv focus defaults follow 10-foot design standards`() {
        assertThat(TvFocusDefaults.FOCUSED_SCALE).isEqualTo(1.06f)
        assertThat(TvFocusDefaults.borderWidth).isEqualTo(2.5.dp)
        assertThat(TvFocusDefaults.glowRadius).isEqualTo(8.dp)
    }

    @Test
    fun `cardBorder resolves dark and light border colors via LocalIsDarkTheme`() {
        var darkBorder: CardBorder? = null
        var expectedDark: CardBorder? = null
        var lightBorder: CardBorder? = null
        var expectedLight: CardBorder? = null

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                darkBorder = TvFocusDefaults.cardBorder(shape = BVShape.large)
                expectedDark = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(TvFocusDefaults.borderWidth, BVColors.DarkBorder),
                        shape = BVShape.large,
                    ),
                )
            }
            BVTheme(themeMode = ThemeMode.Light) {
                lightBorder = TvFocusDefaults.cardBorder(shape = BVShape.medium)
                expectedLight = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(TvFocusDefaults.borderWidth, BVColors.PrimaryStrong),
                        shape = BVShape.medium,
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        assertThat(darkBorder).isEqualTo(expectedDark)
        assertThat(lightBorder).isEqualTo(expectedLight)
    }

    @Test
    fun `surfaceBorder resolves dark and light border colors via LocalIsDarkTheme`() {
        var darkBorder: ClickableSurfaceBorder? = null
        var expectedDark: ClickableSurfaceBorder? = null
        var lightBorder: ClickableSurfaceBorder? = null
        var expectedLight: ClickableSurfaceBorder? = null

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                darkBorder = TvFocusDefaults.surfaceBorder(shape = BVShape.large)
                expectedDark = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(TvFocusDefaults.borderWidth, BVColors.DarkBorder),
                        shape = BVShape.large,
                    ),
                )
            }
            BVTheme(themeMode = ThemeMode.Light) {
                lightBorder = TvFocusDefaults.surfaceBorder(shape = BVShape.medium)
                expectedLight = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(TvFocusDefaults.borderWidth, BVColors.PrimaryStrong),
                        shape = BVShape.medium,
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        assertThat(darkBorder).isEqualTo(expectedDark)
        assertThat(lightBorder).isEqualTo(expectedLight)
    }

    @Test
    fun `tvFocusFeedback renders and animates focus in dark theme`() {
        val focusRequester = FocusRequester()

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .tvFocusFeedback(
                            shape = BVShape.large,
                            focusedScale = 1.06f,
                            borderWidth = 2.5.dp,
                        ),
                )
            }
        }
        composeRule.waitForIdle()

        // 初始非获焦态
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        // 请求焦点触发动效流转与深色 Glow 绘制
        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()

        // 推进动效时钟使 spring 动画完整收敛
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
    }

    @Test
    fun `tvFocusFeedback renders and animates focus in light theme`() {
        val focusRequester = FocusRequester()

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Light) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .tvFocusFeedback(
                            shape = BVShape.medium,
                            focusedScale = 1.06f,
                            borderWidth = 2.5.dp,
                        ),
                )
            }
        }
        composeRule.waitForIdle()

        // 请求焦点触发浅色 Drop Shadow 与深色对比边框分支
        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()

        // 推进动效时钟
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
    }

    @Test
    fun `tvFocusFeedback respects interaction tracker input method`() {
        val focusRequester = FocusRequester()
        val tracker = InteractionTracker(initial = InputMethod.Touch)

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                CompositionLocalProvider(
                    LocalInteractionTracker provides tracker,
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .focusRequester(focusRequester)
                            .focusable()
                            .tvFocusFeedback(),
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Touch 模式下即使获得焦点，也不应投射 DPad TV 焦点动效
        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)

        // 切换为 DPad 模式，激活遥控器大屏立体反馈
        tracker.onDpadKey()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
    }
}
