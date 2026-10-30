package dev.frost819.newbv.app.ui.component.videocard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.R
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.focus.isMenuKey
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTypography

/**
 * 小型视频卡片（对标 YouTube TV 风格）。
 *
 * 展示视频封面、播放数/弹幕数/时长、标题、UP 主名、发布时间。
 * 支持聚焦微放大动效（Scale 1.06x）以及通过遥控器「Menu / 菜单键」或长按唤起快捷操作浮层。
 *
 * @param data 卡片数据。
 * @param onClick 点击卡片回调。
 * @param onAddWatchLater 稍后再看回调（null 时不显示按钮）。
 * @param onGoToDetailPage 详情页回调（null 时不显示按钮）。
 * @param onGoToUpPage UP 主页回调（null 时不显示按钮）。
 * @param onRemoveWatchLater 移除稍后再看回调（null 时不显示按钮）。
 * @param onDeleteHistory 删除历史记录回调（null 时不显示按钮）。
 * @param onRemoveFavorite 取消收藏回调（null 时不显示按钮）。
 */
@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    onClick: () -> Unit,
    onAddWatchLater: (() -> Unit)? = null,
    onGoToDetailPage: (() -> Unit)? = null,
    onGoToUpPage: (() -> Unit)? = null,
    onRemoveWatchLater: (() -> Unit)? = null,
    onDeleteHistory: (() -> Unit)? = null,
    onRemoveFavorite: (() -> Unit)? = null,
) {
    var showActions by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val hasAnyAction =
        onAddWatchLater != null ||
            onGoToDetailPage != null ||
            onGoToUpPage != null ||
            onRemoveWatchLater != null ||
            onDeleteHistory != null ||
            onRemoveFavorite != null

    // Back 键由 VideoCardActionDialog（独立 Dialog 窗口）的 onDismissRequest 处理，
    // 此处无需也不应再注册 BackHandler。

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = { if (!showActions) onClick() },
            onLongClick = {
                if (hasAnyAction) showActions = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .onFocusChanged { isFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.isMenuKey() && event.type == KeyEventType.KeyUp) {
                            if (hasAnyAction) {
                                showActions = true
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }.touchClickable(
                        onClick = { if (!showActions) onClick() },
                        onLongClick = { if (hasAnyAction) showActions = true },
                    ),
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            scale = TvFocusDefaults.cardScale(),
            border = TvFocusDefaults.cardBorder(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CardCover(
                    cover = data.cover,
                    play = data.playString,
                    danmaku = data.danmakuString,
                    time = data.timeString,
                    progress = data.progress,
                )
                if (isFocused && hasAnyAction) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(BVShape.pill)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "菜单 更多",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (showActions && hasAnyAction) {
            VideoCardActionDialog(
                title = data.title,
                upName = data.upName,
                cover = data.cover,
                timeString = data.timeString,
                onDismiss = { showActions = false },
                onRemoveWatchLater = onRemoveWatchLater,
                onDeleteHistory = onDeleteHistory,
                onRemoveFavorite = onRemoveFavorite,
                onAddWatchLater = onAddWatchLater,
                onGoToDetailPage = onGoToDetailPage,
                onGoToUpPage = onGoToUpPage,
            )
        }

        CardInfo(
            modifier = Modifier.fillMaxWidth(),
            title = data.title,
            upName = data.upName,
            pubTime = data.pubTime,
        )
    }
}

/**
 * 卡片封面区域。
 *
 * 封面图片 + 底部渐变遮罩 + 播放数/弹幕数/时长统计。
 */
@Composable
private fun CardCover(
    modifier: Modifier = Modifier,
    cover: String,
    play: String,
    danmaku: String,
    time: String,
    progress: Float? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AsyncImage(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large),
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.85f),
                                ),
                        ),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (play.isNotBlank()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_count),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = play,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (danmaku.isNotBlank()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_danmaku_count),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = danmaku,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            if (time.isNotBlank()) {
                Box(
                    modifier =
                        Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.82f),
                                shape = BVShape.small,
                            ).padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = time,
                        style = BVTypography.tabular(MaterialTheme.typography.bodySmall),
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }

        if (progress != null && progress > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

/**
 * 卡片信息区域。
 *
 * 标题（2 行省略）+ UP 主名 + 发布时间。
 */
@Composable
private fun CardInfo(
    modifier: Modifier = Modifier,
    title: String,
    upName: String,
    pubTime: String?,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_up),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                modifier = Modifier.weight(1f),
                text = upName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pubTime ?: "",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun SmallVideoCardPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        SmallVideoCard(
            modifier = Modifier.width(380.dp),
            data =
                VideoCardData(
                    avid = 1L,
                    cid = 10L,
                    title = "这是一个测试视频标题，可能会很长很长很长很长很长",
                    cover = "",
                    upName = "测试UP主名称",
                    upMid = 100L,
                    playString = "12.3万",
                    danmakuString = "9999",
                    timeString = "10:42",
                    pubTime = "7月29日",
                ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CardCoverPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        Box(modifier = Modifier.width(380.dp)) {
            Box(modifier = Modifier.aspectRatio(1.6f)) {
                CardCover(
                    cover = "",
                    play = "12.3万",
                    danmaku = "9999",
                    time = "10:42",
                    progress = 0.6f,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardInfoPreview() {
    dev.frost819.newbv.core.theme.BVTheme {
        Box(modifier = Modifier.width(380.dp)) {
            CardInfo(
                title = "这是一个测试视频标题，可能会很长很长很长很长很长",
                upName = "测试UP主名称",
                pubTime = "7月29日",
            )
        }
    }
}

// endregion
