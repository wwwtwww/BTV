package dev.frost819.newbv.app.ui.component.search

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Text
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.screen.home.HomeFilterTab
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.core.theme.BVTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 搜索页核心 UI 组件集（软键盘、宽幅建议、历史胶囊、热搜词、UP 主卡片、筛选弹窗与 TopNav 尾槽）插桩测试。
 */
@RunWith(AndroidJUnit4::class)
class SearchComponentsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun softKeyboard_displaysKeysAndTriggersCallbacks() {
        var typed = ""
        var searched = false

        composeRule.setContent {
            BVTheme {
                SoftKeyboard(
                    firstButtonFocusRequester = remember { FocusRequester() },
                    onClick = { typed += it },
                    onClear = { typed = "" },
                    onDelete = { typed = typed.dropLast(1) },
                    onSearch = { searched = true },
                )
            }
        }

        composeRule.onNodeWithText("A").performClick()
        composeRule.onNodeWithText("1").performClick()
        assertThat(typed).isEqualTo("A1")

        composeRule.onNodeWithText("删除").performClick()
        assertThat(typed).isEqualTo("A")

        composeRule.onNodeWithText("搜索").performClick()
        assertThat(searched).isTrue()

        composeRule.onNodeWithText("清除").performClick()
        assertThat(typed).isEmpty()
    }

    @Test
    fun wideSearchSuggestAndHotwordAndHistoryChip_displayAndClick() {
        var selectedSuggest = ""
        var selectedHotword = ""
        var deletedChip = false

        composeRule.setContent {
            BVTheme {
                WideSearchSuggestItem(
                    keyword = "原神 4.8 版本前瞻特别节目完整回放",
                    onClick = { selectedSuggest = "原神 4.8 版本前瞻特别节目完整回放" },
                )
                SearchHotwordItem(
                    rank = 1,
                    showName = "黑神话悟空终极预告",
                    onClick = { selectedHotword = "黑神话悟空终极预告" },
                )
                SearchHistoryChip(
                    keyword = "影视飓风",
                    deleteMode = true,
                    onClick = { deletedChip = true },
                )
            }
        }

        composeRule.onNodeWithText("原神 4.8 版本前瞻特别节目完整回放").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("黑神话悟空终极预告").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("影视飓风").assertIsDisplayed().performClick()

        assertThat(selectedSuggest).isEqualTo("原神 4.8 版本前瞻特别节目完整回放")
        assertThat(selectedHotword).isEqualTo("黑神话悟空终极预告")
        assertThat(deletedChip).isTrue()
    }

    @Test
    fun upCard_displaysUsernameAndFallbackSign() {
        var clicked = false

        composeRule.setContent {
            BVTheme {
                UpCard(
                    avatar = "",
                    username = "罗翔说刑法",
                    sign = "",
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("罗翔说刑法").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("这个人很神秘").assertIsDisplayed()
        assertThat(clicked).isTrue()
    }

    @Test
    fun searchResultFilter_selectsOptionsAndConfirms() {
        var confirmedOrder: SearchFilterOrderType? = null
        var confirmedDuration: SearchFilterDuration? = null

        composeRule.setContent {
            BVTheme {
                SearchResultFilter(
                    selectedOrder = SearchFilterOrderType.ComprehensiveSort,
                    selectedDuration = SearchFilterDuration.All,
                    onConfirm = { order, duration ->
                        confirmedOrder = order
                        confirmedDuration = duration
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("最多点击").performClick()
        composeRule.onNodeWithText("10-30分钟").performClick()
        composeRule.onNodeWithText("确定").performClick()

        assertThat(confirmedOrder).isEqualTo(SearchFilterOrderType.MostClicks)
        assertThat(confirmedDuration).isEqualTo(SearchFilterDuration.Between10And30Minutes)
    }

    @Test
    fun topNav_displaysTrailingContentSlot() {
        composeRule.setContent {
            BVTheme {
                TopNav(
                    items = listOf(HomeFilterTab.Featured, HomeFilterTab.Recommend),
                    selectedIndex = 0,
                    trailingContent = {
                        Text(text = "筛选")
                    },
                )
            }
        }

        composeRule.onNodeWithText("筛选").assertIsDisplayed()
    }
}
