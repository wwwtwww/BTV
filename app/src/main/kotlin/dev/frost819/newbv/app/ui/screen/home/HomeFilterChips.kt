package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.data.datastore.HomeTopNavItem

/**
 * 首页顶部过滤胶囊选项（对标 YouTube TV Filter Chips）。
 */
enum class HomeFilterTab(
    val title: String,
) {
    /** 综合精选：多横向泳道（为你推荐 + 关注更新 + 今日热门）。 */
    Featured("精选"),

    /** 专注推荐：4 列无限瀑布流网格。 */
    Recommend("推荐"),

    /** 专注热门：4 列无限瀑布流网格。 */
    Popular("热门"),

    /** 专注关注动态：4 列无限瀑布流网格。 */
    Dynamics("动态"),
    ;

    companion object {
        /**
         * 从历史 [HomeTopNavItem] 配置映射到 [HomeFilterTab]。
         *
         * 【映射意图说明】：
         * 原版 BV 的首页首选 Tab 默认项为 [HomeTopNavItem.Recommend]（作为首页默认入口）。在 new BV
         * 遵循 10-foot 交互重构后，首页默认入口为多泳道聚合的 [Featured]（精选），包含为你推荐、关注更新与热门，
         * 而纯瀑布流推荐则下沉为次级 Tab [Recommend]。
         * 因此将老配置中的 [HomeTopNavItem.Recommend] 映射到新版的默认落地页 [Featured]，以保证老用户初次进入时
         * 呈现完整的 YouTube TV 风格精选首页。若用户需要直接进入瀑布流推荐，可在 Filter Chips 中切换至「推荐」。
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
 * YouTube TV 风格轻量级 Filter Chips 导航栏。
 *
 * 替代原先沉重阻断式 [dev.frost819.newbv.app.ui.component.TopNav]。
 * 支持胶囊药丸微放大焦点微动效、遥控器方向键穿透、重复点击刷新当前分类。
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeFilterTab.entries.forEachIndexed { index, tab ->
            val isSelected = tab == selectedTab
            val chipModifier =
                if (index == 0 && focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }

            FilterChipItem(
                modifier = chipModifier,
                tab = tab,
                isSelected = isSelected,
                onClick = {
                    if (isSelected) {
                        onTabReselected(tab)
                    } else {
                        onTabSelected(tab)
                    }
                },
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    modifier: Modifier = Modifier,
    tab: HomeFilterTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .height(36.dp)
                .onFocusChanged { isFocused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor =
                    when {
                        isFocused -> MaterialTheme.colorScheme.inverseSurface
                        isSelected -> if (isDarkTheme) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                        else -> Color.Transparent
                    },
                contentColor =
                    when {
                        isFocused -> MaterialTheme.colorScheme.inverseOnSurface
                        isSelected -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    if (isSelected && !isFocused) {
                        Border(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
                            shape = BVShape.pill,
                        )
                    } else {
                        Border.None
                    },
                focusedBorder = Border.None,
            ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
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
