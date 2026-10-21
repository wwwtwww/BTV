package dev.frost819.newbv.app.ui.component.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.app.network.GithubApi
import dev.frost819.newbv.app.network.entity.GithubRelease
import dev.frost819.newbv.app.network.entity.compareSemanticVersions
import dev.frost819.newbv.app.network.entity.findApkAsset
import dev.frost819.newbv.app.network.entity.parseVersionCode
import dev.frost819.newbv.app.util.CacheManager
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * 更新检查弹窗。
 *
 * 打开后自动检查最新版本，支持下载 APK 并调起安装。
 * 下载进度通过 [LinearProgressIndicator] 实时显示。
 *
 * @param show 是否显示弹窗。
 * @param onHideDialog 关闭弹窗回调。
 */
@Composable
fun UpdateDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = Loggers.get("UpdateDialog")

    var updateStatus by remember { mutableStateOf(UpdateStatus.UpdatingInfo) }
    var bytesSentTotal by remember { mutableLongStateOf(0L) }
    var contentLength by remember { mutableLongStateOf(0L) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(targetValue = targetProgress, label = "update progress")
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var latestRelease by remember { mutableStateOf<GithubRelease?>(null) }

    DisposableEffect(show) {
        if (!show) {
            downloadJob?.cancel()
        }
        onDispose {
            downloadJob?.cancel()
        }
    }

    val checkUpdate: () -> Unit = {
        updateStatus = UpdateStatus.UpdatingInfo
        scope.launch(Dispatchers.IO) {
            runCatching {
                val release = GithubApi.getLatestBuild()
                latestRelease = release
                val targetCode = release.parseVersionCode()
                val hasUpdate =
                    when {
                        targetCode != null -> targetCode > BuildConfig.VERSION_CODE
                        else -> {
                            val semVerDiff = compareSemanticVersions(release.tagName, BuildConfig.VERSION_NAME)
                            when {
                                semVerDiff != null -> semVerDiff > 0
                                else -> false
                            }
                        }
                    }
                if (!hasUpdate) {
                    updateStatus = UpdateStatus.NoAvailableUpdate
                    return@launch
                }
                updateStatus = UpdateStatus.Ready
            }.onFailure {
                logger.error(it) { "Failed to get latest version" }
                updateStatus = UpdateStatus.CheckError
            }.onSuccess {
                logger.info { "Find latest version ${latestRelease?.name}" }
            }
        }
    }

    val installUpdate: (File) -> Unit = { file ->
        updateStatus = UpdateStatus.Installing
        runCatching {
            // Android 8.0+ 检查并引导未知来源应用安装权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val manageIntent =
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${BuildConfig.APPLICATION_ID}"),
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                context.startActivity(manageIntent)
                updateStatus = UpdateStatus.Ready
                return@runCatching
            }

            file.setReadable(true, false)

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    file,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val resolveInfos =
                context.packageManager.queryIntentActivities(
                    intent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                )
            for (resolveInfo in resolveInfos) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

            context.startActivity(intent)
        }.onFailure {
            logger.error(it) { "Failed to launch package installer" }
            updateStatus = UpdateStatus.InstallError
        }
    }

    val startUpdate: () -> Unit = {
        updateStatus = UpdateStatus.Downloading
        downloadJob =
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val release = latestRelease ?: error("未获取到可用更新信息")
                    val apkAsset = release.findApkAsset() ?: error("未找到可用的安装包资产 (APK)")
                    val tempFilename = apkAsset.name
                    val tempDir =
                        context.externalCacheDir?.let { File(it, "update_downloader") }
                            ?: context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "update_downloader") }
                            ?: File(context.cacheDir, "update_downloader")
                    if (!tempDir.exists()) tempDir.mkdirs()
                    val tempFile = File(tempDir, tempFilename)

                    // 若本地已有完整下载且大小一致的安装包，直接调起安装避免重复拉取
                    if (tempFile.exists() && tempFile.length() > 0 && tempFile.length() == apkAsset.size.toLong()) {
                        tempFile.setReadable(true, false)
                        if (show) installUpdate(tempFile)
                        return@launch
                    }

                    if (tempFile.exists()) tempFile.delete()
                    tempFile.createNewFile()
                    tempFile.setReadable(true, false)

                    GithubApi.downloadUpdate(apkAsset.browserDownloadUrl, tempFile) { downloaded: Long, total: Long ->
                        bytesSentTotal = downloaded
                        contentLength = total
                        targetProgress =
                            if (total > 0) {
                                downloaded.toFloat() / total
                            } else {
                                0f
                            }
                    }
                    // 缓存写入后检查阈值，保留刚下载的 APK 待安装
                    CacheManager(context).checkCache(preserve = tempFile)
                    if (show) installUpdate(tempFile)
                }.onFailure {
                    logger.error(it) { "Failed to download update" }
                    updateStatus = UpdateStatus.DownloadError
                }
            }
    }

    LaunchedEffect(show) {
        if (show) {
            checkUpdate()
        } else {
            updateStatus = UpdateStatus.UpdatingInfo
        }
    }

    if (show) {
        AlertDialog(
            modifier = modifier.width(400.dp),
            onDismissRequest = onHideDialog,
            title = {
                Text(
                    text =
                        when (updateStatus) {
                            UpdateStatus.UpdatingInfo -> "获取更新信息中"
                            UpdateStatus.Ready -> latestRelease?.name ?: "Loading..."
                            UpdateStatus.Downloading -> "下载中"
                            UpdateStatus.Installing -> "安装中"
                            UpdateStatus.NoAvailableUpdate -> "无可用更新"
                            UpdateStatus.CheckError -> "检查更新失败"
                            UpdateStatus.DownloadError -> "下载失败"
                            UpdateStatus.InstallError -> "安装失败"
                        },
                )
            },
            text = {
                when (updateStatus) {
                    UpdateStatus.UpdatingInfo -> {
                        Text(text = "检查更新中...")
                    }

                    UpdateStatus.Ready -> {
                        Text(text = latestRelease?.body ?: "Empty content")
                    }

                    UpdateStatus.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text = "${bytesSentTotal.toMBString()}/${contentLength.toMBString()}",
                                )
                            }
                        }
                    }

                    UpdateStatus.Installing -> {
                        Text(text = "请坐和放宽")
                    }

                    UpdateStatus.DownloadError -> {
                        Text(text = "下载失败")
                    }

                    UpdateStatus.InstallError -> {
                        Text(text = "安装失败")
                    }

                    UpdateStatus.CheckError -> {
                        Text(text = "获取更新信息失败")
                    }

                    UpdateStatus.NoAvailableUpdate -> {
                        Text(text = "真没更新，骗你是小狗！")
                    }
                }
            },
            confirmButton = {
                when (updateStatus) {
                    UpdateStatus.UpdatingInfo, UpdateStatus.NoAvailableUpdate,
                    UpdateStatus.Downloading, UpdateStatus.Installing,
                    -> {}

                    UpdateStatus.Ready -> {
                        Button(
                            onClick = startUpdate,
                            modifier = Modifier.touchClickable(onClick = startUpdate),
                        ) {
                            Text(text = "立即更新")
                        }
                    }

                    UpdateStatus.InstallError, UpdateStatus.DownloadError, UpdateStatus.CheckError -> {
                        Button(
                            onClick = checkUpdate,
                            modifier = Modifier.touchClickable(onClick = checkUpdate),
                        ) {
                            Text(text = "再试一次")
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled =
                        updateStatus !in
                            setOf(
                                UpdateStatus.Downloading,
                                UpdateStatus.Installing,
                            ),
                    onClick = onHideDialog,
                    modifier =
                        Modifier.touchClickable(onClick = {
                            if (updateStatus !in
                                setOf(
                                    UpdateStatus.Downloading,
                                    UpdateStatus.Installing,
                                )
                            ) {
                                onHideDialog()
                            }
                        }),
                ) {
                    Text(
                        text =
                            when (updateStatus) {
                                UpdateStatus.UpdatingInfo -> "我点错了"
                                UpdateStatus.Ready -> "打死不更"
                                UpdateStatus.NoAvailableUpdate -> "走了走了"
                                UpdateStatus.CheckError, UpdateStatus.DownloadError,
                                UpdateStatus.InstallError,
                                -> "算了算了"
                                UpdateStatus.Downloading, UpdateStatus.Installing -> "你已经无路可逃！"
                            },
                    )
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        )
    }
}

/** 更新状态。 */
enum class UpdateStatus {
    UpdatingInfo,
    Ready,
    Downloading,
    Installing,
    NoAvailableUpdate,
    CheckError,
    DownloadError,
    InstallError,
}

private fun Long.toMBString(): String =
    when {
        this >= 1_000_000 -> "%.1f MB".format(this / 1_000_000.0)
        this >= 1_000 -> "%.1f KB".format(this / 1_000.0)
        else -> "$this B"
    }
