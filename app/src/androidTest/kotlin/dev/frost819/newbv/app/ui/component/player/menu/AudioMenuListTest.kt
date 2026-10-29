package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.Audio
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [AudioMenuList] 的插桩测试。
 *
 * 验证声音菜单音轨选项（含杜比全景声、Hi-Res、192K 等）的渲染与切换交互。
 */
@RunWith(AndroidJUnit4::class)
class AudioMenuListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displays_allAudioOptions_and_handles_selection() {
        var selectedAudio: Audio? = null
        val testAudios =
            listOf(
                Audio.ADolbyAtoms,
                Audio.AHiRes,
                Audio.A192K,
                Audio.A132K,
                Audio.A64K,
            )

        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize().width(300.dp)) {
                    AudioMenuList(
                        audioList = testAudios,
                        currentAudio = Audio.A192K,
                        onAudioChange = { selectedAudio = it },
                        onFocusStateChange = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("杜比全景声 (Dolby Atmos)").assertIsDisplayed()
        composeRule.onNodeWithText("Hi-Res 无损音质").assertIsDisplayed()
        composeRule.onNodeWithText("高品质 192K").assertIsDisplayed()
        composeRule.onNodeWithText("标准 132K").assertIsDisplayed()
        composeRule.onNodeWithText("流畅 64K").assertIsDisplayed()

        composeRule.onNodeWithText("杜比全景声 (Dolby Atmos)").performClick()
        assertThat(selectedAudio).isEqualTo(Audio.ADolbyAtoms)
    }
}
