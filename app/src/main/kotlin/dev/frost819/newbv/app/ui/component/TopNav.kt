package dev.frost819.newbv.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.LocalIsDarkTheme

/**
 * 顶部导航项接口。
 */
interface TopNavItem {
    val displayName: String
}

/**
 * 顶部导航 Tab 栏（极简文字 + 单色黑白滑动指示条）。
 *
 * 遵循 10-foot 大屏视觉与交互规范，去除非必要的胶囊/矩形背景框与彩色边框，
 * 采用靠左对齐排版、纯平稳字阶对比（零几何放大跳跃）与精致平滑的单色滑动指示条。
 *
 * @param modifier 修饰符。
 * @param items Tab 项列表。
 * @param selectedIndex 当前选中的 Tab 索引。
 * @param isLargePadding 内容区未获焦点时是否使用较大内边距。
 * @param onSelectedChanged Tab 焦点切换回调。
 * @param onClick Tab 点击回调（常用于刷新）。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    selectedIndex: Int = 0,
    isLargePadding: Boolean = false,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val isDarkTheme = LocalIsDarkTheme.current

    var selectedTabIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        selectedTabIndex = selectedIndex
    }

    val verticalPadding by animateDpAsState(
        targetValue = if (isLargePadding) 12.dp else 6.dp,
        label = "top-nav-padding",
    )

    // 单色极简指示条色值：深色模式纯白，浅色模式纯黑/深灰
    val indicatorActiveColor = if (isDarkTheme) Color.White else Color(0xFF111827)
    val indicatorInactiveColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF111827).copy(alpha = 0.6f)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = verticalPadding, bottom = verticalPadding),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabRow(
            modifier =
                Modifier
                    .focusRestorer(focusRequester),
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            separator = { Spacer(modifier = Modifier.width(8.dp)) },
            indicator = { tabPositions, doesTabRowHaveFocus ->
                if (selectedTabIndex in tabPositions.indices) {
                    TabRowDefaults.UnderlinedIndicator(
                        currentTabPosition = tabPositions[selectedTabIndex],
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = indicatorActiveColor,
                        inactiveColor = indicatorInactiveColor,
                    )
                }
            },
        ) {
            items.forEachIndexed { index, tab ->
                NavItemTab(
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                    topNavItem = tab,
                    selected = index == selectedTabIndex,
                    onFocus = {
                        selectedTabIndex = index
                        onSelectedChanged(tab)
                    },
                    onClick = { onClick(tab) },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabRowScope.NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    val isDarkTheme = LocalIsDarkTheme.current
    var hasFocus by remember { mutableStateOf(false) }

    // 纯平稳字阶对比：未选中 50% 弱灰，选中/获焦 100% 纯白或纯黑
    val contentActiveColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF111827).copy(alpha = 0.5f)
    val contentFocusedColor =
        if (isDarkTheme) Color.White else Color(0xFF111827)
    val contentSelectedColor =
        if (isDarkTheme) Color.White else Color(0xFF111827)

    Tab(
        modifier =
            modifier
                .onFocusChanged { hasFocus = it.hasFocus }
                .touchClickable(onClick = onClick),
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
        colors =
            TabDefaults.underlinedIndicatorTabColors(
                contentColor = contentActiveColor,
                focusedContentColor = contentFocusedColor,
                selectedContentColor = contentSelectedColor,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = topNavItem.displayName,
                color = if (selected || hasFocus) contentFocusedColor else contentActiveColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected || hasFocus) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

private data class DummyTopNavItem(
    override val displayName: String,
) : TopNavItem

@Preview(showBackground = true)
@Composable
private fun TopNavPreview() {
    BVTheme {
        TopNav(
            items =
                listOf(
                    DummyTopNavItem("精选"),
                    DummyTopNavItem("推荐"),
                    DummyTopNavItem("热门"),
                    DummyTopNavItem("动态"),
                ),
            isLargePadding = true,
        )
    }
}
