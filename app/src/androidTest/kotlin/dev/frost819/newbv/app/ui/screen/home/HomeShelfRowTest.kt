package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [HomeShelfRow] 的 Compose UI 插桩测试。
 *
 * 验证泳道标题、视频卡片及末尾「查看全部」卡片的渲染与点击交互。
 */
@RunWith(AndroidJUnit4::class)
class HomeShelfRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sampleCard =
        VideoCardData(
            avid = 10086L,
            title = "精选大屏评测视频",
            cover = "https://example.com/cover.jpg",
            upName = "极客大屏",
            upMid = 123L,
            playString = "50万",
            danmakuString = "2000",
            timeString = "12:34",
        )

    @Test
    fun displays_shelf_title_and_see_all_card_at_end() {
        var seeAllClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeShelfRow(
                    shelfKey = "test_shelf",
                    title = "为你推荐",
                    items = listOf(sampleCard),
                    onVideoClick = {},
                    onGoToDetailPage = {},
                    onAddWatchLater = {},
                    onSeeAllClick = { seeAllClicked = true },
                )
            }
        }

        // 验证行标题展示
        composeRule.onNodeWithText("为你推荐").assertIsDisplayed()

        // 验证卡片展示
        composeRule.onNodeWithText("精选大屏评测视频").assertIsDisplayed()

        // 验证末尾「查看全部」卡片元素展示
        composeRule.onNodeWithText("查看全部").assertIsDisplayed()
        composeRule.onNodeWithText("查看更多为你推荐").assertIsDisplayed()
        composeRule.onNodeWithText("进入单分类瀑布流").assertIsDisplayed()

        // 点击「查看全部」卡片
        composeRule.onNodeWithText("查看全部").performClick()
        assertTrue(seeAllClicked)
    }
}
