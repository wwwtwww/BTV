package dev.frost819.newbv.app.ui.component.player
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 播放器底部横向胶囊选集轨（Horizontal Capsule Track）。
 *
 * 采用底部轻量悬浮微轨取代传统左侧 300dp 抽屉，占屏高度约 20%~25%，
 * 背景使用半透明黑夜渐变遮罩，不遮挡视频核心主体画面：
 * - 单视频多 P、番剧多集、UGC 合集统一使用 [BVShape.pill] 胶囊样式；
 * - 呼出时通过 [LazyListState.animateScrollToItem] 将当前播放集平滑居中定位并自动落焦；
 * - 超长集数（默认 > [EPISODE_GROUP_SIZE]）自动渲染顶部区间胶囊分页（1-50、51-100 等）；
 * - 多集 UGC 合集内单集含多 P 时，按需在下方展开紧凑的二级分 P 胶囊轨。
 *
 * @param modifier 修饰符。
 * @param show 是否显示选集轨。
 * @param currentCid 当前正在播放的 CID。
 * @param videoList 播放列表数据。
 * @param currentAid 当前正在播放的 AID（可选，用于合集消歧）。
 * @param groupSize 超长列表分组阈值，默认 [EPISODE_GROUP_SIZE]。
 * @param onPlayNewVideo 点击切换新剧集/分 P 的回调。
 * @param onClose 关闭选集轨回到纯净播放的回调。
 */
@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    currentCid: Long,
    videoList: List<VideoListItem>,
    currentAid: Long = 0L,
    groupSize: Int = EPISODE_GROUP_SIZE,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onClose: () -> Unit,
) {
    val trackState =
        remember(videoList, currentCid, currentAid, groupSize) {
            buildEpisodeTrackState(
                videoList = videoList,
                currentCid = currentCid,
                currentAid = currentAid,
                groupSize = groupSize,
            )
        }

    val initialExpandedKey =
        trackState.items
            .getOrNull(trackState.activeItemIndex)
            ?.takeIf { it.subPages.isNotEmpty() }
            ?.key

    var selectedGroupIndex by remember(trackState) { mutableIntStateOf(trackState.activeGroupIndex) }
    var expandedParentKey by remember(trackState) { mutableStateOf(initialExpandedKey) }

    val currentGroupItems =
        remember(trackState, selectedGroupIndex) {
            trackState.itemsInGroup(selectedGroupIndex)
        }
    val defaultLocalIndex =
        remember(trackState, selectedGroupIndex) {
            trackState.localIndexInGroup(trackState.activeItemIndex, selectedGroupIndex).coerceAtLeast(0)
        }
    var focusedPrimaryIndex by remember(selectedGroupIndex, currentGroupItems.size) {
        mutableIntStateOf(defaultLocalIndex.coerceAtMost((currentGroupItems.size - 1).coerceAtLeast(0)))
    }

    val expandedParentItem =
        remember(currentGroupItems, expandedParentKey) {
            currentGroupItems.firstOrNull { it.key == expandedParentKey && it.subPages.isNotEmpty() }
        }
    val expandedSubPages = expandedParentItem?.subPages.orEmpty()
    val defaultSubIndex =
        remember(expandedSubPages) {
            expandedSubPages.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
        }
    var focusedSubIndex by remember(expandedParentItem?.key, expandedSubPages.size) {
        mutableIntStateOf(defaultSubIndex.coerceAtMost((expandedSubPages.size - 1).coerceAtLeast(0)))
    }

    val groupListState = rememberLazyListState()
    val primaryListState = rememberLazyListState()
    val subPageListState = rememberLazyListState()

    val groupRowFocusRequester = remember { FocusRequester() }
    val primaryRowFocusRequester = remember { FocusRequester() }
    val subPageRowFocusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx =
        remember(density, configuration) {
            with(density) { configuration.screenWidthDp.dp.roundToPx() }
        }
    val defaultCapsuleWidthPx = remember(density) { with(density) { 132.dp.roundToPx() } }
    val defaultGroupWidthPx = remember(density) { with(density) { 88.dp.roundToPx() } }

    val backgroundBrush =
        remember {
            Brush.verticalGradient(
                colorStops =
                    arrayOf(
                        0.0f to Color.Transparent,
                        0.30f to Color.Black.copy(alpha = 0.76f),
                        1.0f to Color.Black.copy(alpha = 0.94f),
                    ),
            )
        }

    // 选集轨打开或底层当前播放集变化时，重置分组到当前播放集所在组，平滑居中并请求焦点
    LaunchedEffect(show, trackState) {
        if (!show || trackState.items.isEmpty()) return@LaunchedEffect

        selectedGroupIndex = trackState.activeGroupIndex
        expandedParentKey = initialExpandedKey

        // 等待 AnimatedVisibility 首帧完成测量，确保 LazyRow 具备真实视口宽度
        withFrameNanos { }

        val targetLocalIndex =
            trackState
                .localIndexInGroup(trackState.activeItemIndex, trackState.activeGroupIndex)
                .coerceAtLeast(0)
        focusedPrimaryIndex = targetLocalIndex

        if (trackState.groups.isNotEmpty()) {
            groupListState.scrollToCentered(
                targetIndex = trackState.activeGroupIndex,
                fallbackViewportPx = screenWidthPx,
                fallbackItemPx = defaultGroupWidthPx,
            )
        }

        primaryListState.scrollToCentered(
            targetIndex = targetLocalIndex,
            fallbackViewportPx = screenWidthPx,
            fallbackItemPx = defaultCapsuleWidthPx,
        )

        val activeParent = trackState.items.getOrNull(trackState.activeItemIndex)
        val activeSubIdx = activeParent?.subPages?.indexOfFirst { it.isCurrent } ?: -1
        if (activeSubIdx >= 0) {
            focusedSubIndex = activeSubIdx
            subPageListState.scrollToCentered(
                targetIndex = activeSubIdx,
                fallbackViewportPx = screenWidthPx,
                fallbackItemPx = defaultCapsuleWidthPx,
            )
            requestFocusSafely(subPageRowFocusRequester)
        } else {
            requestFocusSafely(primaryRowFocusRequester)
        }
    }

    // 在面板展开状态下切换 1-50 / 51-100 分组时，同步滚动主胶囊轨预览对应分组内容
    LaunchedEffect(selectedGroupIndex) {
        if (!show || currentGroupItems.isEmpty()) return@LaunchedEffect
        val activeInThisGroup = trackState.localIndexInGroup(trackState.activeItemIndex, selectedGroupIndex)
        if (activeInThisGroup >= 0) {
            focusedPrimaryIndex = activeInThisGroup
            primaryListState.scrollToCentered(
                targetIndex = activeInThisGroup,
                fallbackViewportPx = screenWidthPx,
                fallbackItemPx = defaultCapsuleWidthPx,
            )
        } else {
            focusedPrimaryIndex = 0
            primaryListState.scrollToItem(0)
        }
    }

    fun moveFocusToGroupRow() {
        scope.launch {
            groupListState.scrollToCentered(
                targetIndex = selectedGroupIndex,
                fallbackViewportPx = screenWidthPx,
                fallbackItemPx = defaultGroupWidthPx,
            )
            requestFocusSafely(groupRowFocusRequester)
        }
    }

    fun moveFocusToPrimaryRow() {
        scope.launch {
            val safeTarget = focusedPrimaryIndex.coerceIn(0, (currentGroupItems.size - 1).coerceAtLeast(0))
            val isVisible = primaryListState.layoutInfo.visibleItemsInfo.any { it.index == safeTarget }
            if (!isVisible) {
                primaryListState.scrollToCentered(
                    targetIndex = safeTarget,
                    fallbackViewportPx = screenWidthPx,
                    fallbackItemPx = defaultCapsuleWidthPx,
                )
            }
            requestFocusSafely(primaryRowFocusRequester)
        }
    }

    fun moveFocusToSubPageRow() {
        if (expandedSubPages.isEmpty()) return
        scope.launch {
            val safeTarget = focusedSubIndex.coerceIn(0, expandedSubPages.lastIndex)
            val isVisible = subPageListState.layoutInfo.visibleItemsInfo.any { it.index == safeTarget }
            if (!isVisible) {
                subPageListState.scrollToCentered(
                    targetIndex = safeTarget,
                    fallbackViewportPx = screenWidthPx,
                    fallbackItemPx = defaultCapsuleWidthPx,
                )
            }
            requestFocusSafely(subPageRowFocusRequester)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = show && trackState.items.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            label = "EpisodeCapsuleTrackVisibility",
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(backgroundBrush)
                        .padding(top = 20.dp, bottom = 14.dp)
                        .testTag("episode_capsule_track"),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            ) {
                // 轨头：选集进度 + 遥控器退出提示
                EpisodeTrackHeader(
                    title = trackState.headerTitle,
                    activeNumber = if (trackState.activeItemIndex >= 0) trackState.activeItemIndex + 1 else null,
                    totalCount = trackState.items.size,
                )

                // 超长集数区间分组胶囊轨（仅 > groupSize 时渲染）
                if (trackState.groups.isNotEmpty()) {
                    LazyRow(
                        state = groupListState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusGroup()
                                .testTag("episode_group_row")
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            if (event.nativeKeyEvent.repeatCount == 0) onClose()
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            moveFocusToPrimaryRow()
                                            true
                                        }
                                        else -> false
                                    }
                                },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 2.dp),
                    ) {
                        itemsIndexed(
                            items = trackState.groups,
                            key = { _, group -> "group_${group.groupIndex}" },
                        ) { index, group ->
                            GroupRangeCapsule(
                                modifier =
                                    if (index == selectedGroupIndex) {
                                        Modifier.focusRequester(groupRowFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                label = group.label,
                                isSelected = index == selectedGroupIndex,
                                containsPlayingItem = index == trackState.activeGroupIndex,
                                onFocus = { selectedGroupIndex = index },
                                onClick = {
                                    selectedGroupIndex = index
                                    moveFocusToPrimaryRow()
                                },
                            )
                        }
                    }
                }

                // 主选集胶囊轨
                LazyRow(
                    state = primaryListState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusGroup()
                            .testTag("episode_primary_row")
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        if (trackState.groups.isNotEmpty()) {
                                            moveFocusToGroupRow()
                                        } else if (event.nativeKeyEvent.repeatCount == 0) {
                                            onClose()
                                        }
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        if (expandedSubPages.isNotEmpty()) {
                                            moveFocusToSubPageRow()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 4.dp),
                ) {
                    val safeFocusTarget =
                        focusedPrimaryIndex.coerceIn(0, (currentGroupItems.size - 1).coerceAtLeast(0))
                    itemsIndexed(
                        items = currentGroupItems,
                        key = { _, item -> item.key },
                    ) { index, item ->
                        EpisodeCapsule(
                            modifier =
                                if (index == safeFocusTarget) {
                                    Modifier.focusRequester(primaryRowFocusRequester)
                                } else {
                                    Modifier
                                },
                            item = item,
                            onFocus = {
                                focusedPrimaryIndex = index
                                expandedParentKey = if (item.subPages.isNotEmpty()) item.key else null
                            },
                            onClick = {
                                if (item.subPages.isNotEmpty()) {
                                    if (expandedParentKey != item.key) {
                                        // 触屏首次点击未展开的多 P 合集项时先展开子轨
                                        expandedParentKey = item.key
                                    } else if (!item.isCurrent) {
                                        onPlayNewVideo(item.subPages.first().targetItem)
                                    } else {
                                        moveFocusToSubPageRow()
                                    }
                                } else if (!item.isCurrent) {
                                    onPlayNewVideo(item.targetItem)
                                } else {
                                    onClose()
                                }
                            },
                        )
                    }
                }

                // 二级子分 P 胶囊轨（仅当合集内单集含多个 ugcPages 时按需平滑展开）
                AnimatedVisibility(
                    visible = expandedSubPages.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    label = "SubPageCapsuleTrackVisibility",
                ) {
                    LazyRow(
                        state = subPageListState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusGroup()
                                .testTag("episode_subpage_row")
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            moveFocusToPrimaryRow()
                                            true
                                        }
                                        Key.DirectionDown -> true
                                        else -> false
                                    }
                                },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 2.dp),
                    ) {
                        val safeSubFocus =
                            focusedSubIndex.coerceIn(0, (expandedSubPages.size - 1).coerceAtLeast(0))
                        itemsIndexed(
                            items = expandedSubPages,
                            key = { _, sub -> sub.key },
                        ) { index, sub ->
                            SubPageCapsule(
                                modifier =
                                    if (index == safeSubFocus) {
                                        Modifier.focusRequester(subPageRowFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                item = sub,
                                onFocus = { focusedSubIndex = index },
                                onClick = {
                                    if (!sub.isCurrent) {
                                        onPlayNewVideo(sub.targetItem)
                                    } else {
                                        onClose()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeTrackHeader(
    title: String,
    activeNumber: Int?,
    totalCount: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            val progressLabel =
                if (activeNumber != null) {
                    "$activeNumber / $totalCount"
                } else {
                    "共 $totalCount 项"
                }
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.68f),
            )
        }
        Text(
            text = "按「向上」或「返回」返回播放",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.56f),
        )
    }
}

@Composable
private fun GroupRangeCapsule(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    containsPlayingItem: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val containerColor =
        when {
            isSelected -> Color.White.copy(alpha = 0.24f)
            containsPlayingItem -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = 0.08f)
        }
    val contentColor =
        if (isSelected || containsPlayingItem) {
            Color.White
        } else {
            Color.White.copy(alpha = 0.65f)
        }

    Surface(
        modifier =
            modifier
                .height(28.dp)
                .semantics { selected = isSelected }
                .onFocusChanged { if (it.hasFocus) onFocus() }
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.pill),
        scale = TvFocusDefaults.surfaceScale(focusedScale = 1.04f),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    if (isSelected) {
                        Border(
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            shape = BVShape.pill,
                        )
                    } else {
                        Border.None
                    },
                focusedBorder =
                    Border(
                        border = BorderStroke(1.5.dp, Color.White),
                        shape = BVShape.pill,
                    ),
            ),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EpisodeCapsule(
    modifier: Modifier = Modifier,
    item: EpisodeCapsuleItem,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val isCurrent = item.isCurrent
    val containerColor =
        if (isCurrent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
        } else {
            Color.White.copy(alpha = 0.12f)
        }
    val contentColor =
        if (isCurrent) {
            Color.White
        } else {
            Color.White.copy(alpha = 0.90f)
        }

    Surface(
        modifier =
            modifier
                .height(40.dp)
                .widthIn(min = 84.dp, max = 240.dp)
                .semantics { selected = isCurrent }
                .onFocusChanged { if (it.hasFocus) onFocus() }
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.pill),
        scale = TvFocusDefaults.surfaceScale(focusedScale = 1.05f),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    if (isCurrent) {
                        Border(
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)),
                            shape = BVShape.pill,
                        )
                    } else {
                        Border(
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            shape = BVShape.pill,
                        )
                    },
                focusedBorder =
                    Border(
                        border = BorderStroke(2.dp, Color.White),
                        shape = BVShape.pill,
                    ),
            ),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "正在播放",
                    modifier = Modifier.size(16.dp),
                )
            }
            if (item.badge.isNotEmpty()) {
                Text(
                    text = item.badge,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current.copy(alpha = if (isCurrent) 1f else 0.72f),
                    maxLines = 1,
                )
            }
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subPages.size > 1) {
                Text(
                    text = "${item.subPages.size}P",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalContentColor.current.copy(alpha = 0.68f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SubPageCapsule(
    modifier: Modifier = Modifier,
    item: SubPageCapsuleItem,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val isCurrent = item.isCurrent
    val containerColor =
        if (isCurrent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        } else {
            Color.White.copy(alpha = 0.09f)
        }
    val contentColor =
        if (isCurrent) {
            Color.White
        } else {
            Color.White.copy(alpha = 0.82f)
        }

    Surface(
        modifier =
            modifier
                .height(32.dp)
                .widthIn(min = 72.dp, max = 210.dp)
                .semantics { selected = isCurrent }
                .onFocusChanged { if (it.hasFocus) onFocus() }
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.pill),
        scale = TvFocusDefaults.surfaceScale(focusedScale = 1.04f),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    if (isCurrent) {
                        Border(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                            shape = BVShape.pill,
                        )
                    } else {
                        Border.None
                    },
                focusedBorder =
                    Border(
                        border = BorderStroke(1.5.dp, Color.White),
                        shape = BVShape.pill,
                    ),
            ),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "正在播放",
                    modifier = Modifier.size(14.dp),
                )
            }
            if (item.badge.isNotEmpty()) {
                Text(
                    text = item.badge,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current.copy(alpha = if (isCurrent) 1f else 0.72f),
                    maxLines = 1,
                )
            }
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 将目标下标在 [LazyRow] 视口内平滑横向居中对齐。
 *
 * 距离较远时先无动画贴近至目标前 6 项，再执行最后一段平滑居中滑动，
 * 兼顾 50 集长跨度跳转的响应速度与阻尼滑入质感。
 */
private suspend fun LazyListState.scrollToCentered(
    targetIndex: Int,
    fallbackViewportPx: Int,
    fallbackItemPx: Int,
) {
    val totalCount = layoutInfo.totalItemsCount
    if (targetIndex < 0 || (totalCount > 0 && targetIndex >= totalCount)) return

    if (abs(firstVisibleItemIndex - targetIndex) > 12) {
        scrollToItem((targetIndex - 6).coerceAtLeast(0))
    }

    val viewportPx = layoutInfo.viewportSize.width.takeIf { it > 0 } ?: fallbackViewportPx
    val itemPx = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }?.size ?: fallbackItemPx
    val centerScrollOffset = -((viewportPx - itemPx) / 2).coerceAtLeast(0)
    runCatching {
        animateScrollToItem(index = targetIndex, scrollOffset = centerScrollOffset)
    }
}

private suspend fun requestFocusSafely(focusRequester: FocusRequester) {
    if (!focusRequester.safeRequestFocus()) {
        delay(60)
        focusRequester.safeRequestFocus()
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF101216, widthDp = 960, heightDp = 540)
@Composable
private fun VideoListControllerPreview() {
    val sampleList =
        listOf(
            VideoListItem(
                aid = 1,
                cid = 101,
                title = "序章：世界观设定",
                ugcPages =
                    listOf(
                        VideoPage(
                            cid = 201,
                            index = 1,
                            title = "上半篇",
                            duration = 600,
                            dimension = Dimension(1920, 1080),
                        ),
                        VideoPage(
                            cid = 202,
                            index = 2,
                            title = "下半篇",
                            duration = 600,
                            dimension = Dimension(1920, 1080),
                        ),
                    ),
            ),
            VideoListItem(aid = 2, cid = 102, title = "正篇：启程"),
            VideoListItem(aid = 3, cid = 103, title = "终章：重逢"),
        )

    BVTheme(themeMode = ThemeMode.Dark) {
        VideoListController(
            show = true,
            currentCid = 201L,
            videoList = sampleList,
            onPlayNewVideo = {},
            onClose = {},
        )
    }
}

// endregion
