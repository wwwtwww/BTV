package dev.frost819.newbv.app.ui.component.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.LocalIsDarkTheme

private val orderLabels =
    mapOf(
        SearchFilterOrderType.ComprehensiveSort to "综合排序",
        SearchFilterOrderType.MostClicks to "最多点击",
        SearchFilterOrderType.LatestPublish to "最新发布",
        SearchFilterOrderType.MostDanmaku to "最多弹幕",
        SearchFilterOrderType.MostFavorites to "最多收藏",
    )

private val durationLabels =
    mapOf(
        SearchFilterDuration.All to "全部时长",
        SearchFilterDuration.LessThan10Minutes to "10分钟以下",
        SearchFilterDuration.Between10And30Minutes to "10-30分钟",
        SearchFilterDuration.Between30And60Minutes to "30-60分钟",
        SearchFilterDuration.MoreThan60Minutes to "60分钟以上",
    )

/**
 * 获取排序筛选项的界面展示文案。
 */
fun SearchFilterOrderType.displayLabel(): String = orderLabels[this] ?: name

/**
 * 获取时长筛选项的界面展示文案。
 */
fun SearchFilterDuration.displayLabel(): String = durationLabels[this] ?: name

/**
 * 搜索结果筛选弹窗（仅视频 Web API 支持）。
 *
 * 采用 10-foot 大屏圆角卡片浮层与高对比聚焦反色胶囊组，
 * 打开时自动定位焦点至当前选中的排序项，支持一键重置默认条件。
 *
 * @param selectedOrder 当前生效的排序方式。
 * @param selectedDuration 当前生效的时长筛选。
 * @param onConfirm 确认应用筛选条件回调。
 * @param onDismiss 关闭弹窗回调。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchResultFilter(
    selectedOrder: SearchFilterOrderType,
    selectedDuration: SearchFilterDuration,
    onConfirm: (SearchFilterOrderType, SearchFilterDuration) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentOrder by remember { mutableStateOf(selectedOrder) }
    var currentDuration by remember { mutableStateOf(selectedDuration) }
    val initialFocusRequester = remember { FocusRequester() }
    val isDark = LocalIsDarkTheme.current
    val hasCustomFilter =
        currentOrder != SearchFilterOrderType.ComprehensiveSort ||
            currentDuration != SearchFilterDuration.All

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) {
            initialFocusRequester.safeRequestFocus()
        }

        Box(
            modifier =
                Modifier
                    .width(520.dp)
                    .clip(BVShape.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.12f else 0.08f),
                        shape = BVShape.large,
                    ).padding(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "搜索筛选",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (hasCustomFilter) {
                        DialogActionPill(
                            label = "重置默认",
                            isPrimary = false,
                            onClick = {
                                currentOrder = SearchFilterOrderType.ComprehensiveSort
                                currentDuration = SearchFilterDuration.All
                            },
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "排序方式",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        orderLabels.forEach { (order, label) ->
                            val focusMod =
                                if (order == selectedOrder) {
                                    Modifier.focusRequester(initialFocusRequester)
                                } else {
                                    Modifier
                                }
                            FilterOptionPill(
                                modifier = focusMod,
                                label = label,
                                selected = currentOrder == order,
                                onClick = { currentOrder = order },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "视频时长",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        durationLabels.forEach { (duration, label) ->
                            FilterOptionPill(
                                label = label,
                                selected = currentDuration == duration,
                                onClick = { currentDuration = duration },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogActionPill(
                        label = "取消",
                        isPrimary = false,
                        onClick = onDismiss,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DialogActionPill(
                        label = "确定",
                        isPrimary = true,
                        onClick = { onConfirm(currentOrder, currentDuration) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterOptionPill(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.3f else 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.5f else 0.78f)
        }
    val contentColor =
        if (selected) {
            if (isDark) MaterialTheme.colorScheme.onSurface else BVColors.PrimaryStrong
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val border =
        if (selected) {
            Border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                shape = BVShape.pill,
            )
        } else {
            Border.None
        }

    Surface(
        modifier =
            modifier
                .height(34.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = border,
                focusedBorder = Border.None,
            ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DialogActionPill(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        if (isPrimary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.8f)
        }
    val contentColor =
        if (isPrimary) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        modifier =
            Modifier
                .height(36.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
