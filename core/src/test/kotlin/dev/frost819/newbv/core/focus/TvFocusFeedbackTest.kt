package dev.frost819.newbv.core.focus

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardBorder
import androidx.tv.material3.ClickableSurfaceBorder
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.core.interaction.InputMethod
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 电视端立体焦点系统 ([TvFocusDefaults] / [Modifier.tvFocusFeedback]) 的单元与行为测试。
 *
 * 验证深浅主题边框尺寸契约、非均匀圆角绘制安全、D-pad 获焦与 Touch 模式门控流转，
 * 避免在测试中过度复刻生产构造公式。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTvFocusApi::class)
class TvFocusFeedbackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cardBorder resolves non-null distinct border configurations across dark and light themes with same shape`() {
        var darkBorder: CardBorder? = null
        var lightBorder: CardBorder? = null

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                darkBorder = TvFocusDefaults.cardBorder(shape = BVShape.large)
            }
            BVTheme(themeMode = ThemeMode.Light) {
                lightBorder = TvFocusDefaults.cardBorder(shape = BVShape.large)
            }
        }
        composeRule.waitForIdle()

        // 验证返回合法边框对象，在完全相同的 shape 下深浅主题自适应颜色产生实质区分度
        assertThat(darkBorder).isNotNull()
        assertThat(lightBorder).isNotNull()
        assertThat(darkBorder).isNotEqualTo(lightBorder)
    }

    @Test
    fun `surfaceBorder resolves distinct configurations across dark and light themes with same shape`() {
        var darkBorder: ClickableSurfaceBorder? = null
        var lightBorder: ClickableSurfaceBorder? = null

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                darkBorder = TvFocusDefaults.surfaceBorder(shape = BVShape.large)
            }
            BVTheme(themeMode = ThemeMode.Light) {
                lightBorder = TvFocusDefaults.surfaceBorder(shape = BVShape.large)
            }
        }
        composeRule.waitForIdle()

        assertThat(darkBorder).isNotNull()
        assertThat(lightBorder).isNotNull()
        assertThat(darkBorder).isNotEqualTo(lightBorder)
    }

    @Test
    fun `tvFocusFeedback with asymmetric shapes renders roundRect path correctly`() {
        // 关键断言：验证非均匀圆角（如 drawerStart 与 bannerEnd）在获焦时通过 Path 精确绘制，不抛出异常
        val focusRequester = FocusRequester()

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .testTag("asymmetric_card")
                            .focusRequester(focusRequester)
                            .tvFocusFeedback(
                                shape = BVShape.drawerStart,
                                focusedScale = 1.06f,
                            ).focusable(),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag("asymmetric_card")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, true))
    }

    @Test
    fun `tvFocusFeedback renders and animates focus in dark theme with behavior assertions`() {
        val focusRequester = FocusRequester()

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .testTag("focus_box_dark")
                            .focusRequester(focusRequester)
                            .tvFocusFeedback(
                                shape = BVShape.large,
                                focusedScale = 1.06f,
                                borderWidth = 2.5.dp,
                            ).focusable(),
                )
            }
        }
        composeRule.waitForIdle()

        // 初始未获焦态断言：反馈未激活
        composeRule
            .onNodeWithTag("focus_box_dark")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, false))

        // 请求焦点触发动效流转与深色 Glow 绘制
        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()

        // 推进动效时钟使 spring 动画完整收敛
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()

        // 获焦态断言：深色模式大屏立体焦点反馈激活
        composeRule
            .onNodeWithTag("focus_box_dark")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, true))
    }

    @Test
    fun `tvFocusFeedback renders and animates focus in light theme with behavior assertions`() {
        val focusRequester = FocusRequester()

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Light) {
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .testTag("focus_box_light")
                            .focusRequester(focusRequester)
                            .tvFocusFeedback(
                                shape = BVShape.medium,
                                focusedScale = 1.06f,
                                borderWidth = 2.5.dp,
                            ).focusable(),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag("focus_box_light")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, false))

        // 请求焦点触发浅色 Drop Shadow 与深色对比边框分支
        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag("focus_box_light")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, true))
    }

    @Test
    fun `tvFocusFeedback respects interaction tracker input method with behavior assertions`() {
        val focusRequester = FocusRequester()
        val tracker = InteractionTracker(initial = InputMethod.Touch)

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                CompositionLocalProvider(
                    LocalInteractionTracker provides tracker,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(100.dp)
                                .testTag("tracker_box")
                                .focusRequester(focusRequester)
                                .tvFocusFeedback()
                                .focusable(),
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
        composeRule.waitForIdle()

        // 行为断言：Touch 输入模式下反馈必须被门控拦截抑制
        composeRule
            .onNodeWithTag("tracker_box")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, false))

        // 切换为 DPad 模式，激活遥控器大屏立体反馈
        tracker.onDpadKey()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        // 行为断言：切换为 DPad 后反馈立刻激活生效
        composeRule
            .onNodeWithTag("tracker_box")
            .assert(SemanticsMatcher.expectValue(TvFocusSemantics.IsFocusFeedbackActive, true))
    }
}
