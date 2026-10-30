package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.state.player.PlayerErrorType
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.theme.BVShape

/**
 * 播放器大屏沉浸式错误恢复卡片。
 *
 * 当视频播放遇到网络断开、缓冲超时、解码异常或媒体加载失败时居中展示。
 * 提供友好文案、首项自动获焦与一键重试、画质降级、网络诊断及退出等闭环操作。
 *
 * @param errorMessage 错误详情文本
 * @param errorType 错误分类枚举
 * @param canDowngradeQuality 是否支持降低画质重试
 * @param onRetry 重新加载回调
 * @param onDowngradeQuality 降低画质重试回调
 * @param onOpenDiagnostics 打开网络诊断弹窗回调
 * @param onExit 退出播放回调
 * @param modifier Modifier
 */
@Composable
fun PlayerErrorOverlay(
    errorMessage: String?,
    errorType: PlayerErrorType = PlayerErrorType.Generic,
    canDowngradeQuality: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onDowngradeQuality: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }

    val (titleText, icon) =
        when (errorType) {
            PlayerErrorType.NetworkDisconnected -> "网络连接已断开" to Icons.Rounded.SignalWifiOff
            PlayerErrorType.Timeout -> "网络缓冲超时" to Icons.Rounded.Speed
            PlayerErrorType.SourceResolutionFailed -> "视频资源解析失败" to Icons.Rounded.Warning
            PlayerErrorType.DecoderError -> "视频解码遇到异常" to Icons.Rounded.Warning
            PlayerErrorType.Generic -> "播放遇到问题" to Icons.Rounded.Warning
        }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .width(620.dp)
                    .clip(MaterialTheme.shapes.large),
            colors =
                SurfaceDefaults.colors(
                    containerColor = Color(0xF2141724),
                ),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint =
                        if (errorType ==
                            PlayerErrorType.NetworkDisconnected
                        ) {
                            Color(0xFFFF5252)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage ?: "请检查电视网络连接或尝试重新播放",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 横向胶囊操作按钮行（支持横向滚动，首项自动获焦）
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onRetry != null) {
                        ErrorActionPill(
                            icon = Icons.Rounded.Refresh,
                            text = "重新加载",
                            focusRequester = firstFocusRequester,
                            onClick = onRetry,
                        )
                    }

                    if (canDowngradeQuality && onDowngradeQuality != null) {
                        ErrorActionPill(
                            icon = Icons.Rounded.Speed,
                            text = "降级画质重试",
                            focusRequester = if (onRetry == null) firstFocusRequester else null,
                            onClick = onDowngradeQuality,
                        )
                    }

                    if (onOpenDiagnostics != null) {
                        ErrorActionPill(
                            icon = Icons.Rounded.NetworkCheck,
                            text = "网络诊断",
                            onClick = onOpenDiagnostics,
                        )
                    }

                    if (onExit != null) {
                        ErrorActionPill(
                            icon = Icons.Rounded.ArrowBack,
                            text = "退出播放",
                            onClick = onExit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorActionPill(
    icon: ImageVector,
    text: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
        scale = TvFocusDefaults.surfaceScale(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
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
