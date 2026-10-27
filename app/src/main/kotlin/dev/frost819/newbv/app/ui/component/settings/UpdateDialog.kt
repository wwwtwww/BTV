package dev.frost819.newbv.app.ui.component.settings

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.app.network.GithubApi
import dev.frost819.newbv.app.network.entity.GithubRelease
import dev.frost819.newbv.app.network.entity.compareSemanticVersions
import dev.frost819.newbv.app.network.entity.findApkAsset
import dev.frost819.newbv.app.network.entity.parseVersionCode
import dev.frost819.newbv.app.util.OtaInstallCallbackEvent
import dev.frost819.newbv.app.util.OtaUpdateInstaller
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 安装器响应超时阈值（毫秒）：超过此时间系统仍未弹出安装确认框，自动解除「安装中」等待并提供兜底指引。 */
private const val INSTALLER_LAUNCH_TIMEOUT_MS = 8_000L

/**
 * 更新检查与 OTA 升级弹窗。
 *
 * 针对 Android TV（尤其是雷鸟 FFALCON / TCL 等 Android 11+ 定制固件）深度加固：
 * 1. **双轨安装引擎**：优先通过 [OtaUpdateInstaller.installViaPackageInstallerSession] 流式安装并监听
 *    [OtaUpdateInstaller.installEvents] 广播回调，同时支持一键切换 [OtaUpdateInstaller.installViaLegacyViewIntent] 兼容安装；
 * 2. **公共 Download 目录同步导出**：下载完成后自动将 APK 备份至电视公共 `Download` 目录，
 *    即便电视 ROM 彻底禁用了应用内自我覆盖安装，用户也能直接在电视自带「文件管理器 -> 下载」中免 U 盘一键升级；
 * 3. **告别「无路可逃」假死**：任何阶段均允许按返回键或点击底部按钮关闭弹窗，并在 8 秒未检测到系统安装界面时自动切换为可操作的兜底引导页。
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val logger = Loggers.get("UpdateDialog")

    var updateStatus by remember { mutableStateOf(UpdateStatus.UpdatingInfo) }
    var bytesSentTotal by remember { mutableLongStateOf(0L) }
    var contentLength by remember { mutableLongStateOf(0L) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(targetValue = targetProgress, label = "update progress")
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var installWatchdogJob by remember { mutableStateOf<Job?>(null) }
    var latestRelease by remember { mutableStateOf<GithubRelease?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var exportedDownloadPath by remember { mutableStateOf<String?>(null) }
    var installErrorDetail by remember { mutableStateOf<String?>(null) }
    var waitingForPermissionReturn by remember { mutableStateOf(false) }

    DisposableEffect(show) {
        if (!show) {
            downloadJob?.cancel()
            installWatchdogJob?.cancel()
        }
        onDispose {
            downloadJob?.cancel()
            installWatchdogJob?.cancel()
        }
    }

    val startInstallWatchdog: () -> Unit = {
        installWatchdogJob?.cancel()
        installWatchdogJob =
            scope.launch {
                delay(INSTALLER_LAUNCH_TIMEOUT_MS)
                if (updateStatus == UpdateStatus.Installing) {
                    logger.warn { "System package installer did not respond within ${INSTALLER_LAUNCH_TIMEOUT_MS}ms" }
                    installErrorDetail =
                        "未检测到系统安装界面弹出（部分电视系统会静默拦截应用内升级）。"
                    updateStatus = UpdateStatus.InstallError
                }
            }
    }

    val triggerApkInstall: (File, Boolean) -> Unit = { file, useLegacyIntent ->
        downloadedApkFile = file
        installErrorDetail = null

        if (!useLegacyIntent && !OtaUpdateInstaller.canRequestPackageInstalls(context)) {
            logger.info { "Missing REQUEST_INSTALL_PACKAGES permission, prompting user to grant" }
            updateStatus = UpdateStatus.RequireUnknownSourcesPermission
        } else {
            updateStatus = UpdateStatus.Installing
            startInstallWatchdog()
            scope.launch(Dispatchers.IO) {
                logger.info {
                    "Triggering install: ${file.absolutePath}, size=${file.length()}, legacy=$useLegacyIntent"
                }
                val result =
                    if (useLegacyIntent) {
                        OtaUpdateInstaller.installViaLegacyViewIntent(context, file)
                    } else {
                        val sessionResult = OtaUpdateInstaller.installViaPackageInstallerSession(context, file)
                        sessionResult.fold(
                            onSuccess = { Result.success(Unit) },
                            onFailure = { sessionErr ->
                                if (sessionErr is CancellationException) throw sessionErr
                                logger.warn(sessionErr) {
                                    "PackageInstaller session failed, falling back to ACTION_VIEW"
                                }
                                OtaUpdateInstaller.installViaLegacyViewIntent(context, file)
                            },
                        )
                    }
                result.onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.error(error) { "Failed to launch installer (useLegacyIntent=$useLegacyIntent)" }
                    withContext(Dispatchers.Main) {
                        installWatchdogJob?.cancel()
                        installErrorDetail = error.message
                        updateStatus = UpdateStatus.InstallError
                    }
                }
            }
        }
    }

    // 监听 PackageInstaller 异步广播回调
    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        // 【调用顺序约束】：必须在 collect 之前调用 clearInstallEvents()，避免 replay=1 重放上次遗留的陈旧事件
        OtaUpdateInstaller.clearInstallEvents()
        OtaUpdateInstaller.installEvents.collect { event ->
            when (event) {
                is OtaInstallCallbackEvent.PendingUserAction -> {
                    // 系统已尝试拉起确认 Activity，继续保持超时守卫：若系统静默吞掉该 Activity，8 秒后自动转入兜底提示
                    startInstallWatchdog()
                }

                is OtaInstallCallbackEvent.Success -> {
                    installWatchdogJob?.cancel()
                    onHideDialog()
                }

                is OtaInstallCallbackEvent.Failure -> {
                    installWatchdogJob?.cancel()
                    logger.warn {
                        "PackageInstaller reported failure: status=${event.status}, msg=${event.message}"
                    }
                    // PackageInstaller 被系统拦截时，切到 IO 调度器尝试一次传统 ACTION_VIEW 兜底，
                    // 避免主线程执行同步文件检查、FileProvider 解析与 Binder IPC 引发低配 TV 设备 ANR
                    val apk = downloadedApkFile
                    if (apk != null) {
                        scope.launch(Dispatchers.IO) {
                            val exists = apk.exists()
                            val legacyResult =
                                if (exists) {
                                    OtaUpdateInstaller.installViaLegacyViewIntent(context, apk)
                                } else {
                                    null
                                }
                            withContext(Dispatchers.Main) {
                                if (legacyResult == null || legacyResult.isFailure) {
                                    installErrorDetail = event.message
                                    updateStatus = UpdateStatus.InstallError
                                } else {
                                    startInstallWatchdog()
                                }
                            }
                        }
                    } else {
                        installErrorDetail = event.message
                        updateStatus = UpdateStatus.InstallError
                    }
                }
            }
        }
    }

    // 从系统「未知来源安装权限」设置页返回时，若已获得授权则自动继续安装
    DisposableEffect(lifecycleOwner, show) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && show && waitingForPermissionReturn) {
                    waitingForPermissionReturn = false
                    val apk = downloadedApkFile
                    if (apk != null && apk.exists() && OtaUpdateInstaller.canRequestPackageInstalls(context)) {
                        triggerApkInstall(apk, false)
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val checkUpdate: () -> Unit = {
        updateStatus = UpdateStatus.UpdatingInfo
        installErrorDetail = null
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
            }.onFailure { error ->
                if (error is CancellationException) throw error
                logger.error(error) { "Failed to get latest version" }
                updateStatus = UpdateStatus.CheckError
            }.onSuccess {
                logger.info { "Find latest version ${latestRelease?.name}" }
            }
        }
    }

    val startUpdate: () -> Unit = {
        updateStatus = UpdateStatus.Downloading
        installErrorDetail = null
        downloadJob =
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val release = latestRelease ?: error("未获取到可用更新信息")
                    val apkAsset = release.findApkAsset() ?: error("未找到可用的安装包资产 (APK)")
                    val tempFilename = apkAsset.name

                    // 内部缓存目录存放主安装包
                    val tempDir = File(context.cacheDir, "ota_packages")
                    if (!tempDir.exists()) tempDir.mkdirs()

                    // 清理旧版本遗留包，避免占用磁盘空间，同时保留当前最新包
                    tempDir.listFiles()?.forEach { oldFile ->
                        if (oldFile.name != tempFilename && oldFile.name.endsWith(".apk")) {
                            oldFile.delete()
                        }
                    }

                    val tempFile = File(tempDir, tempFilename)

                    // 若本地已有完整下载且大小一致的安装包，直接导出并调起安装避免重复拉取
                    if (!tempFile.exists() || tempFile.length() <= 0L || tempFile.length() != apkAsset.size.toLong()) {
                        if (tempFile.exists()) tempFile.delete()
                        tempFile.createNewFile()

                        GithubApi.downloadUpdate(
                            downloadUrl = apkAsset.browserDownloadUrl,
                            file = tempFile,
                        ) { downloaded: Long, total: Long ->
                            bytesSentTotal = downloaded
                            contentLength = total
                            targetProgress =
                                if (total > 0) {
                                    downloaded.toFloat() / total
                                } else {
                                    0f
                                }
                        }
                    }

                    tempFile.setReadable(true, false)
                    downloadedApkFile = tempFile
                    // 同步导出一份至公共 Download 目录，方便在锁死应用内安装的电视上直接通过「文件管理器 -> 下载」安装
                    exportedDownloadPath = OtaUpdateInstaller.exportApkToPublicDownloads(context, tempFile)

                    if (show) {
                        triggerApkInstall(tempFile, false)
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.error(error) { "Failed to download update" }
                    installErrorDetail = error.message
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
            modifier = modifier.width(440.dp),
            onDismissRequest = {
                downloadJob?.cancel()
                installWatchdogJob?.cancel()
                onHideDialog()
            },
            title = {
                Text(
                    text =
                        when (updateStatus) {
                            UpdateStatus.UpdatingInfo -> "获取更新信息中"
                            UpdateStatus.Ready -> latestRelease?.name ?: "发现新版本"
                            UpdateStatus.Downloading -> "下载中"
                            UpdateStatus.Installing -> "正在调起系统安装器"
                            UpdateStatus.RequireUnknownSourcesPermission -> "需要开启未知来源安装权限"
                            UpdateStatus.NoAvailableUpdate -> "无可用更新"
                            UpdateStatus.CheckError -> "检查更新失败"
                            UpdateStatus.DownloadError -> "下载失败"
                            UpdateStatus.InstallError -> "未能自动弹出安装界面"
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
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "正在请求系统安装新版本，请稍候...")
                            exportedDownloadPath?.let { path ->
                                Text(
                                    text = "安装包已同步保存至电视「$path」，若电视未弹出安装框，可稍后打开电视自带【文件管理器 -> 下载】直接点击安装。",
                                )
                            }
                        }
                    }

                    UpdateStatus.RequireUnknownSourcesPermission -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "当前电视系统默认阻止应用内直接升级。请点击【去开启安装权限】并在列表中允许本应用安装未知应用。",
                            )
                            exportedDownloadPath?.let { path ->
                                Text(
                                    text = "💡 免 U 盘直装提示：安装包已自动保存至电视存储「$path」，您也可以直接打开电视自带的【文件管理器 / 应用助手 -> 下载】点击安装！",
                                )
                            }
                        }
                    }

                    UpdateStatus.DownloadError -> {
                        Text(text = "下载失败${installErrorDetail?.let { "：$it" } ?: ""}")
                    }

                    UpdateStatus.InstallError -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val errorMsg =
                                installErrorDetail
                                    ?: "电视系统拦截了应用内安装弹窗（常见于雷鸟/TCL 等电视系统限制）。"
                            Text(text = errorMsg)
                            exportedDownloadPath?.let { path ->
                                val manualHint =
                                    "✅ 免 U 盘解决办法：新版本安装包已为您导出至电视本地存储「$path」。" +
                                        "请直接打开电视自带的【文件管理器 / 电视卫士 -> 下载（Download）】即可一键点击完成升级！"
                                Text(text = manualHint)
                            }
                        }
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
                    UpdateStatus.Downloading,
                    -> {}

                    UpdateStatus.Ready -> {
                        Button(
                            onClick = startUpdate,
                            modifier = Modifier.touchClickable(onClick = startUpdate),
                        ) {
                            Text(text = "立即更新")
                        }
                    }

                    UpdateStatus.Installing -> {
                        val retryLegacyAction = {
                            val apk = downloadedApkFile
                            if (apk != null && apk.exists()) {
                                triggerApkInstall(apk, true)
                            }
                        }
                        Button(
                            onClick = retryLegacyAction,
                            modifier = Modifier.touchClickable(onClick = retryLegacyAction),
                        ) {
                            Text(text = "换用兼容模式重试")
                        }
                    }

                    UpdateStatus.RequireUnknownSourcesPermission -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val openSettingsAction = {
                                waitingForPermissionReturn = true
                                OtaUpdateInstaller.openUnknownSourceSettings(context)
                                Unit
                            }
                            Button(
                                onClick = openSettingsAction,
                                modifier = Modifier.touchClickable(onClick = openSettingsAction),
                            ) {
                                Text(text = "去开启安装权限")
                            }

                            val forceTryAction = {
                                val apk = downloadedApkFile
                                if (apk != null && apk.exists()) {
                                    triggerApkInstall(apk, true)
                                }
                            }
                            OutlinedButton(
                                onClick = forceTryAction,
                                modifier = Modifier.touchClickable(onClick = forceTryAction),
                            ) {
                                Text(text = "直接尝试安装")
                            }
                        }
                    }

                    UpdateStatus.InstallError -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val retryLegacyAction = {
                                val apk = downloadedApkFile
                                if (apk != null && apk.exists()) {
                                    triggerApkInstall(apk, true)
                                } else {
                                    checkUpdate()
                                }
                            }
                            Button(
                                onClick = retryLegacyAction,
                                modifier = Modifier.touchClickable(onClick = retryLegacyAction),
                            ) {
                                Text(text = "兼容模式重试")
                            }

                            val openSettingsAction = {
                                waitingForPermissionReturn = true
                                OtaUpdateInstaller.openUnknownSourceSettings(context)
                                Unit
                            }
                            OutlinedButton(
                                onClick = openSettingsAction,
                                modifier = Modifier.touchClickable(onClick = openSettingsAction),
                            ) {
                                Text(text = "去开启安装权限")
                            }
                        }
                    }

                    UpdateStatus.DownloadError, UpdateStatus.CheckError -> {
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
                val closeAction = {
                    downloadJob?.cancel()
                    installWatchdogJob?.cancel()
                    onHideDialog()
                }
                OutlinedButton(
                    onClick = closeAction,
                    modifier = Modifier.touchClickable(onClick = closeAction),
                ) {
                    Text(
                        text =
                            when (updateStatus) {
                                UpdateStatus.UpdatingInfo -> "我点错了"
                                UpdateStatus.Ready -> "打死不更"
                                UpdateStatus.NoAvailableUpdate -> "走了走了"
                                UpdateStatus.Downloading -> "取消下载"
                                UpdateStatus.Installing -> "关闭（安装将继续）"
                                UpdateStatus.RequireUnknownSourcesPermission,
                                UpdateStatus.CheckError,
                                UpdateStatus.DownloadError,
                                UpdateStatus.InstallError,
                                -> "关闭"
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
    RequireUnknownSourcesPermission,
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
