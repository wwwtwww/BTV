package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.core.theme.BVShape
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * YouTube TV 风格横向内容泳道（Horizontal Shelf）。
 *
 * 包含行标题栏与横向滚动的视频卡片行。
 * 支持右滑预加载、即点即播与上下文菜单。
 *
 * @param modifier 修饰符。
 * @param shelfKey 泳道唯一标识（用于焦点记忆与列表 key）。
 * @param title 泳道标题（如"为你推荐"、"今日热门"）。
 * @param items 卡片列表数据。
 * @param onVideoClick 点击视频卡片（直接起播）。
 * @param onGoToDetailPage 详情页回调。
 * @param onGoToUpPage UP 主页回调。
 * @param onAddWatchLater 添加到稍后再看。
 * @param onSeeAllClick 查看全部回调（点击切换至对应单分类瀑布流）。
 * @param onLoadMore 滑动到末端时预加载回调。
 * @param hasMore 是否仍有更多数据可加载。
 * @param focusSaver 焦点记忆器。
 */
@Composable
fun HomeShelfRow(
    modifier: Modifier = Modifier,
    shelfKey: String,
    title: String,
    items: List<VideoCardData>,
    onVideoClick: (VideoCardData) -> Unit,
    onGoToDetailPage: (VideoCardData) -> Unit,
    onGoToUpPage: ((mid: Long, name: String) -> Unit)? = null,
    onAddWatchLater: (aid: Long) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    hasMore: Boolean = true,
    focusSaver: FocusSaver? = null,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 行头部：标题 + 查看全部
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (onSeeAllClick != null) {
                Surface(
                    onClick = onSeeAllClick,
                    shape = ClickableSurfaceDefaults.shape(BVShape.pill),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                    colors =
                        ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        text = "查看更多 >",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        // 水平滑动卡片列表
        // 推荐流刷新后顺序会变化：先按 avid 去重（防止重复 key 崩溃），
        // key 与焦点记忆均使用 avid 稳定标识（而非 index），
        // 否则刷新会导致整行卡片无法复用、焦点恢复指向“位置”而非“原视频”。
        val distinctItems = remember(items) { items.distinctBy { it.avid } }
        val listState = rememberLazyListState()

        // 行级滑动预加载监听：滑动到距离末尾 4 项时自动触发，记录已触发的列表长度避免重组/重入重复请求
        if (onLoadMore != null) {
            var lastTriggeredSize by remember { mutableIntStateOf(0) }
            LaunchedEffect(listState, distinctItems.size) {
                snapshotFlow {
                    val lastVisible =
                        listState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -1
                    lastVisible to distinctItems.size
                }.distinctUntilChanged()
                    .collect { (lastVisible, size) ->
                        if (size > 0 && lastVisible >= size - 4 && size != lastTriggeredSize) {
                            lastTriggeredSize = size
                            onLoadMore()
                        }
                    }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = distinctItems,
                key = { _, item -> "${shelfKey}_${item.avid}" },
            ) { _, item ->
                val itemModifier =
                    if (focusSaver != null) {
                        Modifier
                            .width(260.dp)
                            .focusSaverItem(focusSaver, "${shelfKey}_${item.avid}")
                    } else {
                        Modifier.width(260.dp)
                    }

                SmallVideoCard(
                    modifier = itemModifier,
                    data = item,
                    onClick = { onVideoClick(item) },
                    onGoToDetailPage = { onGoToDetailPage(item) },
                    onGoToUpPage =
                        item.upMid?.let { mid ->
                            { onGoToUpPage?.invoke(mid, item.upName) }
                        },
                    onAddWatchLater = { onAddWatchLater(item.avid) },
                )
            }

            if (!hasMore && distinctItems.isNotEmpty()) {
                item(key = "${shelfKey}_no_more") {
                    Box(
                        modifier =
                            Modifier
                                .width(120.dp)
                                .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "没有更多了",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
