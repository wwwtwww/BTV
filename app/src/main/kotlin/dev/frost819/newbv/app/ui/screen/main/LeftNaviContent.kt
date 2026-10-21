package dev.frost819.newbv.app.ui.screen.main

import dev.frost819.newbv.core.theme.BVShape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.core.focus.isDpadRight
import dev.frost819.newbv.core.focus.isKeyDown
import dev.frost819.newbv.data.datastore.LeftNaviItem

/**
 * YouTube TV 风格展开式抽屉导航栏。
 *
 * 默认处于紧凑折叠状态（仅图标，宽 72dp），最大化内容显示空间；
 * 当焦点进入左侧导航栏时，抽屉平滑向右展开（宽 220dp），
 * 展示完整功能文本与用户信息；
 * 焦点向右移出时折叠（宽 72dp）并精准恢复内容区焦点。
 *
 * @param modifier 外部修饰符。
 * @param isLogin 是否已登录。
 * @param avatar 头像 URL。
 * @param username 用户名称。
 * @param selectedItem 当前选中的导航项。
 * @param onLeftNaviItemChanged 导航项切换回调。
 * @param onOpenSettings 打开设置回调。
 * @param onShowUserPanel 显示用户面板回调。
 * @param onFocusToContent 聚焦内容区回调，返回是否成功。
 * @param onLogin 登录回调。
 * @param focusSaver 焦点恢复器（由 MainScreen 共享传入）。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LeftNaviContent(
    modifier: Modifier = Modifier,
    isLogin: Boolean = false,
    avatar: String = "",
    username: String = "",
    selectedItem: LeftNaviItem,
    onLeftNaviItemChanged: (LeftNaviItem) -> Unit,
    onOpenSettings: () -> Unit,
    onShowUserPanel: () -> Unit,
    onFocusToContent: () -> Boolean,
    onLogin: () -> Unit,
    focusSaver: FocusSaver,
) {
    val focusManager = LocalFocusManager.current
    var hasLocalFocus by remember { mutableStateOf(false) }

    // 冷启动时系统会自动聚焦左侧栏头像，产生一次瞬时 hasLocalFocus=true，
    // 随后 MainScreen 的 LaunchedEffect 会把焦点抢回内容区；若首帧即用焦点驱动展开，
    // 抽屉会“展开又收起”闪烁一次。跳过首帧后再启用焦点驱动的展开。
    var focusSettled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusSettled = true
    }

    val isExpanded = focusSettled && hasLocalFocus
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val drawerWidth by animateDpAsState(
        targetValue = if (isExpanded) 220.dp else 72.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "drawer-width",
    )

    Surface(
        modifier =
            modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .onFocusChanged { hasLocalFocus = it.hasFocus }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.isDpadRight() && keyEvent.isKeyDown()) {
                        if (onFocusToContent() || focusManager.moveFocus(FocusDirection.Right)) {
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
        colors =
            SurfaceDefaults.colors(
                containerColor =
                    if (isDarkTheme) {
                        if (isExpanded) Color(0xFF141720) else Color(0xFF0C101A)
                    } else {
                        if (isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 用户头像与信息
            DrawerNavItem(
                modifier = Modifier.focusSaverItem(focusSaver, "user"),
                isExpanded = isExpanded,
                isSelected = false,
                onClick = {
                    if (isLogin) {
                        onShowUserPanel()
                    } else {
                        onLogin()
                    }
                },
                icon = {
                    if (isLogin) {
                        Surface(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                            colors =
                                SurfaceDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        ) {
                            AsyncImage(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                model = avatar,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
                label = if (isLogin) username.ifBlank { "我的账户" } else "点击登录",
                subtitle = if (isLogin) "个人中心" else "扫码或输入",
            )

            // 中间导航项
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                LeftNaviItem.entries.forEach { item ->
                    DrawerNavItem(
                        isExpanded = isExpanded,
                        isSelected = item == selectedItem,
                        onClick = { onLeftNaviItemChanged(item) },
                        icon = {
                            Icon(
                                imageVector = item.displayIcon,
                                contentDescription = null,
                            )
                        },
                        label = item.displayName,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // 底部设置
            DrawerNavItem(
                modifier = Modifier.focusSaverItem(focusSaver, "settings"),
                isExpanded = isExpanded,
                isSelected = false,
                onClick = onOpenSettings,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                    )
                },
                label = "设置",
            )
        }
    }
}

/**
 * 抽屉行单项组件。
 *
 * @param modifier 修饰符。
 * @param isExpanded 抽屉是否处于展开状态。
 * @param isSelected 当前项是否被选中。
 * @param onClick 点击事件回调。
 * @param icon 图标槽位 Composable。
 * @param label 主文本标签。
 * @param subtitle 可选副文本（展开态展示）。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DrawerNavItem(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    subtitle: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val indicatorColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MaterialTheme.colorScheme.border
            } else {
                Color.Transparent
            },
        label = "selection-indicator",
    )

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .onFocusChanged {
                    isFocused = it.hasFocus
                }.selectionIndicator(indicatorColor),
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.medium),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor =
                    if (isSelected && !isFocused) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    } else {
                        Color.Transparent
                    },
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120)),
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFocused) MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** 左侧导航项的图标与显示名称扩展。 */
val LeftNaviItem.displayIcon: ImageVector
    get() =
        when (this) {
            LeftNaviItem.Search -> Icons.Default.Search
            LeftNaviItem.Personal -> Icons.Default.Person
            LeftNaviItem.Home -> Icons.Default.Home
            LeftNaviItem.UGC -> Icons.Default.OndemandVideo
            LeftNaviItem.PGC -> Icons.Default.Movie
            LeftNaviItem.Live -> Icons.Default.LiveTv
        }

/** 左侧导航项的显示名称扩展。 */
val LeftNaviItem.displayName: String
    get() =
        when (this) {
            LeftNaviItem.Search -> "搜索"
            LeftNaviItem.Personal -> "个人"
            LeftNaviItem.Home -> "主页"
            LeftNaviItem.UGC -> "分区"
            LeftNaviItem.PGC -> "影视"
            LeftNaviItem.Live -> "直播"
        }

/** 绘制左侧选中指示条。 */
private fun Modifier.selectionIndicator(color: Color): Modifier =
    this.drawBehind {
        val strokeWidth = 4.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(width = strokeWidth, height = size.height),
        )
    }

@Preview(showBackground = true, heightDp = 1080)
@Composable
private fun LeftNaviContentPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        LeftNaviContent(
            isLogin = true,
            avatar = "",
            username = "测试用户",
            selectedItem = LeftNaviItem.Home,
            onLeftNaviItemChanged = {},
            onOpenSettings = {},
            onShowUserPanel = {},
            onFocusToContent = { true },
            onLogin = {},
            focusSaver =
                dev.frost819.newbv.app.ui.component
                    .rememberFocusSaver(),
        )
    }
}
