package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * [ControllerVideoInfoBottom] 的插桩测试。
 *
 * 重点验证：
 * 1. 「选集」与「相关推荐」入口彻底解耦；
 * 2. PGC 多集番剧展示「选集」按钮，但隐藏「视频详情 / UP 主空间 / 相关推荐」；
 * 3. 非 PGC 多 P / 合集同时展示「选集」与「相关推荐」，单集仅展示「相关推荐」。
 */
@RunWith(AndroidJUnit4::class)
class ControllerVideoInfoBottomTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        isPgc: Boolean,
        episodesAvailable: Boolean = false,
        relatedAvailable: Boolean = true,
    ) {
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ControllerVideoInfoBottom(
                        isSeeking = false,
                        goTime = 0L,
                        seekerState = SeekerState(totalDuration = 600_000L, currentTime = 120_000L),
                        videoShot = null,
                        videoShotCache = VideoShotImageCache(),
                        isPgc = isPgc,
                        episodesAvailable = episodesAvailable,
                        relatedAvailable = relatedAvailable,
                        danmakuEnabled = true,
                        isLooping = false,
                        onDirectionLeft = {},
                        onDirectionRight = {},
                        onSeekGoTime = {},
                        onSeekToPosition = {},
                        onPlayPause = {},
                        onDanmakuSwitchChange = {},
                        onShowSettings = {},
                        onShowEpisodes = {},
                        onShowRelatedVideos = {},
                        onGoToVideoInfo = {},
                        onToggleLoop = {},
                        onGoToUpPage = {},
                        onShowInteraction = {},
                        onShowComments = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun pgc_multi_episode_shows_episodes_button_and_hides_ugc_only_buttons() {
        setContent(isPgc = true, episodesAvailable = true, relatedAvailable = false)

        composeRule.onNodeWithContentDescription("选集").assertExists()
        composeRule.onNodeWithText("按遥控器「向下」展开底部选集").assertExists()
        composeRule.onNodeWithContentDescription("视频详情").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("UP 主空间").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("相关推荐").assertDoesNotExist()
    }

    @Test
    fun pgc_single_movie_hides_both_episodes_and_related_buttons() {
        setContent(isPgc = true, episodesAvailable = false, relatedAvailable = false)

        composeRule.onNodeWithContentDescription("选集").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("相关推荐").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("视频详情").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("UP 主空间").assertDoesNotExist()
    }

    @Test
    fun non_pgc_multi_episode_shows_both_episodes_and_related_buttons() {
        setContent(isPgc = false, episodesAvailable = true, relatedAvailable = true)

        composeRule.onNodeWithContentDescription("选集").assertExists()
        composeRule.onNodeWithContentDescription("相关推荐").assertExists()
        composeRule.onNodeWithContentDescription("视频详情").assertExists()
        composeRule.onNodeWithContentDescription("UP 主空间").assertExists()
    }

    @Test
    fun non_pgc_single_video_hides_episodes_and_shows_related_button() {
        setContent(isPgc = false, episodesAvailable = false, relatedAvailable = true)

        composeRule.onNodeWithContentDescription("选集").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("相关推荐").assertExists()
        composeRule.onNodeWithText("按遥控器「向下」浏览相关推荐").assertExists()
    }

    @Test
    fun pgc_still_shows_play_danmaku_and_settings_buttons() {
        setContent(isPgc = true)

        composeRule.onNodeWithContentDescription("播放 / 暂停").assertExists()
        composeRule.onNodeWithContentDescription("清晰度与设置").assertExists()
        composeRule.onNodeWithContentDescription("画面比例：默认（原始）").assertExists()
    }
}
