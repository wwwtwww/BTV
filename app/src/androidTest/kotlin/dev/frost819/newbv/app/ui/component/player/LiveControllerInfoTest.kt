package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.repositories.LivePlayLine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [LiveControllerInfo] 的插桩测试。
 *
 * 验证标题、人气 meta 行显示与隐藏，以及快捷菜单水平锚点对位。
 */
@RunWith(AndroidJUnit4::class)
class LiveControllerInfoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        title: String,
        areaName: String = "",
        onlineCount: String,
        availableQualities: List<Pair<Int, String>> = emptyList(),
        currentQuality: Int = -1,
        availableLines: List<LivePlayLine> = emptyList(),
        currentLine: Int = 1,
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LiveControllerInfo(
                        show = true,
                        title = title,
                        areaName = areaName,
                        onlineCount = onlineCount,
                        clock = Pair(20, 30),
                        isPlaying = true,
                        danmakuEnabled = true,
                        availableQualities = availableQualities,
                        currentQuality = currentQuality,
                        availableLines = availableLines,
                        currentLine = currentLine,
                        onPlayPause = {},
                        onRefresh = {},
                        onDanmakuSwitchChange = {},
                        onShowSettings = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun displays_title_and_online_count() {
        setContent(title = "测试直播标题", onlineCount = "1.2万")

        composeRule.onNodeWithText("测试直播标题").assertIsDisplayed()
        composeRule.onNodeWithText("人气 1.2万").assertIsDisplayed()
    }

    @Test
    fun hides_online_count_when_blank() {
        setContent(title = "标题", onlineCount = "")

        composeRule.onNodeWithText("人气", substring = true).assertDoesNotExist()
    }

    @Test
    fun displays_area_name_when_present() {
        setContent(title = "标题", areaName = "王者荣耀", onlineCount = "5769")

        composeRule.onNodeWithText("王者荣耀").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun live_quick_menu_popover_aligns_horizontally_with_button() {
        setContent(
            title = "测试直播",
            onlineCount = "1000",
            availableQualities = listOf(10000 to "原画", 4000 to "蓝光"),
            currentQuality = 10000,
            availableLines =
                listOf(
                    LivePlayLine(order = 1, url = "https://live1.example.com"),
                    LivePlayLine(order = 2, url = "https://live2.example.com"),
                ),
            currentLine = 1,
        )

        val qualityButtonNode = composeRule.onNodeWithText("原画")
        val buttonLeft = qualityButtonNode.getUnclippedBoundsInRoot().left

        qualityButtonNode.requestFocus()
        composeRule.waitForIdle()
        qualityButtonNode.performKeyInput {
            pressKey(Key.Enter)
        }
        composeRule.waitForIdle()

        val popoverTitleLeft = composeRule.onNodeWithText("画质清晰度").getUnclippedBoundsInRoot().left
        assertThat((popoverTitleLeft - buttonLeft).value).isWithin(20f).of(16f)
    }

    @Test
    fun falls_back_to_first_quality_badge_when_current_quality_unspecified() {
        // 当 currentQuality 缺省 (-1) 时，自动回退展示列表首项（"原画"），防止展示空/泛化文案
        setContent(
            title = "测试直播",
            onlineCount = "888",
            availableQualities = listOf(10000 to "原画", 4000 to "蓝光"),
            currentQuality = -1,
        )

        composeRule.onNodeWithText("原画").assertIsDisplayed()
    }
}
