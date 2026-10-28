package dev.frost819.newbv.app.ui.component.settings

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.app.network.GithubApi
import dev.frost819.newbv.app.network.entity.GithubRelease
import dev.frost819.newbv.app.network.entity.findApkAsset
import dev.frost819.newbv.app.util.OtaInstallCallbackEvent
import dev.frost819.newbv.app.util.OtaInstallMode
import dev.frost819.newbv.app.util.OtaUpdateInstaller
import dev.frost819.newbv.app.util.ReleaseNoteBlock
import dev.frost819.newbv.app.util.ReleaseNoteEntry
import dev.frost819.newbv.app.util.ReleaseNotesParser
import dev.frost819.newbv.core.focus.isDpadDown
import dev.frost819.newbv.core.focus.isDpadUp
import dev.frost819.newbv.core.focus.isKeyDown
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.core.theme.BVShape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

/** 安装器响应超时阈值（毫秒）：超过此时间系统仍未弹出安装确认框，自动解除「安装中」等待并提供兜底指引。 */
private const val INSTALLER_LAUNCH_TIMEOUT_MS = 8_000L

/** 检查更新网络请求超时阈值（毫秒）。 */
private const val CHECK_UPDATE_TIMEOUT_MS = 10_000L

/**
 * 更新弹窗运行模式。
 */
enum class UpdateDialogMode {
    /** 联网检查远程新版本并执行 OTA 下载升级；若无更新则展示当前版本的更新说明。 */
    CheckUpdate,

    /** 直接查阅本地内置的当前及历史版本更新说明（支持离线秒开）。 */
    ViewChangelog,
}

/**
 * 更新检查、版本说明浏览与 OTA 升级弹窗。
 *
 * 针对 Android TV 大屏（含雷鸟 FFALCON / TCL 等定制固件）深度优化：
 * 1. **升级前展示详细更新说明**：采用 620dp 大屏宽版卡片，头部呈现「当前版本 → 新版本、包大小、发布日期」，
 *    正文支持跨多版本升级说明自动聚合及遥控器方向键上下平滑滚动；
 * 2. **直达安装无前置拦截**：下载完成后，若系统未授予未知来源权限则直接调用已验证兼容的 `ACTION_VIEW` 调起电视安装器，
 *    避免定制 TV ROM 上前置权限拦截页阻断更新；
 * 3. **空壳设置页秒退检测与多级降级**：若触发安装超时兜底页，点击「去开启安装权限」会自动监测 800ms 空壳秒退，
 *    自动降级打开应用详情/系统设置页并展示手动路径与公共 `Download` 目录免 U 盘直装提示。
 *
 * @param modifier 修饰符。
 * @param show 是否显示弹窗。
 * @param mode 弹窗运行模式（默认 [UpdateDialogMode.CheckUpdate]）。
 * @param onHideDialog 关闭弹窗回调。
 */
@Composable
fun UpdateDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    mode: UpdateDialogMode = UpdateDialogMode.CheckUpdate,
    onHideDialog: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val logger = remember { Loggers.get("UpdateDialog") }

    var updateStatus by remember { mutableStateOf(UpdateStatus.UpdatingInfo) }
    var bytesSentTotal by remember { mutableLongStateOf(0L) }
    var contentLength by remember { mutableLongStateOf(0L) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(targetValue = targetProgress, label = "update progress")

    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var installWatchdogJob by remember { mutableStateOf<Job?>(null) }
    var settingsStubCheckJob by remember { mutableStateOf<Job?>(null) }

    var latestRelease by remember { mutableStateOf<GithubRelease?>(null) }
    var releaseNoteEntries by remember { mutableStateOf<List<ReleaseNoteEntry>>(emptyList()) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var exportedDownloadPath by remember { mutableStateOf<String?>(null) }
    var installErrorDetail by remember { mutableStateOf<String?>(null) }
    var settingsFallbackHint by remember { mutableStateOf<String?>(null) }

    var waitingForPermissionReturn by remember { mutableStateOf(false) }
    var settingsDidPause by remember { mutableStateOf(false) }
    var settingsUsedFallback by remember { mutableStateOf(false) }
    var settingsLaunchElapsedMs by remember { mutableLongStateOf(0L) }

    val primaryButtonFocusRequester = remember { FocusRequester() }
    val notesScrollFocusRequester = remember { FocusRequester() }

    val maxNotesHeightDp: Dp =
        with(density) {
            (windowInfo.containerSize.height * 0.48f).toDp().coerceIn(180.dp, 340.dp)
        }

    DisposableEffect(show) {
        if (!show) {
            downloadJob?.cancel()
            installWatchdogJob?.cancel()
            settingsStubCheckJob?.cancel()
            waitingForPermissionReturn = false
        }
        onDispose {
            downloadJob?.cancel()
            installWatchdogJob?.cancel()
            settingsStubCheckJob?.cancel()
        }
    }

    val startInstallWatchdog: () -> Unit = {
        installWatchdogJob?.cancel()
        installWatchdogJob =
            scope.launch {
                delay(INSTALLER_LAUNCH_TIMEOUT_MS)
                // 仅当应用仍停留在前台 RESUMED 状态（说明系统安装确认页未覆盖前台或已被静默销毁）时判定超时拦截
                val stillInForeground =
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (updateStatus == UpdateStatus.Installing && stillInForeground) {
                    logger.warn {
                        "System package installer did not take foreground within ${INSTALLER_LAUNCH_TIMEOUT_MS}ms"
                    }
                    installErrorDetail = "未检测到电视系统安装界面弹出（部分定制电视系统会静默拦截应用内升级）。"
                    updateStatus = UpdateStatus.InstallError
                }
            }
    }

    val triggerApkInstall: (File, Boolean) -> Unit = { file, forceLegacy ->
        downloadedApkFile = file
        installErrorDetail = null
        settingsFallbackHint = null
        updateStatus = UpdateStatus.Installing
        startInstallWatchdog()

        scope.launch {
            val installResult =
                withContext(Dispatchers.IO) {
                    val canRequest = OtaUpdateInstaller.canRequestPackageInstalls(context)
                    val preferredMode = OtaUpdateInstaller.selectPreferredInstallMode(canRequest, forceLegacy)
                    logger.info {
                        "Triggering install: ${file.absolutePath}, size=${file.length()}, " +
                            "canRequest=$canRequest, mode=$preferredMode"
                    }
                    when (preferredMode) {
                        OtaInstallMode.LegacyActionView -> {
                            OtaUpdateInstaller.installViaLegacyViewIntent(context, file)
                        }

                        OtaInstallMode.PackageInstallerSession -> {
                            val sessionRes = OtaUpdateInstaller.installViaPackageInstallerSession(context, file)
                            sessionRes.fold(
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
                    }
                }

            installResult.onFailure { error ->
                if (error is CancellationException) throw error
                logger.error(error) { "Failed to launch installer (forceLegacy=$forceLegacy)" }
                installWatchdogJob?.cancel()
                installErrorDetail = error.message
                updateStatus = UpdateStatus.InstallError
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
                    val apk = downloadedApkFile
                    if (apk != null) {
                        val legacyResult =
                            withContext(Dispatchers.IO) {
                                if (apk.exists()) {
                                    OtaUpdateInstaller.installViaLegacyViewIntent(context, apk)
                                } else {
                                    null
                                }
                            }
                        if (legacyResult == null || legacyResult.isFailure) {
                            installErrorDetail = event.message
                            updateStatus = UpdateStatus.InstallError
                        } else {
                            startInstallWatchdog()
                        }
                    } else {
                        installErrorDetail = event.message
                        updateStatus = UpdateStatus.InstallError
                    }
                }
            }
        }
    }

    val openSettingsWithFallback: () -> Unit = {
        settingsStubCheckJob?.cancel()
        waitingForPermissionReturn = true
        settingsDidPause = false
        settingsUsedFallback = false
        settingsFallbackHint = null
        settingsLaunchElapsedMs = SystemClock.elapsedRealtime()

        settingsStubCheckJob =
            scope.launch {
                val launched =
                    withContext(Dispatchers.IO) {
                        OtaUpdateInstaller.openUnknownSourceSettings(
                            context = context,
                            skipUnknownSourcesAction = false,
                        )
                    }
                if (!launched) {
                    waitingForPermissionReturn = false
                    settingsFallbackHint =
                        "当前电视系统禁用了设置跳转，请使用遥控器打开电视【系统设置 → 应用 → 安全与限制】开启允许安装，" +
                        "或前往电视【文件管理器 → 下载】直接安装。"
                } else {
                    settingsLaunchElapsedMs = SystemClock.elapsedRealtime()
                    // 守护检测：针对 Theme.NoDisplay 空壳 Activity 不触发 ON_PAUSE 静默忽略的情况，800ms 后自动降级跳转
                    delay(OtaUpdateInstaller.SETTINGS_STUB_BOUNCE_THRESHOLD_MS)
                    if (waitingForPermissionReturn && !settingsDidPause && !settingsUsedFallback) {
                        settingsUsedFallback = true
                        settingsLaunchElapsedMs = SystemClock.elapsedRealtime()
                        val fallbackLaunched =
                            withContext(Dispatchers.IO) {
                                OtaUpdateInstaller.openUnknownSourceSettings(
                                    context = context,
                                    skipUnknownSourcesAction = true,
                                )
                            }
                        settingsFallbackHint =
                            if (fallbackLaunched) {
                                "电视未知来源专属页无响应，已自动为您打开应用详情/系统设置页；请在【应用权限 / 安全与限制】中开启安装权限。"
                            } else {
                                "电视系统拦截了设置页跳转，请手动进入电视【系统设置 → 应用 → 安全与限制】开启，或前往【文件管理器 → 下载】安装。"
                            }
                        if (!fallbackLaunched) {
                            waitingForPermissionReturn = false
                        }
                    }
                }
            }
    }

    // 监听设置页返回：支持授权后自动续装 + 800ms 空壳 Activity 秒退自动降级
    DisposableEffect(lifecycleOwner, show) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (!show || !waitingForPermissionReturn) return@LifecycleEventObserver

                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        settingsDidPause = true
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        val elapsed = SystemClock.elapsedRealtime() - settingsLaunchElapsedMs
                        val hasPermission = OtaUpdateInstaller.canRequestPackageInstalls(context)

                        if (hasPermission) {
                            waitingForPermissionReturn = false
                            settingsStubCheckJob?.cancel()
                            val apk = downloadedApkFile
                            if (apk != null && apk.exists()) {
                                triggerApkInstall(apk, false)
                            }
                        } else if (
                            OtaUpdateInstaller.shouldFallbackFromSettingsStub(
                                elapsedMs = elapsed,
                                hasInstallPermission = false,
                                alreadyUsedFallback = settingsUsedFallback,
                            )
                        ) {
                            settingsUsedFallback = true
                            settingsStubCheckJob?.cancel()
                            settingsLaunchElapsedMs = SystemClock.elapsedRealtime()
                            scope.launch {
                                val fallbackLaunched =
                                    withContext(Dispatchers.IO) {
                                        OtaUpdateInstaller.openUnknownSourceSettings(
                                            context = context,
                                            skipUnknownSourcesAction = true,
                                        )
                                    }
                                settingsFallbackHint =
                                    if (fallbackLaunched) {
                                        "检测到电视未知来源页被系统空壳秒退，已自动为您切换打开应用详情/系统设置页。"
                                    } else {
                                        "当前电视系统不支持直达权限页，请手动前往电视【系统设置 → 应用 → 安全与限制】开启，或前往【文件管理器 → 下载】安装。"
                                    }
                                if (!fallbackLaunched) {
                                    waitingForPermissionReturn = false
                                }
                            }
                        } else {
                            waitingForPermissionReturn = false
                            settingsStubCheckJob?.cancel()
                        }
                    }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val loadLocalChangelogOnly: () -> Unit = {
        updateStatus = UpdateStatus.UpdatingInfo
        installErrorDetail = null
        settingsFallbackHint = null
        scope.launch {
            val entries =
                withContext(Dispatchers.IO) {
                    val bundled = ReleaseNotesParser.readBundledChangelog(context)
                    ReleaseNotesParser.resolveLocalVersionNotes(
                        currentVersionName = BuildConfig.VERSION_NAME,
                        bundledChangelog = bundled,
                        includeHistory = true,
                    )
                }
            latestRelease = null
            releaseNoteEntries = entries
            updateStatus = UpdateStatus.NoAvailableUpdate
        }
    }

    val checkUpdate: () -> Unit = {
        updateStatus = UpdateStatus.UpdatingInfo
        installErrorDetail = null
        settingsFallbackHint = null
        scope.launch {
            runCatching {
                withTimeout(CHECK_UPDATE_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        val bundled = ReleaseNotesParser.readBundledChangelog(context)
                        val releases =
                            runCatching { GithubApi.getReleases(pageSize = 20) }
                                .getOrNull()
                                ?.takeIf { it.isNotEmpty() }
                                ?: listOf(GithubApi.getLatestBuild())
                        val upgradeResult =
                            ReleaseNotesParser.evaluateUpgrade(
                                releases = releases,
                                currentVersionCode = BuildConfig.VERSION_CODE,
                                currentVersionName = BuildConfig.VERSION_NAME,
                                bundledChangelog = bundled,
                            )
                        val localNotes =
                            if (upgradeResult.latestRelease == null) {
                                ReleaseNotesParser.resolveLocalVersionNotes(
                                    currentVersionName = BuildConfig.VERSION_NAME,
                                    bundledChangelog = bundled,
                                    remoteReleases = releases,
                                    includeHistory = false,
                                )
                            } else {
                                emptyList()
                            }
                        Triple(upgradeResult.latestRelease, upgradeResult.notes, localNotes)
                    }
                }
            }.onSuccess { (targetRelease, upgradeNotes, localNotes) ->
                if (targetRelease == null) {
                    latestRelease = null
                    releaseNoteEntries = localNotes
                    updateStatus = UpdateStatus.NoAvailableUpdate
                } else {
                    latestRelease = targetRelease
                    releaseNoteEntries = upgradeNotes
                    logger.info { "Found latest release: ${targetRelease.name} (${targetRelease.tagName})" }
                    updateStatus = UpdateStatus.Ready
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to check latest release" }
                updateStatus = UpdateStatus.CheckError
            }
        }
    }

    val startUpdate: () -> Unit = {
        updateStatus = UpdateStatus.Downloading
        installErrorDetail = null
        settingsFallbackHint = null
        bytesSentTotal = 0L
        contentLength = 0L
        targetProgress = 0f

        downloadJob =
            scope.launch {
                runCatching {
                    val release = latestRelease ?: error("未获取到可用更新信息")
                    val apkAsset = release.findApkAsset() ?: error("未找到可用的安装包资产 (APK)")

                    val (apkFile, exportedPath) =
                        withContext(Dispatchers.IO) {
                            val tempFilename = apkAsset.name
                            val tempDir = File(context.cacheDir, "ota_packages")
                            if (!tempDir.exists()) tempDir.mkdirs()

                            tempDir.listFiles()?.forEach { oldFile ->
                                if (oldFile.name != tempFilename && oldFile.name.endsWith(".apk")) {
                                    oldFile.delete()
                                }
                            }

                            val tempFile = File(tempDir, tempFilename)
                            val expectedSize = apkAsset.size.toLong()
                            if (!tempFile.exists() || tempFile.length() <= 0L || tempFile.length() != expectedSize) {
                                if (tempFile.exists()) tempFile.delete()
                                tempFile.createNewFile()

                                GithubApi.downloadUpdate(
                                    downloadUrl = apkAsset.browserDownloadUrl,
                                    file = tempFile,
                                ) { downloaded: Long, total: Long ->
                                    bytesSentTotal = downloaded
                                    contentLength = total
                                    targetProgress = if (total > 0) downloaded.toFloat() / total else 0f
                                }
                            } else {
                                bytesSentTotal = expectedSize
                                contentLength = expectedSize
                                targetProgress = 1f
                            }

                            tempFile.setReadable(true, false)
                            val publicPath = OtaUpdateInstaller.exportApkToPublicDownloads(context, tempFile)
                            tempFile to publicPath
                        }

                    downloadedApkFile = apkFile
                    exportedDownloadPath = exportedPath

                    if (show) {
                        triggerApkInstall(apkFile, false)
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.error(error) { "Failed to download update" }
                    installErrorDetail = error.message
                    updateStatus = UpdateStatus.DownloadError
                }
            }
    }

    LaunchedEffect(show, mode) {
        if (show) {
            when (mode) {
                UpdateDialogMode.CheckUpdate -> checkUpdate()
                UpdateDialogMode.ViewChangelog -> loadLocalChangelogOnly()
            }
        } else {
            updateStatus = UpdateStatus.UpdatingInfo
        }
    }

    // 状态稳定后自动将初始焦点引导至底部主操作按钮，确保遥控器一键确认即可操作
    LaunchedEffect(show, updateStatus, mode) {
        if (show && updateStatus.shouldAutoFocusPrimaryButton()) {
            delay(60L)
            runCatching { primaryButtonFocusRequester.requestFocus() }
        }
    }

    if (show) {
        val dismissDialogAction = {
            downloadJob?.cancel()
            installWatchdogJob?.cancel()
            settingsStubCheckJob?.cancel()
            waitingForPermissionReturn = false
            onHideDialog()
        }

        Dialog(
            onDismissRequest = dismissDialogAction,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = modifier.width(620.dp).wrapContentHeight(),
                shape = BVShape.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    UpdateDialogHeader(
                        mode = mode,
                        updateStatus = updateStatus,
                        latestRelease = latestRelease,
                        noteCount = releaseNoteEntries.size,
                    )

                    UpdateDialogBody(
                        mode = mode,
                        updateStatus = updateStatus,
                        releaseNoteEntries = releaseNoteEntries,
                        progress = progress,
                        bytesSentTotal = bytesSentTotal,
                        contentLength = contentLength,
                        exportedDownloadPath = exportedDownloadPath,
                        installErrorDetail = installErrorDetail,
                        settingsFallbackHint = settingsFallbackHint,
                        maxNotesHeightDp = maxNotesHeightDp,
                        notesScrollFocusRequester = notesScrollFocusRequester,
                        primaryButtonFocusRequester = primaryButtonFocusRequester,
                    )

                    UpdateDialogFooter(
                        updateStatus = updateStatus,
                        hasReleaseNotesBox =
                            (
                                updateStatus == UpdateStatus.Ready ||
                                    updateStatus == UpdateStatus.NoAvailableUpdate
                            ) &&
                                releaseNoteEntries.isNotEmpty(),
                        primaryButtonFocusRequester = primaryButtonFocusRequester,
                        notesScrollFocusRequester = notesScrollFocusRequester,
                        onStartUpdate = startUpdate,
                        onRetryCheck = checkUpdate,
                        onRetryLegacyInstall = {
                            val apk = downloadedApkFile
                            if (apk != null && apk.exists()) {
                                triggerApkInstall(apk, true)
                            } else {
                                checkUpdate()
                            }
                        },
                        onOpenSettings = openSettingsWithFallback,
                        onDismiss = dismissDialogAction,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateDialogHeader(
    mode: UpdateDialogMode,
    updateStatus: UpdateStatus,
    latestRelease: GithubRelease?,
    noteCount: Int,
) {
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val titleText =
            when {
                mode == UpdateDialogMode.ViewChangelog -> "版本更新说明"
                updateStatus == UpdateStatus.UpdatingInfo -> "正在检查更新"
                updateStatus == UpdateStatus.Ready -> {
                    val releaseTitle = latestRelease?.name?.takeIf { it.isNotBlank() }
                    if (releaseTitle != null) "发现新版本 · $releaseTitle" else "发现新版本"
                }
                updateStatus == UpdateStatus.Downloading -> "正在下载更新包"
                updateStatus == UpdateStatus.Installing -> "正在调起电视安装器"
                updateStatus == UpdateStatus.NoAvailableUpdate -> "当前已是最新版本"
                updateStatus == UpdateStatus.CheckError -> "检查更新失败"
                updateStatus == UpdateStatus.DownloadError -> "更新包下载失败"
                updateStatus == UpdateStatus.InstallError -> "未能自动弹出安装界面"
                else -> "应用更新"
            }

        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (updateStatus == UpdateStatus.Ready && latestRelease != null) {
            val currentBase = ReleaseNotesParser.formatBaseVersion(BuildConfig.VERSION_NAME)
            val targetBase = ReleaseNotesParser.formatBaseVersion(latestRelease.tagName)
            val apkSize = latestRelease.findApkAsset()?.size?.toLong()
            val publishDate = ReleaseNotesParser.extractDate(latestRelease.publishedAt)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetadataPill(text = "版本 $currentBase → $targetBase", highlight = true)
                if (apkSize != null && apkSize > 0L) {
                    MetadataPill(text = "大小 ${apkSize.toMBString()}")
                }
                if (publishDate != null) {
                    MetadataPill(text = "发布于 $publishDate")
                }
                if (noteCount > 1) {
                    MetadataPill(text = "含跨 $noteCount 个版本的全部更新")
                }
            }
        } else if (updateStatus == UpdateStatus.NoAvailableUpdate) {
            Text(
                text = "当前安装版本：${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
            )
        }
    }
}

@Composable
private fun MetadataPill(
    text: String,
    highlight: Boolean = false,
) {
    val containerColor =
        if (highlight) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (highlight) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        }

    Box(
        modifier =
            Modifier
                .clip(BVShape.pill)
                .background(containerColor)
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}

@Composable
private fun UpdateDialogBody(
    mode: UpdateDialogMode,
    updateStatus: UpdateStatus,
    releaseNoteEntries: List<ReleaseNoteEntry>,
    progress: Float,
    bytesSentTotal: Long,
    contentLength: Long,
    exportedDownloadPath: String?,
    installErrorDetail: String?,
    settingsFallbackHint: String?,
    maxNotesHeightDp: Dp,
    notesScrollFocusRequester: FocusRequester,
    primaryButtonFocusRequester: FocusRequester,
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)

    when (updateStatus) {
        UpdateStatus.UpdatingInfo -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text =
                        if (mode == UpdateDialogMode.ViewChangelog) {
                            "正在读取版本更新说明..."
                        } else {
                            "正在连接 GitHub 获取最新版本与更新说明，请稍候..."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                )
            }
        }

        UpdateStatus.Ready -> {
            ReleaseNotesScrollSection(
                entries = releaseNoteEntries,
                showVersionSubheaders = releaseNoteEntries.size > 1,
                maxHeightDp = maxNotesHeightDp,
                scrollFocusRequester = notesScrollFocusRequester,
                primaryButtonFocusRequester = primaryButtonFocusRequester,
            )
        }

        UpdateStatus.NoAvailableUpdate -> {
            if (releaseNoteEntries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode == UpdateDialogMode.CheckUpdate) {
                        Text(
                            text = "本版本更新内容：",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                        )
                    }
                    ReleaseNotesScrollSection(
                        entries = releaseNoteEntries,
                        showVersionSubheaders = mode == UpdateDialogMode.ViewChangelog || releaseNoteEntries.size > 1,
                        maxHeightDp = maxNotesHeightDp,
                        scrollFocusRequester = notesScrollFocusRequester,
                        primaryButtonFocusRequester = primaryButtonFocusRequester,
                    )
                }
            } else {
                Text(
                    text = "您当前使用的已是最新版本，无需更新。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = primaryColor,
                )
            }
        }

        UpdateStatus.Downloading -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor,
                    )
                    Text(
                        text = "${bytesSentTotal.toMBString()} / ${contentLength.toMBString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                    )
                }
            }
        }

        UpdateStatus.Installing -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "正在请求电视系统安装新版本，请在弹出的系统窗口中确认安装...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = primaryColor,
                )
                exportedDownloadPath?.let { path ->
                    Text(
                        text =
                            "安装包已同步备份至电视存储「$path」。若系统长时间未弹出安装框，" +
                                "也可稍后打开电视自带【文件管理器 → 下载】直接点击安装。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                    )
                }
            }
        }

        UpdateStatus.InstallError -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val errorMsg =
                    installErrorDetail
                        ?: "电视系统拦截了应用内安装弹窗（常见于部分定制电视固件限制）。"
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyLarge,
                    color = primaryColor,
                )
                settingsFallbackHint?.let { hint ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(BVShape.small)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(12.dp),
                    ) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                exportedDownloadPath?.let { path ->
                    Text(
                        text =
                            "免 U 盘直装指引：新版本安装包已自动导出至电视本地存储「$path」，" +
                                "可直接打开电视自带【文件管理器 / 应用助手 → 下载】一键点击完成升级。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                    )
                }
            }
        }

        UpdateStatus.DownloadError -> {
            Text(
                text = "下载安装包失败${installErrorDetail?.let { "：$it" } ?: "，请检查网络连接后重试。"}",
                style = MaterialTheme.typography.bodyLarge,
                color = primaryColor,
            )
        }

        UpdateStatus.CheckError -> {
            Text(
                text = "连接更新服务器失败，请检查网络连接后重试。",
                style = MaterialTheme.typography.bodyLarge,
                color = primaryColor,
            )
        }
    }
}

@Composable
private fun ReleaseNotesScrollSection(
    entries: List<ReleaseNoteEntry>,
    showVersionSubheaders: Boolean,
    maxHeightDp: Dp,
    scrollFocusRequester: FocusRequester,
    primaryButtonFocusRequester: FocusRequester,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val stepPx = with(density) { 110.dp.roundToPx() }
    var isFocused by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var targetScrollY by remember { mutableIntStateOf(0) }

    val borderStroke =
        if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f))
        }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp, max = maxHeightDp)
                    .clip(BVShape.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                    .border(borderStroke, BVShape.medium)
                    .focusRequester(scrollFocusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .onPreviewKeyEvent { keyEvent ->
                        if (!keyEvent.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            keyEvent.isDpadDown() -> {
                                val currentBase =
                                    if (scrollJob?.isActive == true) targetScrollY else scrollState.value
                                if (currentBase < scrollState.maxValue) {
                                    val target = (currentBase + stepPx).coerceAtMost(scrollState.maxValue)
                                    targetScrollY = target
                                    scrollJob?.cancel()
                                    val isRepeat = keyEvent.nativeKeyEvent.repeatCount > 0
                                    scrollJob =
                                        scope.launch {
                                            if (isRepeat) {
                                                scrollState.scrollTo(target)
                                            } else {
                                                scrollState.animateScrollTo(target)
                                            }
                                        }
                                } else {
                                    runCatching { primaryButtonFocusRequester.requestFocus() }
                                }
                                true
                            }

                            keyEvent.isDpadUp() -> {
                                val currentBase =
                                    if (scrollJob?.isActive == true) targetScrollY else scrollState.value
                                if (currentBase > 0) {
                                    val target = (currentBase - stepPx).coerceAtLeast(0)
                                    targetScrollY = target
                                    scrollJob?.cancel()
                                    val isRepeat = keyEvent.nativeKeyEvent.repeatCount > 0
                                    scrollJob =
                                        scope.launch {
                                            if (isRepeat) {
                                                scrollState.scrollTo(target)
                                            } else {
                                                scrollState.animateScrollTo(target)
                                            }
                                        }
                                }
                                true
                            }

                            else -> false
                        }
                    }.focusable()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.forEachIndexed { entryIndex, entry ->
                    if (showVersionSubheaders) {
                        if (entryIndex > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = entry.versionTag,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            entry.publishedDate?.let { date ->
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                                )
                            }
                        }
                    }

                    entry.blocks.forEach { block ->
                        when (block) {
                            is ReleaseNoteBlock.SectionHeader -> {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            is ReleaseNoteBlock.BulletItem -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = block.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f),
                                    )
                                }
                            }

                            is ReleaseNoteBlock.Paragraph -> {
                                Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (scrollState.maxValue > 0) {
            val scrollHint =
                when {
                    !isFocused -> "▲ 按上方向键可进入更新说明区域滚动查看完整内容"
                    scrollState.value >= scrollState.maxValue -> "已滑动到底部，按下方向键返回底部操作按钮"
                    else -> "▲▼ 使用上下方向键滚动阅读更新说明"
                }
            Text(
                text = scrollHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun UpdateDialogFooter(
    updateStatus: UpdateStatus,
    hasReleaseNotesBox: Boolean,
    primaryButtonFocusRequester: FocusRequester,
    notesScrollFocusRequester: FocusRequester,
    onStartUpdate: () -> Unit,
    onRetryCheck: () -> Unit,
    onRetryLegacyInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val routeUpToNotesModifier =
        if (hasReleaseNotesBox) {
            Modifier.onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isKeyDown() && keyEvent.isDpadUp()) {
                    runCatching { notesScrollFocusRequester.requestFocus() }
                    true
                } else {
                    false
                }
            }
        } else {
            Modifier
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (updateStatus) {
            UpdateStatus.UpdatingInfo -> {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "取消")
                }
            }

            UpdateStatus.Ready -> {
                Button(
                    onClick = onStartUpdate,
                    modifier =
                        Modifier
                            .focusRequester(primaryButtonFocusRequester)
                            .then(routeUpToNotesModifier)
                            .touchClickable(onClick = onStartUpdate),
                ) {
                    Text(text = "立即更新")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .then(routeUpToNotesModifier)
                            .touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "暂不更新")
                }
            }

            UpdateStatus.Downloading -> {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "取消下载")
                }
            }

            UpdateStatus.Installing -> {
                Button(
                    onClick = onRetryLegacyInstall,
                    modifier = Modifier.touchClickable(onClick = onRetryLegacyInstall),
                ) {
                    Text(text = "重新调起安装")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "关闭（安装将继续）")
                }
            }

            UpdateStatus.InstallError -> {
                Button(
                    onClick = onRetryLegacyInstall,
                    modifier =
                        Modifier
                            .focusRequester(primaryButtonFocusRequester)
                            .touchClickable(onClick = onRetryLegacyInstall),
                ) {
                    Text(text = "重试安装")
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.touchClickable(onClick = onOpenSettings),
                ) {
                    Text(text = "去开启安装权限")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "关闭")
                }
            }

            UpdateStatus.DownloadError, UpdateStatus.CheckError -> {
                Button(
                    onClick = onRetryCheck,
                    modifier =
                        Modifier
                            .focusRequester(primaryButtonFocusRequester)
                            .touchClickable(onClick = onRetryCheck),
                ) {
                    Text(text = "重试")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "关闭")
                }
            }

            UpdateStatus.NoAvailableUpdate -> {
                Button(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .focusRequester(primaryButtonFocusRequester)
                            .then(routeUpToNotesModifier)
                            .touchClickable(onClick = onDismiss),
                ) {
                    Text(text = "关闭")
                }
            }
        }
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

private fun UpdateStatus.shouldAutoFocusPrimaryButton(): Boolean =
    when (this) {
        UpdateStatus.Ready,
        UpdateStatus.NoAvailableUpdate,
        UpdateStatus.InstallError,
        UpdateStatus.CheckError,
        UpdateStatus.DownloadError,
        -> true
        UpdateStatus.UpdatingInfo,
        UpdateStatus.Downloading,
        UpdateStatus.Installing,
        -> false
    }

private fun Long.toMBString(): String =
    when {
        this >= 1_000_000 -> "%.1f MB".format(this / 1_000_000.0)
        this >= 1_000 -> "%.1f KB".format(this / 1_000.0)
        else -> "$this B"
    }
