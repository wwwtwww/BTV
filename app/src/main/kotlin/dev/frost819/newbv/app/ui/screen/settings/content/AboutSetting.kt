package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.app.network.GithubApi
import dev.frost819.newbv.app.ui.component.settings.UpdateDialog
import dev.frost819.newbv.app.ui.component.settings.UpdateDialogMode
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val LATEST_VERSION_TIMEOUT_MS = 10_000L

/**
 * 关于页。
 *
 * 显示当前版本号、最新版本号，并提供「检查更新」与「版本更新说明」入口。
 *
 * @param modifier 修饰符。
 */
@Composable
fun AboutSetting(modifier: Modifier = Modifier) {
    val logger = remember { Loggers.get("AboutSetting") }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf(UpdateDialogMode.CheckUpdate) }
    var latestVersionName by remember { mutableStateOf("正在获取...") }

    LaunchedEffect(Unit) {
        runCatching {
            withTimeout(LATEST_VERSION_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    GithubApi.getLatestBuild().name
                }
            }
        }.onSuccess { name ->
            latestVersionName = name
            logger.info { "Find latest version $name" }
        }.onFailure { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) {
                throw error
            }
            logger.error(error) { "Failed to get latest version" }
            latestVersionName = "获取失败"
        }
    }

    val openCheckUpdate = {
        dialogMode = UpdateDialogMode.CheckUpdate
        showUpdateDialog = true
    }

    val openViewChangelog = {
        dialogMode = UpdateDialogMode.ViewChangelog
        showUpdateDialog = true
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = SettingsMenuNavItem.About.displayName,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "当前版本：${BuildConfig.VERSION_NAME}")
                Text(text = "最新版本：$latestVersionName")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = openCheckUpdate,
                    modifier = Modifier.touchClickable(onClick = openCheckUpdate),
                ) {
                    Text(text = "检查更新")
                }
                OutlinedButton(
                    onClick = openViewChangelog,
                    modifier = Modifier.touchClickable(onClick = openViewChangelog),
                ) {
                    Text(text = "版本更新说明")
                }
            }
        }
        Text(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "项目开源地址：https://github.com/wwwtwww/BTV",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
        )
    }

    UpdateDialog(
        show = showUpdateDialog,
        mode = dialogMode,
        onHideDialog = { showUpdateDialog = false },
    )
}
