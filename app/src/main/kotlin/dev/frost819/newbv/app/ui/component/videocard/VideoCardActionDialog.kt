package dev.frost819.newbv.app.ui.component.videocard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.R
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVShape

/**
 * 视频与剧集卡片上下文快捷操作浮层（对标 YouTube TV 底部半屏 ActionSheet）。
 *
 * 通过遥控器【菜单】键或长按卡片触发，在屏幕底部呈现悬浮抽屉面板，
 * 包含视频缩略图、元信息与横向排布的胶囊操作按钮。
 * 首选焦点直接定位至首个管理操作（如移出/删除），便于遥控器一键高频流转。
 *
 * @param title 视频或剧集标题。
 * @param upName UP 主名称或副标题信息。
 * @param cover 封面图片 URL。
 * @param timeString 时长或播放进度文本。
 * @param onDismiss 关闭弹窗回调。
 * @param onRemoveWatchLater 移出稍后再看。
 * @param onDeleteHistory 从历史记录中删除。
 * @param onRemoveFavorite 从当前收藏夹中取消收藏。
 * @param onCancelFollowingSeason 取消追番或追剧。
 * @param onAddWatchLater 添加到稍后再看。
 * @param onGoToDetailPage 查看详情页。
 * @param onGoToUpPage 查看 UP 主空间。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoCardActionDialog(
    title: String,
    upName: String = "",
    cover: String = "",
    timeString: String = "",
    onDismiss: () -> Unit,
    onRemoveWatchLater: (() -> Unit)? = null,
    onDeleteHistory: (() -> Unit)? = null,
    onRemoveFavorite: (() -> Unit)? = null,
    onCancelFollowingSeason: (() -> Unit)? = null,
    onAddWatchLater: (() -> Unit)? = null,
    onGoToDetailPage: (() -> Unit)? = null,
    onGoToUpPage: (() -> Unit)? = null,
) {
    val firstFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) {
            firstFocusRequester.requestFocus()
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .touchClickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .touchClickable(onClick = {}),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors =
                    SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // 顶部：封面 + 标题 + UP 主 / 进度信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (cover.isNotBlank()) {
                            Box(
                                modifier =
                                    Modifier
                                        .width(160.dp)
                                        .aspectRatio(1.6f)
                                        .clip(BVShape.medium),
                            ) {
                                AsyncImage(
                                    model = cover,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (upName.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_up),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = upName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                if (timeString.isNotBlank()) {
                                    Text(
                                        text = timeString,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    // 动作项列表构建
                    val actions =
                        buildList {
                            onRemoveWatchLater?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.remove_from_list,
                                        text = "移出稍后再看",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            onDeleteHistory?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.remove_from_list,
                                        text = "从历史中删除",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            onRemoveFavorite?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.remove_from_list,
                                        text = "取消收藏",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            onCancelFollowingSeason?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.remove_from_list,
                                        text = "取消追番",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            onAddWatchLater?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.add_to_list,
                                        text = "稍后再看",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            onGoToDetailPage?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.info_24px,
                                        text = "查看详情",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            onGoToUpPage?.let { action ->
                                add(
                                    DialogAction(
                                        iconRes = R.drawable.contact_page_24px,
                                        text = "UP 主空间",
                                        onClick = {
                                            action()
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                        }

                    // 底部：横向胶囊操作按钮（支持横向滚动，防止多动作项在大屏窄屏或长文案时溢出）
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions.forEachIndexed { index, dialogAction ->
                            ActionPillItem(
                                icon = painterResource(id = dialogAction.iconRes),
                                text = dialogAction.text,
                                focusRequester = if (index == 0) firstFocusRequester else null,
                                onClick = dialogAction.onClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 底部操作栏胶囊按钮。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionPillItem(
    icon: Painter,
    text: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.pill),
        scale = TvFocusDefaults.surfaceScale(),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class DialogAction(
    val iconRes: Int,
    val text: String,
    val onClick: () -> Unit,
)
