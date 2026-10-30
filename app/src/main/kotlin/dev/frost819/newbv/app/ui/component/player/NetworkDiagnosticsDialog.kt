package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.app.util.NetworkDiagnosticReport
import dev.frost819.newbv.app.util.NetworkType
import dev.frost819.newbv.app.util.NetworkUtil
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.theme.BVShape
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URI

/**
 * 播放器大屏网络诊断弹窗。
 *
 * 用于检测并展示当前局域网类型、本机 IP、B 站 API 服务可达性与视频 CDN 节点连通状态。
 *
 * @param currentCdnUrl 当前播放视频流 CDN 地址
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun NetworkDiagnosticsDialog(
    currentCdnUrl: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<NetworkDiagnosticReport?>(null) }
    var checkingJob by remember { mutableStateOf<Job?>(null) }

    fun runCheck() {
        if (isChecking) return // 防重入保护：诊断正在进行中直接忽略重复触发
        isChecking = true
        checkingJob?.cancel()
        checkingJob =
            scope.launch {
                try {
                    report = NetworkUtil.runDiagnostics(context, currentCdnUrl)
                } finally {
                    isChecking = false
                }
            }
    }

    LaunchedEffect(Unit) {
        runCheck()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .width(580.dp)
                    .clip(MaterialTheme.shapes.large),
            colors =
                SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "网络连接诊断",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isChecking) {
                    Row(
                        modifier = Modifier.padding(vertical = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                        Text(
                            text = "正在检测局域网与 CDN 节点延时...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (report != null) {
                    val r = report!!
                    val cdnHost =
                        currentCdnUrl?.let {
                            try {
                                URI(it).host
                            } catch (_: Exception) {
                                null
                            }
                        } ?: "无活跃流地址"

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DiagnosticItem(
                            label = "网络连接类型",
                            value = r.networkType.displayName,
                            isSuccess = r.networkType != NetworkType.Disconnected,
                        )

                        DiagnosticItem(
                            label = "本机局域网 IP",
                            value = r.localIp ?: "未能获取",
                            isSuccess = r.localIp != null,
                        )

                        DiagnosticItem(
                            label = "B 站 API 服务延迟",
                            value = r.apiLatencyMs?.let { "${it}ms" } ?: "连接超时 / 不可达",
                            isSuccess = r.apiLatencyMs != null && r.apiLatencyMs < 1000,
                        )

                        DiagnosticItem(
                            label = "视频 CDN 节点 ($cdnHost)",
                            value =
                                if (currentCdnUrl == null) {
                                    "未连接 CDN 流"
                                } else {
                                    r.cdnLatencyMs?.let { "${it}ms" } ?: "测速超时 / 节点异常"
                                },
                            isSuccess = r.cdnLatencyMs != null && r.cdnLatencyMs < 1500,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        onClick = { runCheck() },
                        enabled = !isChecking,
                        colors =
                            ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            ),
                        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
                        scale = TvFocusDefaults.surfaceScale(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "重新测试",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        colors =
                            ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
                        scale = TvFocusDefaults.surfaceScale(),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "关闭",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
    isSuccess: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                fontSize = 15.sp,
            )
        }
    }
}
