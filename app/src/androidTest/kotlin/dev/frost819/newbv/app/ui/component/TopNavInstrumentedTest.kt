package dev.frost819.newbv.app.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.screen.home.HomeFilterTab
import dev.frost819.newbv.core.theme.BVTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [TopNav] 顶部导航栏的 Android 插桩 UI 测试。
 *
 * 遵循 10-foot 大屏组件设计契约：
 * 1. 焦点转移驱动选择流转：触发 [onSelectedChanged]；
 * 2. 确认/点击按键独立触发：触发 [onClick]（常用于刷新）。
 */
@RunWith(AndroidJUnit4::class)
class TopNavInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displays_all_tabs_correctly() {
        val tabs = listOf(
            HomeFilterTab.Featured,
            HomeFilterTab.Recommend,
            HomeFilterTab.Popular,
            HomeFilterTab.Dynamics,
        )

        composeRule.setContent {
            BVTheme {
                TopNav(
                    items = tabs,
                    selectedIndex = 0,
                )
            }
        }
        composeRule.waitForIdle()

        tabs.forEach { tab ->
            composeRule.onNodeWithText(tab.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun clicking_tab_triggers_onClick_callback() {
        val tabs = listOf(
            HomeFilterTab.Featured,
            HomeFilterTab.Recommend,
            HomeFilterTab.Popular,
            HomeFilterTab.Dynamics,
        )
        var clickedItem: TopNavItem? = null

        composeRule.setContent {
            BVTheme {
                TopNav(
                    items = tabs,
                    selectedIndex = 0,
                    onClick = { clickedItem = it },
                )
            }
        }
        composeRule.waitForIdle()

        // 语义点击触发 onClick 回调
        composeRule.onNodeWithText(HomeFilterTab.Popular.displayName).performClick()
        composeRule.waitForIdle()

        assertThat(clickedItem).isEqualTo(HomeFilterTab.Popular)
    }

    @Test
    fun touch_interaction_transfers_focus_and_triggers_onSelectedChanged() {
        val tabs = listOf(
            HomeFilterTab.Featured,
            HomeFilterTab.Recommend,
            HomeFilterTab.Popular,
            HomeFilterTab.Dynamics,
        )
        var selectedItem: TopNavItem? = null

        composeRule.setContent {
            BVTheme {
                TopNav(
                    items = tabs,
                    selectedIndex = 0,
                    onSelectedChanged = { selectedItem = it },
                )
            }
        }
        composeRule.waitForIdle()

        // 模拟真实触摸输入：获取焦点并触发 onFocus 流转
        composeRule.onNodeWithText(HomeFilterTab.Popular.displayName).performTouchInput {
            click()
        }
        composeRule.waitForIdle()

        assertThat(selectedItem).isEqualTo(HomeFilterTab.Popular)
    }
}
