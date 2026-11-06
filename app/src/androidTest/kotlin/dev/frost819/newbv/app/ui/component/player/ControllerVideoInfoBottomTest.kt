package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiStatus
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiType
import dev.frost819.newbv.biliapi.entity.video.SubtitleType
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
        availableSubtitles: List<Subtitle> = emptyList(),
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
                        availableSubtitles = availableSubtitles,
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun subtitle_quick_menu_popover_aligns_horizontally_with_subtitle_button() {
        // Given
        setContent(
            isPgc = false,
            availableSubtitles =
                listOf(
                    Subtitle(
                        id = 1L,
                        lang = "zh-CN",
                        langDoc = "中文（简体）",
                        url = "https://example.com/1.bcc",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None,
                    ),
                ),
        )
        val subtitleButtonNode = composeRule.onNodeWithText("字幕:关")
        val buttonLeft = subtitleButtonNode.getUnclippedBoundsInRoot().left

        // When: 聚焦并按确认键打开字幕快捷菜单
        subtitleButtonNode.requestFocus()
        composeRule.waitForIdle()
        subtitleButtonNode.performKeyInput {
            pressKey(Key.Enter)
        }
        composeRule.waitForIdle()

        // Then: 弹窗标题节点左边界紧贴字幕按钮左边界（含卡片内边距 16dp，误差不超过 20dp）
        val popoverTitleLeft = composeRule.onNodeWithText("字幕语言").getUnclippedBoundsInRoot().left
        assertThat((popoverTitleLeft - buttonLeft).value).isWithin(20f).of(16f)
    }

    @Test
    fun subtitle_button_is_not_rendered_when_subtitles_empty() {
        // Given: 无可用字幕
        setContent(
            isPgc = false,
            availableSubtitles = emptyList(),
        )

        // Then: 快捷菜单按钮行中不应出现字幕按钮
        composeRule.onNodeWithText("字幕:关").assertDoesNotExist()
        composeRule.onNodeWithText("字幕:开").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("暂无可用字幕").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun active_quick_menu_dismisses_when_show_becomes_false() {
        val showState = androidx.compose.runtime.mutableStateOf(true)
        composeRule.setContent {
            TvMaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ControllerVideoInfoBottom(
                        show = showState.value,
                        isSeeking = false,
                        goTime = 0L,
                        seekerState = SeekerState(totalDuration = 600_000L, currentTime = 120_000L),
                        videoShot = null,
                        videoShotCache = VideoShotImageCache(),
                        isPgc = false,
                        episodesAvailable = false,
                        relatedAvailable = true,
                        danmakuEnabled = true,
                        isLooping = false,
                        availableSubtitles =
                            listOf(
                                Subtitle(
                                    id = 1L,
                                    lang = "zh-CN",
                                    langDoc = "中文（简体）",
                                    url = "https://example.com/1.bcc",
                                    type = SubtitleType.CC,
                                    aiType = SubtitleAiType.Normal,
                                    aiStatus = SubtitleAiStatus.None,
                                ),
                            ),
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

        // 打开字幕快捷微卡片
        val subtitleButtonNode = composeRule.onNodeWithText("字幕:关")
        subtitleButtonNode.requestFocus()
        composeRule.waitForIdle()
        subtitleButtonNode.performKeyInput {
            pressKey(Key.Enter)
        }
        composeRule.waitForIdle()

        // 验证弹窗已显示
        composeRule.onNodeWithText("字幕语言").assertExists()

        // When: 控制栏隐藏（show = false，如超时自动隐藏或按设置键/Menu键打开抽屉）
        showState.value = false
        composeRule.waitForIdle()

        // Then: 快捷微卡片应立即关闭，不再残留
        composeRule.onNodeWithText("字幕语言").assertDoesNotExist()
    }
}
