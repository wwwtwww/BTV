package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.biliapi.repositories.LivePlayLine
import dev.frost819.newbv.biliapi.util.runSuspendCatching
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.focus.touchClickable
import kotlinx.coroutines.delay

/**
 * 直播播放器控制器信息层。
 *
 * 包含顶部标题+人气 meta 行+时钟和底部操作按钮两部分。
 * 通过 [AnimatedVisibility] 控制进出场动画。
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param title 直播间标题
 * @param areaName 分区名
 * @param onlineCount 人气值
 * @param clock 时钟（hour, minute）
 * @param isPlaying 是否正在播放
 * @param danmakuEnabled 弹幕是否开启
 * @param onPlayPause 播放/暂停回调
 * @param onRefresh 刷新回调
 * @param onDanmakuSwitchChange 弹幕开关回调
 * @param onShowSettings 打开设置回调
 * @param availableQualities 可用画质列表
 * @param currentQuality 当前生效的画质代码（默认 -1）
 * @param onQualityChange 画质切换回调
 * @param availableLines 可用播放线路列表
 * @param currentLine 当前生效的播放线路序号（1-indexed）
 * @param onLineChange 线路切换回调
 * @param onResetAutoHide 重置 OSD 自动隐藏计时器回调
 */
@Composable
fun LiveControllerInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    areaName: String,
    onlineCount: String,
    clock: Pair<Int, Int>,
    isPlaying: Boolean,
    danmakuEnabled: Boolean,
    onPlayPause: () -> Unit,
    onRefresh: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    availableQualities: List<Pair<Int, String>> = emptyList(),
    currentQuality: Int = -1,
    onQualityChange: (Int) -> Unit = {},
    availableLines: List<LivePlayLine> = emptyList(),
    currentLine: Int = 1,
    onLineChange: (Int) -> Unit = {},
    onResetAutoHide: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "LiveControllerTop",
        ) {
            LiveControllerInfoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = title,
                areaName = areaName,
                onlineCount = onlineCount,
                clock = clock,
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "LiveControllerBottom",
        ) {
            LiveControllerInfoBottom(
                modifier = Modifier.align(Alignment.BottomCenter),
                show = show,
                isPlaying = isPlaying,
                danmakuEnabled = danmakuEnabled,
                onPlayPause = onPlayPause,
                onRefresh = onRefresh,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onShowSettings = onShowSettings,
                availableQualities = availableQualities,
                currentQuality = currentQuality,
                onQualityChange = onQualityChange,
                availableLines = availableLines,
                currentLine = currentLine,
                onLineChange = onLineChange,
                onResetAutoHide = onResetAutoHide,
            )
        }
    }
}

/**
 * 直播控制器顶部信息（标题 + 人气 meta 行 + 时钟）。
 *
 * 人气显示与点播 [ControllerVideoInfoTop] 的同时观看人数保持一致：
 * 标题正下方 meta 行，双人形图标 + 小字文案。
 */
@Composable
private fun LiveControllerInfoTop(
    modifier: Modifier = Modifier,
    title: String,
    areaName: String,
    onlineCount: String,
    clock: Pair<Int, Int>,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp),
                    ),
                ).background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0f),
                                ),
                        ),
                ).padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                text = title,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        shadow = Shadow(color = Color.Black, blurRadius = 1f),
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (areaName.isNotBlank()) {
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
            Clock(hour = clock.first, minute = clock.second)
        }
        // 人气 meta 行：与点播的同时观看人数样式一致（标题下方小字）
        if (onlineCount.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_player_watching),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "人气 $onlineCount",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            shadow = Shadow(color = Color.Black, blurRadius = 1f),
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/**
 * 直播控制器底部信息（操作按钮行）。
 */
@Composable
private fun LiveControllerInfoBottom(
    modifier: Modifier = Modifier,
    show: Boolean = true,
    isPlaying: Boolean,
    danmakuEnabled: Boolean,
    onPlayPause: () -> Unit,
    onRefresh: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    availableQualities: List<Pair<Int, String>> = emptyList(),
    currentQuality: Int = -1,
    onQualityChange: (Int) -> Unit = {},
    availableLines: List<LivePlayLine> = emptyList(),
    currentLine: Int = 1,
    onLineChange: (Int) -> Unit = {},
    onResetAutoHide: () -> Unit = {},
) {
    val playPauseFocusRequester = remember { FocusRequester() }
    val qualityFocusRequester = remember { FocusRequester() }
    val lineFocusRequester = remember { FocusRequester() }

    var activeQuickMenu by remember { mutableStateOf<LiveQuickMenuType?>(null) }
    var popoverContainerRootX by remember { mutableFloatStateOf(0f) }
    val quickMenuAnchorRootX = remember { mutableStateMapOf<LiveQuickMenuType, Float>() }
    var focusedDescription by remember { mutableStateOf("") }

    val defaultQualityLabel = stringResource(R.string.player_quality_default)
    val resolvedQuality =
        remember(availableQualities, currentQuality) {
            resolveLiveQuality(availableQualities, currentQuality)
        }
    val effectiveQuality = resolvedQuality?.first ?: currentQuality
    val currentQualityLabel = resolvedQuality?.second ?: defaultQualityLabel

    LaunchedEffect(Unit) {
        // TV 遥控器输入焦点时序：延迟 50ms（约 2-3 帧垂直同步），
        // 等待直播控制器完全展开并挂载至 Compose Focus 树节点，避免直接请求焦点静默失败。
        delay(50)
        // 初始呼出直接聚焦播放控制按钮行
        runSuspendCatching { playPauseFocusRequester.safeRequestFocus() }
    }

    Column(
        modifier =
            modifier.clip(
                MaterialTheme.shapes.large.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp),
                ),
            ),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // 快捷微卡片浮层（位于进度条与操作按钮行之间，水平对齐触发按钮实际坐标）
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .onGloballyPositioned { coordinates ->
                        // 记录微卡片水平对齐基准容器的全局 X 坐标。
                        // 在 OSD 底栏初次展示完成时即刻捕获稳态值，微卡片在后续用户触发挂载时首帧直接读取该值，保证 0 帧延迟精准对齐。
                        popoverContainerRootX = coordinates.positionInRoot().x
                    },
        ) {
            var lastMenu by remember { mutableStateOf<LiveQuickMenuType?>(null) }
            if (activeQuickMenu != null) {
                lastMenu = activeQuickMenu
            }
            val currentMenu = activeQuickMenu ?: lastMenu
            val isMenuVisible = activeQuickMenu != null

            androidx.compose.animation.AnimatedVisibility(
                visible = isMenuVisible && currentMenu != null,
                enter = EnterTransition.None,
                exit = fadeOut(tween(durationMillis = 250)),
            ) {
                if (currentMenu != null) {
                    AnimatedContent(
                        targetState = currentMenu,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 150)) togetherWith
                                fadeOut(tween(durationMillis = 150))
                        },
                        label = "LiveQuickMenuTransition",
                    ) { menu ->
                        Box(
                            modifier =
                                Modifier
                                    .padding(bottom = 6.dp)
                                    .alignToQuickMenuAnchor(
                                        anchorRootXProvider = { quickMenuAnchorRootX[menu] },
                                        containerRootXProvider = { popoverContainerRootX },
                                    ),
                        ) {
                            val navigateLeftAction: (() -> Unit)? =
                                menu.previous()?.let { prev ->
                                    {
                                        activeQuickMenu = prev
                                        onResetAutoHide()
                                    }
                                }
                            val navigateRightAction: (() -> Unit)? =
                                menu.next(hasLines = availableLines.isNotEmpty())?.let { next ->
                                    {
                                        activeQuickMenu = next
                                        onResetAutoHide()
                                    }
                                }
                            when (menu) {
                                LiveQuickMenuType.Quality -> {
                                    val items =
                                        availableQualities.map {
                                            QuickMenuItem(
                                                id = it.first,
                                                label = it.second,
                                                isSelected = it.first == effectiveQuality,
                                            )
                                        }
                                    PlayerQuickMenuPopover(
                                        visible = isMenuVisible,
                                        title = "画质清晰度",
                                        items = items,
                                        onSelect = {
                                            onQualityChange(it)
                                            activeQuickMenu = null
                                            qualityFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onDismiss = {
                                            activeQuickMenu = null
                                            qualityFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onNavigateLeft = navigateLeftAction,
                                        onNavigateRight = navigateRightAction,
                                    )
                                }

                                LiveQuickMenuType.Line -> {
                                    val items =
                                        availableLines.indices.map { idx ->
                                            QuickMenuItem(
                                                id = idx + 1,
                                                label = "线路 ${idx + 1}",
                                                isSelected = (idx + 1) == currentLine,
                                            )
                                        }
                                    PlayerQuickMenuPopover(
                                        visible = isMenuVisible,
                                        title = "播放线路",
                                        items = items,
                                        onSelect = {
                                            onLineChange(it)
                                            activeQuickMenu = null
                                            lineFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onDismiss = {
                                            activeQuickMenu = null
                                            lineFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onNavigateLeft = navigateLeftAction,
                                        onNavigateRight = navigateRightAction,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 按钮行定义
        val buttons =
            buildList {
                add(
                    LiveControllerButton(
                        id = "play_pause",
                        icon = R.drawable.play_pause_24px,
                        description = if (isPlaying) "暂停" else "播放",
                        focusRequester = playPauseFocusRequester,
                        action = onPlayPause,
                    ),
                )
                if (availableQualities.isNotEmpty()) {
                    add(
                        LiveControllerButton(
                            id = "quality",
                            badge = currentQualityLabel,
                            description = "清晰度：$currentQualityLabel (按 OK 切换)",
                            focusRequester = qualityFocusRequester,
                            quickMenuType = LiveQuickMenuType.Quality,
                            action = {
                                activeQuickMenu =
                                    if (activeQuickMenu ==
                                        LiveQuickMenuType.Quality
                                    ) {
                                        null
                                    } else {
                                        LiveQuickMenuType.Quality
                                    }
                                onResetAutoHide()
                            },
                        ),
                    )
                }
                if (availableLines.isNotEmpty()) {
                    add(
                        LiveControllerButton(
                            id = "line",
                            badge = "线路$currentLine",
                            description = "播放线路：线路$currentLine (按 OK 切换)",
                            focusRequester = lineFocusRequester,
                            quickMenuType = LiveQuickMenuType.Line,
                            action = {
                                activeQuickMenu =
                                    if (activeQuickMenu == LiveQuickMenuType.Line) null else LiveQuickMenuType.Line
                                onResetAutoHide()
                            },
                        ),
                    )
                }
                add(
                    LiveControllerButton(
                        id = "danmaku",
                        icon = if (danmakuEnabled) R.drawable.danmaku_on_24px else R.drawable.danmaku_off_24px,
                        description = if (danmakuEnabled) "弹幕：开启" else "弹幕：关闭",
                        action = onDanmakuSwitchChange,
                    ),
                )
                add(
                    LiveControllerButton(
                        id = "refresh",
                        icon = R.drawable.baseline_refresh_24,
                        description = "刷新直播流",
                        action = onRefresh,
                    ),
                )
                add(
                    LiveControllerButton(
                        id = "settings",
                        icon = R.drawable.settings_24px,
                        description = "更多设置",
                        action = {
                            activeQuickMenu = null
                            onShowSettings()
                        },
                    ),
                )
            }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onKeyEvent {
                        if (it.key == Key.DirectionUp) {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            return@onKeyEvent false
                        }
                        false
                    }.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons.forEach { item ->
                key(item.id) {
                    val anchorMod =
                        if (item.quickMenuType != null) {
                            Modifier.onGloballyPositioned { coordinates ->
                                quickMenuAnchorRootX[item.quickMenuType] = coordinates.positionInRoot().x
                            }
                        } else {
                            Modifier
                        }
                    val focusMod =
                        if (item.focusRequester != null) {
                            Modifier.focusRequester(item.focusRequester)
                        } else {
                            Modifier
                        }
                    Surface(
                        modifier =
                            anchorMod
                                .height(46.dp)
                                .defaultMinSize(minWidth = 46.dp)
                                .then(focusMod)
                                .onFocusChanged {
                                    if (it.hasFocus) focusedDescription = item.description
                                }.touchClickable(
                                    onClick = item.action,
                                ),
                        onClick = item.action,
                        shape =
                            ClickableSurfaceDefaults.shape(
                                shape = if (item.badge != null) RoundedCornerShape(23.dp) else CircleShape,
                            ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
                        colors =
                            ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF0F0F0F),
                            ),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = if (item.badge != null) 12.dp else 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.badge != null) {
                                Text(
                                    text = item.badge,
                                    style =
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp,
                                        ),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            } else if (item.icon != null) {
                                Icon(
                                    painter = painterResource(id = item.icon),
                                    contentDescription = item.description,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (focusedDescription.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 28.dp, bottom = 14.dp),
                text = focusedDescription,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 3f),
                    ),
                color = Color(0xFFE0E0E0),
            )
        }
    }
}

/**
 * 直播操作按钮模型。
 */
private data class LiveControllerButton(
    val id: String,
    val icon: Int? = null,
    val badge: String? = null,
    val description: String,
    val focusRequester: FocusRequester? = null,
    val quickMenuType: LiveQuickMenuType? = null,
    val action: () -> Unit,
)

/**
 * 时钟组件。
 */
@Composable
private fun Clock(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
) {
    Text(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        style =
            TextStyle(
                shadow = Shadow(color = Color.Black, blurRadius = 1f),
                fontFeatureSettings = "tnum",
                letterSpacing = 0.5.sp,
            ),
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 32.sp)) {
                    append("$hour".padStart(2, '0'))
                    append(":")
                    append("$minute".padStart(2, '0'))
                }
            },
    )
}

/**
 * 解析直播当前生效的画质代码与展示文案。
 *
 * 若 [currentQuality] 存在于 [availableQualities] 中，则优先匹配该项；
 * 若未命中但 [availableQualities] 非空，则自动回退到首项作为当前选中项（避免未初始化或默认值 -1 导致丢失选中指示）；
 * 若列表为空，则返回 null，由调用侧 @Composable 上下文提供本地化兜底展示文案。
 *
 * @param availableQualities 可用画质列表 (qn to label)
 * @param currentQuality 传入的目标画质代码（默认 -1）
 * @return 生效的画质代码与展示标签，列表为空时返回 null
 */
internal fun resolveLiveQuality(
    availableQualities: List<Pair<Int, String>>,
    currentQuality: Int,
): Pair<Int, String>? {
    val matched = availableQualities.find { it.first == currentQuality }
    if (matched != null) return matched
    return availableQualities.firstOrNull()
}
