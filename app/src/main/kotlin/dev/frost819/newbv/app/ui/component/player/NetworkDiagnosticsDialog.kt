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
import androidx.compose.ui.graphics.Color
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
    var isChecking by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf<NetworkDiagnosticReport?>(null) }

    fun runCheck() {
        isChecking = true
        scope.launch {
            report = NetworkUtil.runDiagnostics(context, currentCdnUrl)
            isChecking = false
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
                    containerColor = Color(0xF0121622),
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
                        color = Color.White,
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
                            text = "正在检测网络与 B 站服务连通性...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                } else {
                    val rep = report
                    if (rep != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            DiagnosticItem(
                                label = "网络连接",
                                value = rep.networkType.displayName,
                                isSuccess = rep.networkType != NetworkType.Disconnected,
                            )
                            DiagnosticItem(
                                label = "本地 IP",
                                value = rep.localIp ?: "未分配或不可达",
                                isSuccess = rep.localIp != null,
                            )
                            DiagnosticItem(
                                label = "B 站 API 状态",
                                value =
                                    if (rep.apiLatencyMs != null) {
                                        "连通正常 (${rep.apiLatencyMs}ms)"
                                    } else {
                                        "无法连接 (超时或被阻断)"
                                    },
                                isSuccess = rep.apiLatencyMs != null,
                            )
                            val cdnHost =
                                remember(currentCdnUrl) {
                                    currentCdnUrl?.let {
                                        try {
                                            URI(it).host
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }
                            if (cdnHost != null) {
                                DiagnosticItem(
                                    label = "视频 CDN 节点",
                                    value =
                                        if (rep.cdnLatencyMs != null) {
                                            "$cdnHost (${rep.cdnLatencyMs}ms)"
                                        } else {
                                            "$cdnHost (连接超时)"
                                        },
                                    isSuccess = rep.cdnLatencyMs != null,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                ) {
                    Surface(
                        onClick = { runCheck() },
                        enabled = !isChecking,
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
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) Color.White else Color(0xFFFF8A80),
                fontSize = 15.sp,
            )
        }
    }
}
