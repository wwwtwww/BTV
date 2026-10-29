package dev.frost819.newbv.app.ui.component.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [VideoListController] 底部横向胶囊选集轨的插桩 UI 测试。
 *
 * 验证胶囊渲染、当前播放集高亮、超长集数 1-50 / 51-60 分组切换、
 * 合集多 P 二级胶囊轨的点击回调、以及 show=false 与空列表守卫。
 */
@RunWith(AndroidJUnit4::class)
class VideoListControllerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun does_not_render_when_show_is_false() {
        val episodes =
            listOf(
                VideoListItem(aid = 1L, cid = 101L, epid = 1, seasonId = 10, title = "1"),
                VideoListItem(aid = 2L, cid = 102L, epid = 2, seasonId = 10, title = "2"),
            )

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                VideoListController(
                    show = false,
                    currentCid = 101L,
                    videoList = episodes,
                    onPlayNewVideo = {},
                    onClose = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("episode_capsule_track").assertDoesNotExist()
    }

    @Test
    fun handles_empty_video_list_gracefully() {
        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                VideoListController(
                    show = true,
                    currentCid = 0L,
                    videoList = emptyList(),
                    onPlayNewVideo = {},
                    onClose = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("episode_capsule_track").assertDoesNotExist()
    }

    @Test
    fun highlights_current_episode_and_triggers_onPlayNewVideo() {
        var clickedItem: VideoListItem? = null
        val episodes =
            listOf(
                VideoListItem(aid = 1L, cid = 101L, epid = 1, seasonId = 10, title = "1"),
                VideoListItem(aid = 2L, cid = 102L, epid = 2, seasonId = 10, title = "2"),
                VideoListItem(aid = 3L, cid = 103L, epid = 3, seasonId = 10, title = "3"),
            )

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                VideoListController(
                    show = true,
                    currentCid = 102L,
                    videoList = episodes,
                    onPlayNewVideo = { clickedItem = it },
                    onClose = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("episode_capsule_track").assertIsDisplayed()
        composeRule.onNodeWithTag("episode_group_row").assertDoesNotExist()
        composeRule.onNode(hasAnyDescendant(hasText("第 2 话"))).assertIsSelected()

        composeRule.onNodeWithText("第 3 话").performClick()
        assertThat(clickedItem?.cid).isEqualTo(103L)
    }

    @Test
    fun renders_group_capsules_for_long_episode_lists_and_switches_group() {
        val longEpisodes =
            (1..60).map { index ->
                VideoListItem(
                    aid = index.toLong(),
                    cid = (1000 + index).toLong(),
                    epid = index,
                    seasonId = 99,
                    title = index.toString(),
                )
            }

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                VideoListController(
                    show = true,
                    currentCid = 1055L,
                    videoList = longEpisodes,
                    onPlayNewVideo = {},
                    onClose = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("episode_group_row").assertIsDisplayed()
        composeRule.onNodeWithText("1-50").assertIsDisplayed()
        composeRule.onNodeWithText("51-60").assertIsDisplayed()
        composeRule.onNodeWithText("第 55 话").assertIsDisplayed()

        // 点击切换到 1-50 分组后展示第 1 话
        composeRule.onNodeWithText("1-50").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第 1 话").assertIsDisplayed()
    }

    @Test
    fun expands_secondary_subpage_track_for_nested_ugc_season_item() {
        var selectedTarget: VideoListItem? = null
        val dim = Dimension(1920, 1080)
        val nestedSeason =
            listOf(
                VideoListItem(
                    aid = 10L,
                    cid = 201L,
                    title = "第一章",
                    ugcPages =
                        listOf(
                            VideoPage(cid = 201L, index = 1, title = "上篇", duration = 300, dimension = dim),
                            VideoPage(cid = 202L, index = 2, title = "下篇", duration = 300, dimension = dim),
                        ),
                ),
                VideoListItem(aid = 20L, cid = 301L, title = "第二章"),
            )

        composeRule.setContent {
            BVTheme(themeMode = ThemeMode.Dark) {
                VideoListController(
                    show = true,
                    currentCid = 201L,
                    currentAid = 10L,
                    videoList = nestedSeason,
                    onPlayNewVideo = { selectedTarget = it },
                    onClose = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("episode_subpage_row").assertIsDisplayed()
        composeRule.onNodeWithText("下篇").performClick()

        assertThat(selectedTarget?.cid).isEqualTo(202L)
        assertThat(selectedTarget?.title).isEqualTo("下篇")
    }
}
