package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.state.player.NextPlayOverlayInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [NextPlayOverlay] 的 UI 与交互测试。
 *
 * 验证播毕连播微浮层卡片在展示时的标题、作者、倒计时文字渲染，
 * 以及触控/确定与取消回调的正确触发。
 */
@RunWith(AndroidJUnit4::class)
class NextPlayOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overlay_hidden_when_visible_is_false() {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    NextPlayOverlay(
                        visible = false,
                        info =
                            NextPlayOverlayInfo(
                                aid = 1001L,
                                cid = 2001L,
                                title = "下一集标题",
                                cover = null,
                                author = "测试UP主",
                                isEpisode = true,
                            ),
                        countdownSec = 5,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("下一集标题").assertDoesNotExist()
    }

    @Test
    fun overlay_displays_metadata_and_triggers_callbacks() {
        var clickedPlay = false
        var clickedCancel = false

        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    NextPlayOverlay(
                        visible = true,
                        info =
                            NextPlayOverlayInfo(
                                aid = 1001L,
                                cid = 2001L,
                                title = "精彩下一集内容",
                                cover = null,
                                author = "高能君",
                                isEpisode = true,
                            ),
                        countdownSec = 4,
                        onClick = { clickedPlay = true },
                        onCancel = { clickedCancel = true },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("下一集").assertIsDisplayed()
        composeRule.onNodeWithText("精彩下一集内容").assertIsDisplayed()
        composeRule.onNodeWithText("高能君").assertIsDisplayed()
        composeRule.onNodeWithText("4 秒后自动播放").assertIsDisplayed()
        composeRule.onNodeWithText("[OK] 立即播 · [返回] 取消").assertIsDisplayed()

        // 验证点击取消触发回调
        composeRule.onNodeWithText("[OK] 立即播 · [返回] 取消").performClick()
        assertThat(clickedCancel).isTrue()

        // 验证点击卡片主体触发起播回调
        composeRule.onNodeWithText("精彩下一集内容").performClick()
        assertThat(clickedPlay).isTrue()
    }
}
