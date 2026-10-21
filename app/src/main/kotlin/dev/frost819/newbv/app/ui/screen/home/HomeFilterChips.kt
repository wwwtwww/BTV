package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.component.TopNavItem
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.data.datastore.HomeTopNavItem

/**
 * 首页顶部过滤选项。
 */
enum class HomeFilterTab(
    val title: String,
) : TopNavItem {
    /** 综合精选：多横向泳道（为你推荐 + 关注更新 + 今日热门）。 */
    Featured("精选"),

    /** 专注推荐：4 列无限瀑布流网格。 */
    Recommend("推荐"),

    /** 专注热门：4 列无限瀑布流网格。 */
    Popular("热门"),

    /** 专注关注动态：4 列无限瀑布流网格。 */
    Dynamics("动态"),
    ;

    override val displayName: String
        get() = title

    companion object {
        /**
         * 从历史 [HomeTopNavItem] 配置映射到 [HomeFilterTab]。
         *
         * 【映射意图说明】：
         * 原版 BV 的首页首选 Tab 默认项为 [HomeTopNavItem.Recommend]（作为首页默认入口）。在 new BV
         * 遵循 10-foot 交互重构后，首页默认入口为多泳道聚合的 [Featured]（精选），包含为你推荐、关注更新与热门，
         * 而纯瀑布流推荐则下沉为次级 Tab [Recommend]。
         * 因此将老配置中的 [HomeTopNavItem.Recommend] 映射到新版的默认落地页 [Featured]，以保证老用户初次进入时
         * 呈现完整的 YouTube TV 风格精选首页。若用户需要直接进入瀑布流推荐，可在顶栏中切换至「推荐」。
         */
        fun fromHomeTopNavItem(item: HomeTopNavItem): HomeFilterTab =
            when (item) {
                HomeTopNavItem.Recommend -> Featured
                HomeTopNavItem.Popular -> Popular
                HomeTopNavItem.Dynamics -> Dynamics
            }
    }
}

/**
 * 首页顶部导航栏（全站统一的极简单色指示条风格）。
 *
 * 桥接全站通用的 [TopNav]，彻底告别旧版全圆角胶囊形态。
 *
 * @param modifier 修饰符。
 * @param selectedTab 当前选中的 Tab。
 * @param onTabSelected 选中切换回调。
 * @param onTabReselected 点击已选中的 Tab 触发刷新回调。
 * @param focusRequester 初始焦点请求器。
 */
@Composable
fun HomeFilterChips(
    modifier: Modifier = Modifier,
    selectedTab: HomeFilterTab,
    onTabSelected: (HomeFilterTab) -> Unit,
    onTabReselected: (HomeFilterTab) -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    TopNav(
        modifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier,
        items = HomeFilterTab.entries,
        selectedIndex = HomeFilterTab.entries.indexOf(selectedTab),
        isLargePadding = false,
        onSelectedChanged = { onTabSelected(it as HomeFilterTab) },
        onClick = { onTabReselected(it as HomeFilterTab) },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun HomeFilterChipsPreview() {
    BVTheme {
        HomeFilterChips(
            selectedTab = HomeFilterTab.Featured,
            onTabSelected = {},
        )
    }
}
