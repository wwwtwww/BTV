package dev.frost819.newbv.app.ui.component.videocard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
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
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.core.theme.BVShape

/**
 * 视频卡片上下文快捷菜单浮层（对标 YouTube TV 菜单弹窗）。
 *
 * 通过遥控器「Menu / 菜单键」或长按触发，在屏幕中央浮动展示操作项，
 * 避免粗暴替换卡片封面，提供更平滑的高清客厅大屏体验。
 *
 * @param title 视频标题。
 * @param upName UP 主名称。
 * @param onDismiss 关闭弹窗回调。
 * @param onAddWatchLater 添加到稍后再看。
 * @param onRemoveWatchLater 移出稍后再看。
 * @param onGoToDetailPage 查看详情页。
 * @param onGoToUpPage 查看 UP 主主页。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoCardActionDialog(
    title: String,
    upName: String = "",
    onDismiss: () -> Unit,
    onAddWatchLater: (() -> Unit)? = null,
    onRemoveWatchLater: (() -> Unit)? = null,
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
                    .width(420.dp)
                    .clip(BVShape.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val actions =
                    buildList {
                        onRemoveWatchLater?.let { action ->
                            add(
                                DialogAction(
                                    iconRes = R.drawable.remove_from_list,
                                    text = "从稍后再看中移除",
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
                                    text = "添加到稍后再看",
                                    onClick = {
                                        action()
                                        onDismiss()
                                    },
                                ),
                            )
                        }
                        onGoToUpPage?.let { action ->
                            val label = if (upName.isNotBlank()) "查看 UP 主主页 ($upName)" else "查看 UP 主主页"
                            add(
                                DialogAction(
                                    iconRes = R.drawable.contact_page_24px,
                                    text = label,
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
                                    text = "查看视频详情",
                                    onClick = {
                                        action()
                                        onDismiss()
                                    },
                                ),
                            )
                        }
                    }

                actions.forEachIndexed { index, dialogAction ->
                    ActionDialogItem(
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionDialogItem(
    icon: Painter,
    text: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.medium),
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
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
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
