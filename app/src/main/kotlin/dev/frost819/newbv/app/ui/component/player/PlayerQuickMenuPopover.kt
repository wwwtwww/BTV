package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * 快速选择菜单单项数据。
 *
 * @param id 选项唯一标识值
 * @param label 显示主文案
 * @param badge 可选右侧角标（如 "8K", "杜比", "VIP" 等）
 * @param isSelected 是否为当前正在生效的选项
 * @param enabled 是否可选
 */
data class QuickMenuItem<T : Any>(
    val id: T,
    val label: String,
    val badge: String? = null,
    val isSelected: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * 点播/番剧底部 OSD 快捷设置菜单类别。
 */
enum class VodQuickMenuType(
    val title: String,
) {
    Resolution("清晰度"),
    PlaySpeed("播放倍速"),
    Audio("音频音轨"),
    Subtitle("字幕语言"),
    ;

    /**
     * 获取当前菜单的左侧相邻菜单（前一项）。
     */
    fun previous(): VodQuickMenuType? =
        when (this) {
            Resolution -> null
            PlaySpeed -> Resolution
            Audio -> PlaySpeed
            Subtitle -> Audio
        }

    /**
     * 获取当前菜单的右侧相邻菜单（后一项）。
     *
     * @param hasSubtitles 当前视频是否包含可用字幕轨道（无字幕时跳过或阻止进入字幕菜单）
     */
    fun next(hasSubtitles: Boolean = true): VodQuickMenuType? =
        when (this) {
            Resolution -> PlaySpeed
            PlaySpeed -> Audio
            Audio -> if (hasSubtitles) Subtitle else null
            Subtitle -> null
        }
}

/**
 * 直播底部 OSD 快捷设置菜单类别。
 */
enum class LiveQuickMenuType(
    val title: String,
) {
    Quality("画质清晰度"),
    Line("播放线路"),
    ;

    /**
     * 获取当前直播菜单的左侧相邻菜单。
     */
    fun previous(): LiveQuickMenuType? =
        when (this) {
            Quality -> null
            Line -> Quality
        }

    /**
     * 获取当前直播菜单的右侧相邻菜单。
     *
     * @param hasLines 是否存在可用播放线路（无备用线路时不可向右切换至线路菜单）
     */
    fun next(hasLines: Boolean = true): LiveQuickMenuType? =
        when (this) {
            Quality -> if (hasLines) Line else null
            Line -> null
        }
}

/**
 * 计算快捷悬浮菜单在容器内的水平起始像素偏移。
 *
 * 将菜单左边缘与触发按钮左边缘对齐，并在超出容器可用宽度时向右边界钳制，避免小屏或靠右按钮溢出。
 *
 * @param anchorRootX 触发按钮在根坐标系中的左边缘 X（像素），未完成测量时为 null
 * @param containerRootX 浮层容器内容区在根坐标系中的左边缘 X（像素）
 * @param containerWidthPx 浮层容器内容区宽度（像素）
 * @param popoverWidthPx 悬浮菜单自身测量宽度（像素）
 * @return 容器内容区内的水平偏移像素值（`>= 0`）
 */
internal fun calculateQuickMenuOffsetPx(
    anchorRootX: Float?,
    containerRootX: Float,
    containerWidthPx: Int,
    popoverWidthPx: Int,
): Int {
    val safeContainerX = if (containerRootX.isFinite()) containerRootX else 0f
    val safeAnchorX = if (anchorRootX != null && anchorRootX.isFinite()) anchorRootX else safeContainerX
    val rawX = (safeAnchorX - safeContainerX).roundToInt().coerceAtLeast(0)
    val maxX = (containerWidthPx - popoverWidthPx).coerceAtLeast(0)
    return rawX.coerceIn(0, maxX)
}

/**
 * 将快捷悬浮菜单水平对齐到触发按钮的实际起始坐标，并在超出容器右边界时自动钳制。
 *
 * @param anchorRootXProvider 触发按钮在根坐标系中的左边缘 X（像素）提供器
 * @param containerRootXProvider 浮层容器内容区在根坐标系中的左边缘 X（像素）提供器
 */
internal fun Modifier.alignToQuickMenuAnchor(
    anchorRootXProvider: () -> Float?,
    containerRootXProvider: () -> Float,
): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0))
        val offsetX =
            calculateQuickMenuOffsetPx(
                anchorRootX = anchorRootXProvider(),
                containerRootX = containerRootXProvider(),
                containerWidthPx = constraints.maxWidth,
                popoverWidthPx = placeable.width,
            )
        layout(constraints.maxWidth, placeable.height) {
            placeable.place(offsetX, 0)
        }
    }

/**
 * 处理快捷微菜单按键事件。
 *
 * 遥控器按键消费优先级规范：
 * - [Key.Back], [Key.Menu], [Key(763)]：消费并调用 [onDismiss] 关闭微菜单，焦点交还对应底栏按钮，防止事件冒泡唤出大抽屉或关闭底栏；
 * - [Key.DirectionLeft]：消费并调用 [onNavigateLeft] 平移至左侧相邻微菜单（边界无相邻项时仍消费，防止触发 seek）；
 * - [Key.DirectionRight]：消费并调用 [onNavigateRight] 平移至右侧相邻微菜单（边界无相邻项时仍消费，防止触发 seek）。
 *
 * @param type 按键类型（KeyDown/KeyUp 等）
 * @param key 按键键值
 * @param onDismiss 关闭微菜单回调
 * @param onNavigateLeft 向左平移微菜单回调
 * @param onNavigateRight 向右平移微菜单回调
 * @return 是否消费了该按键事件
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun handleQuickMenuKeyEvent(
    type: KeyEventType,
    key: Key,
    onDismiss: () -> Unit,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
): Boolean {
    if (type != KeyEventType.KeyDown) return false
    return when (key) {
        Key.Back, Key.Menu, Key(763) -> {
            onDismiss()
            true
        }
        Key.DirectionLeft -> {
            onNavigateLeft?.invoke()
            // 边界菜单未提供 onNavigateLeft 时仍需消费按键，防止冒泡到父级触发 seek
            true
        }
        Key.DirectionRight -> {
            onNavigateRight?.invoke()
            // 边界菜单未提供 onNavigateRight 时仍需消费按键，防止冒泡到父级触发 seek
            true
        }
        else -> false
    }
}

/**
 * 处理快捷微菜单按键事件（[KeyEvent] 重载）。
 *
 * @param keyEvent 键盘/遥控器输入事件
 * @param onDismiss 关闭微菜单回调
 * @param onNavigateLeft 向左平移微菜单回调
 * @param onNavigateRight 向右平移微菜单回调
 * @return 是否消费了该按键事件
 */
fun handleQuickMenuKeyEvent(
    keyEvent: KeyEvent,
    onDismiss: () -> Unit,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
): Boolean =
    handleQuickMenuKeyEvent(
        type = keyEvent.type,
        key = keyEvent.key,
        onDismiss = onDismiss,
        onNavigateLeft = onNavigateLeft,
        onNavigateRight = onNavigateRight,
    )

/**
 * 播放器轻量快捷悬浮菜单（YouTube TV 体验基准）。
 *
 * 位于底部操作栏对应按钮正上方展开的垂直悬浮半透明卡片：
 * 1. 展开时焦点自动锁定在当前生效的配置项；
 * 2. 遥控器「上/下」键移动焦点，按「OK」立即生效并自动收起；
 * 3. 遥控器「向左/向右」键支持无缝平滑切换到相邻的快捷菜单；
 * 4. 遥控器「返回」键平滑收起并恢复底栏对应按钮的焦点。
 *
 * @param T 选项唯一标识类型
 * @param modifier 修饰符
 * @param visible 弹窗是否可见，用于驱动进出场平滑过渡动画
 * @param title 菜单顶栏标题（如 "清晰度"、"倍速"）
 * @param items 选项列表
 * @param onSelect 选项确认回调
 * @param onDismiss 菜单关闭回调
 * @param onNavigateLeft 遥控器按向左切换相邻菜单回调（为 null 表示无上一项）
 * @param onNavigateRight 遥控器按向右切换相邻菜单回调（为 null 表示无下一项）
 *
 * @see QuickMenuItem
 * @see VodQuickMenuType
 * @see LiveQuickMenuType
 */
@Composable
fun <T : Any> PlayerQuickMenuPopover(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    title: String,
    items: List<QuickMenuItem<T>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val selectedIndex =
        remember(items) {
            items.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemFocusRequesters =
        remember(items.size) {
            List(items.size) { FocusRequester() }
        }

    LaunchedEffect(visible, title, selectedIndex) {
        if (!visible) return@LaunchedEffect
        // 若目标项处于当前视口之外，主动先触发滚动以触发布局挂载，避免 visibleItemsInfo 挂起
        if (lazyListState.layoutInfo.visibleItemsInfo.none { it.index == selectedIndex }) {
            lazyListState.scrollToItem(selectedIndex)
        }
        // 等待 LazyColumn 针对目标项完成实际测量布局，确保其挂载至 Compose Focus 树后再请求焦点，杜绝静默失败
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .filter { it.any { info -> info.index == selectedIndex } }
            .first()
        if (selectedIndex in itemFocusRequesters.indices) {
            runCatching { itemFocusRequesters[selectedIndex].safeRequestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 250)) + slideInVertically(tween(durationMillis = 250)) { it / 4 },
        exit = fadeOut(tween(durationMillis = 250)) + slideOutVertically(tween(durationMillis = 250)) { it / 4 },
    ) {
        Surface(
            modifier =
                modifier
                    .widthIn(min = 200.dp, max = 240.dp)
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                    ),
            shape = RoundedCornerShape(14.dp),
            colors =
                SurfaceDefaults.colors(
                    containerColor = BVColors.PopoverBackground.copy(alpha = 0.94f),
                    contentColor = Color.White,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                        .onKeyEvent { keyEvent ->
                            handleQuickMenuKeyEvent(
                                keyEvent = keyEvent,
                                onDismiss = onDismiss,
                                onNavigateLeft = onNavigateLeft,
                                onNavigateRight = onNavigateRight,
                            )
                        },
            ) {
                // 顶部标题与左右切换提示
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = BVColors.PopoverTitle,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onNavigateLeft != null) {
                            Text(
                                text = "◀",
                                fontSize = 9.sp,
                                color = Color(0xFF777777),
                            )
                        }
                        if (onNavigateRight != null) {
                            Text(
                                text = "▶",
                                fontSize = 9.sp,
                                color = Color(0xFF777777),
                            )
                        }
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 2.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        val focusRequester = itemFocusRequesters.getOrNull(index) ?: remember { FocusRequester() }
                        QuickMenuItemRow(
                            item = item,
                            focusRequester = focusRequester,
                            isFirstItem = index == 0,
                            isLastItem = index == items.lastIndex,
                            onDismiss = onDismiss,
                            onClick = {
                                if (item.enabled) {
                                    onSelect(item.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 快捷选项单行组件。
 */
@Composable
private fun <T : Any> QuickMenuItemRow(
    item: QuickMenuItem<T>,
    focusRequester: FocusRequester,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionUp -> {
                            // 首项按「上」键消费事件，阻止焦点向背景进度条逃逸
                            isFirstItem
                        }
                        Key.DirectionDown -> {
                            if (isLastItem) {
                                // 尾项按「下」键顺滑收回弹窗，焦点安全回到底栏对应按钮
                                onDismiss()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }.touchClickable(onClick = onClick),
        onClick = onClick,
        enabled = item.enabled,
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (item.isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                contentColor = if (item.isSelected) Color.White else BVColors.PopoverItemUnselected,
                focusedContainerColor = Color.White,
                focusedContentColor = Color(0xFF0F0F0F),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                // 选中指示圆点
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                            ),
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!item.badge.isNullOrBlank()) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = item.badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
