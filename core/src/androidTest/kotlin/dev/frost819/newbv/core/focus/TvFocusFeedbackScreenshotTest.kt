package dev.frost819.newbv.core.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [Modifier.tvFocusFeedback] 的真实光栅化像素插桩测试。
 *
 * 验证在真实 Android 渲染管道与 Compose Canvas 下，获焦（DPad 焦点）后产生的高光边框与
 * 发光辉光在像素层面发生实质性渲染突变，确保不是无绘制效果的虚拟修饰符。
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTvFocusApi::class)
class TvFocusFeedbackScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tvFocusFeedback_produces_pixel_difference_between_unfocused_and_focused() {
        val focusRequester = FocusRequester()

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .testTag("tv_card")
                            .focusRequester(focusRequester)
                            .background(Color.Black)
                            .tvFocusFeedback(
                                shape = BVShape.medium,
                                focusedScale = 1.06f,
                                borderWidth = 4.dp,
                            ).focusable(),
                )
            }
        }
        composeRule.waitForIdle()

        // 捕获未获焦态像素
        val unfocusedBitmap = composeRule.onNodeWithTag("tv_card").captureToImage().asAndroidBitmap()
        val unfocusedPixels = IntArray(unfocusedBitmap.width * unfocusedBitmap.height)
        unfocusedBitmap.getPixels(
            unfocusedPixels,
            0,
            unfocusedBitmap.width,
            0,
            0,
            unfocusedBitmap.width,
            unfocusedBitmap.height,
        )

        // 请求焦点并等待动画完整收敛
        composeRule.runOnUiThread {
            focusRequester.requestFocus()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()

        // 捕获获焦态像素
        val focusedBitmap = composeRule.onNodeWithTag("tv_card").captureToImage().asAndroidBitmap()
        val focusedPixels = IntArray(focusedBitmap.width * focusedBitmap.height)
        focusedBitmap.getPixels(
            focusedPixels,
            0,
            focusedBitmap.width,
            0,
            0,
            focusedBitmap.width,
            focusedBitmap.height,
        )

        // 验证光栅化差异：高光边框和极光 Glow 投射导致像素变化数大于零
        val changedPixelCount = unfocusedPixels.indices.count { unfocusedPixels[it] != focusedPixels[it] }
        assertThat(changedPixelCount).isGreaterThan(0)

        // 精准断言：获焦态边框/发光区域必须包含高光像素（非纯黑背景），证明实际光栅化了 Glow 与高对比度 Border
        val hasLuminousPixels =
            focusedPixels.any { pixel ->
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                red > 80 || green > 80 || blue > 80
            }
        assertThat(hasLuminousPixels).isTrue()
    }
}
