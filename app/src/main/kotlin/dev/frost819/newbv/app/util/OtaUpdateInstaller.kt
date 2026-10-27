package dev.frost819.newbv.app.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/**
 * OTA 安装回调事件。
 */
sealed interface OtaInstallCallbackEvent {
    /**
     * 系统安装器要求启动确认界面（User Action Required）。
     *
     * @param confirmIntent 系统返回的确认安装 [Intent]。
     */
    data class PendingUserAction(
        val confirmIntent: Intent,
    ) : OtaInstallCallbackEvent

    /** 安装成功。 */
    data object Success : OtaInstallCallbackEvent

    /**
     * 安装失败或被系统拦截。
     *
     * @param status [PackageInstaller] 状态码。
     * @param message 错误详情描述。
     */
    data class Failure(
        val status: Int,
        val message: String,
    ) : OtaInstallCallbackEvent
}

/**
 * 接收 [PackageInstaller.Session.commit] 异步结果的 [BroadcastReceiver]。
 *
 * 在 Android TV (尤其是 TCL/雷鸟等定制 ROM) 上，通过 [PackageInstaller] 提交安装会话后，
 * 系统会通过此 Receiver 回调是否需要弹出确认框（[PackageInstaller.STATUS_PENDING_USER_ACTION]）
 * 或是否因缺少权限/ROM 策略被静默拦截（[PackageInstaller.STATUS_FAILURE_BLOCKED] 等），
 * 从而彻底避免传统 `ACTION_VIEW` 无任何回调导致的「安装中」界面假死。
 */
class OtaInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "未知安装错误 (status=$status)"
        OtaUpdateInstaller.onInstallCallback(context, status, statusMessage, intent)
    }
}

/**
 * 应用内 OTA 升级安装与导出管理器。
 *
 * 提供三大核心能力：
 * 1. **双轨安装引擎**：优先支持 [PackageInstaller] Session 流式安装（主动捕获系统拦截状态与用户确认 Intent），
 *    并提供传统 [FileProvider] + [Intent.ACTION_VIEW] 跨进程显式 URI 授权兜底；
 * 2. **公共 Download 目录自动同步导出**：通过 [MediaStore.Downloads]（Android 10+）或公共下载目录同步保存一份 APK 副本，
 *    当电视 ROM 锁死应用内自升级时，用户可直接打开电视自带「文件管理器 -> 下载」免 U 盘一键安装；
 * 3. **未知来源权限检测与安全跳转**：检测 [PackageManager.canRequestPackageInstalls]，并安全引导跳转系统授权页。
 */
object OtaUpdateInstaller {
    private val logger = Loggers.get("OtaUpdateInstaller")
    private const val INSTALL_REQUEST_CODE = 10480
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    // 使用 replay = 1 防止系统广播早于 UI 订阅到达时丢失事件。
    // 【调用顺序约束】：UI 订阅者必须在每次开启新安装会话或进入监听作用域前（即 collect 之前）
    // 显式调用 clearInstallEvents() 清空 replay 缓存，否则会立即重放上一次遗留的陈旧安装结果事件。
    private val _installEvents = MutableSharedFlow<OtaInstallCallbackEvent>(replay = 1, extraBufferCapacity = 8)

    /**
     * 暴露给 UI 层的安装状态回调事件流。
     *
     * **注意调用顺序约束**：由于底层配置了 `replay = 1` 以避免极端竞态下丢失广播回调，
     * 订阅方在开始 `collect` 之前**必须**先调用 [clearInstallEvents]，防止收到上一次会话遗留的陈旧事件。
     */
    val installEvents: SharedFlow<OtaInstallCallbackEvent> = _installEvents.asSharedFlow()

    /**
     * 清空缓存的历史安装回调事件，防止新打开弹窗时重放旧状态。
     */
    fun clearInstallEvents() {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        _installEvents.resetReplayCache()
    }

    /**
     * 处理来自 [OtaInstallResultReceiver] 的系统安装器广播回调。
     */
    internal fun onInstallCallback(
        context: Context,
        status: Int,
        statusMessage: String,
        rawIntent: Intent,
    ) {
        logger.info { "PackageInstaller callback: status=$status, message=$statusMessage" }
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        rawIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        rawIntent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching {
                        context.startActivity(confirmIntent)
                        _installEvents.tryEmit(OtaInstallCallbackEvent.PendingUserAction(confirmIntent))
                    }.onFailure { error ->
                        logger.error(error) { "Failed to start PackageInstaller confirmation activity" }
                        _installEvents.tryEmit(
                            OtaInstallCallbackEvent.Failure(
                                status = status,
                                message = "无法拉起系统安装确认窗口: ${error.message}",
                            ),
                        )
                    }
                } else {
                    _installEvents.tryEmit(
                        OtaInstallCallbackEvent.Failure(
                            status = status,
                            message = "系统未返回安装确认窗口 Intent",
                        ),
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                _installEvents.tryEmit(OtaInstallCallbackEvent.Success)
            }

            else -> {
                _installEvents.tryEmit(
                    OtaInstallCallbackEvent.Failure(
                        status = status,
                        message = statusMessage,
                    ),
                )
            }
        }
    }

    /**
     * 检查当前应用是否已被授予「安装未知应用」权限。
     *
     * @param context 上下文。
     * @return Android 8.0+ 返回 [PackageManager.canRequestPackageInstalls]，低版本默认返回 `true`。
     */
    fun canRequestPackageInstalls(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean =
        if (sdkInt >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(true)
        } else {
            true
        }

    /**
     * 生成跳转未知来源权限或安全设置页的候选动作列表（按优先级从高到低）。
     */
    internal fun buildSettingsIntentActions(
        packageName: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): List<Pair<String, String?>> =
        buildList {
            if (sdkInt >= Build.VERSION_CODES.O) {
                add(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to "package:$packageName")
                add(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to null)
            }
            add(Settings.ACTION_SECURITY_SETTINGS to null)
            add(Settings.ACTION_SETTINGS to null)
        }

    /**
     * 尝试打开系统「允许安装未知应用」设置页面。
     *
     * 针对部分裁剪了指定包名跳转页的国产 TV ROM，会自动降级到通用安全设置或主设置页。
     *
     * @param context 上下文。
     * @return 是否成功调起设置页面。
     */
    fun openUnknownSourceSettings(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
        intentLauncher: (String, String?) -> Unit = { action, dataUri ->
            val intent =
                Intent(action).apply {
                    if (dataUri != null) {
                        data = Uri.parse(dataUri)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        },
    ): Boolean {
        val candidates = buildSettingsIntentActions(context.packageName, sdkInt)
        for ((action, dataUri) in candidates) {
            if (runCatching { intentLauncher(action, dataUri) }.isSuccess) {
                return true
            }
        }
        return false
    }

    /**
     * 将已下载的 APK 同步导出一份至系统公共 `Download` 目录。
     *
     * 这样即使电视系统完全禁止第三方应用内自升级（如雷鸟/TCL 部分固件），
     * 用户也可直接打开电视自带的「文件管理器 / 应用助手 -> 下载（Download）」目录免 U 盘直接点击安装。
     *
     * @param context 上下文。
     * @param sourceApk 内部缓存中的完整 APK 文件。
     * @return 成功导出后的可读路径描述（如 `/sdcard/Download/xxx.apk`），失败返回 `null`。
     */
    fun exportApkToPublicDownloads(
        context: Context,
        sourceApk: File,
    ): String? {
        if (!sourceApk.exists() || sourceApk.length() <= 0L) return null
        val fileName = sourceApk.name

        // 优先在 Android 10 (API 29)+ 使用 MediaStore.Downloads 免权限写入公共 Download 目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                // 先清理同名旧记录，避免生成 (1).apk 冗余文件
                runCatching {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(fileName),
                    )
                }

                val values =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME_TYPE)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { output ->
                        sourceApk.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: error("无法打开 MediaStore 输出流")

                    val completeValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                    resolver.update(itemUri, completeValues, null, null)
                    logger.info { "Exported APK to MediaStore Downloads: $fileName" }
                    return "Download/$fileName"
                }
            }.onFailure { error ->
                logger.warn(error) { "MediaStore export failed, falling back to direct File API" }
            }
        }

        // Fallback: 直接写入标准公共 Download 目录或外部存储应用 Download 目录
        return runCatching {
            @Suppress("DEPRECATION")
            val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloadDir.exists()) publicDownloadDir.mkdirs()
            val targetFile = File(publicDownloadDir, fileName)
            sourceApk.inputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.setReadable(true, false)
            logger.info { "Exported APK to public Download directory: ${targetFile.absolutePath}" }
            "Download/$fileName"
        }.recoverCatching {
            val externalAppDownloadDir =
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: error("外部存储目录不可用")
            if (!externalAppDownloadDir.exists()) externalAppDownloadDir.mkdirs()
            val targetFile = File(externalAppDownloadDir, fileName)
            sourceApk.inputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.setReadable(true, false)
            targetFile.absolutePath
        }.getOrNull()
    }

    /**
     * 使用 [PackageInstaller] Session 流式安装 APK。
     *
     * 直接向系统 PackageManager 管道写入 APK 字节流，规避跨进程 FileProvider URI 权限丢失问题，
     * 并通过 [OtaInstallResultReceiver] 获取精准的系统状态回调。
     *
     * @param context 上下文。
     * @param apkFile 待安装的 APK 文件。
     * @return 会话提交是否成功发起。
     */
    fun installViaPackageInstallerSession(
        context: Context,
        apkFile: File,
    ): Result<Unit> =
        runCatching {
            require(apkFile.exists() && apkFile.length() > 0L) { "安装包文件不存在或为空: ${apkFile.absolutePath}" }
            val packageInstaller = context.packageManager.packageInstaller
            val params =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    setSize(apkFile.length())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                    }
                }

            val sessionId = packageInstaller.createSession(params)
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite("btv_ota_update.apk", 0, apkFile.length()).use { output ->
                    apkFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                    session.fsync(output)
                }

                val callbackIntent =
                    Intent(context, OtaInstallResultReceiver::class.java).apply {
                        setPackage(context.packageName)
                    }
                val pendingIntentFlags =
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.FLAG_MUTABLE
                        } else {
                            0
                        }
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        INSTALL_REQUEST_CODE,
                        callbackIntent,
                        pendingIntentFlags,
                    )
                session.commit(pendingIntent.intentSender)
                logger.info { "Committed PackageInstaller sessionId=$sessionId for ${apkFile.name}" }
            }
        }

    /**
     * 使用传统 `Intent.ACTION_VIEW` + [FileProvider] 调起系统安装器（兼容模式兜底）。
     *
     * 会显式遍历所有匹配的安装器 Activity 并逐一授予 `FLAG_GRANT_READ_URI_PERMISSION`，
     * 防止定制 TV ROM 因未传递 ClipData 权限而静默退出。
     *
     * @param context 上下文。
     * @param apkFile 待安装的 APK 文件。
     */
    fun installViaLegacyViewIntent(
        context: Context,
        apkFile: File,
    ): Result<Unit> =
        runCatching {
            require(apkFile.exists() && apkFile.length() > 0L) { "安装包文件不存在或为空: ${apkFile.absolutePath}" }
            apkFile.setReadable(true, false)
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    apkFile,
                )
            logger.info { "Launching legacy ACTION_VIEW installer with URI: $uri" }
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(uri, APK_MIME_TYPE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

            // 显式向所有匹配的包安装器组件授权 URI 读取权限
            runCatching {
                val resolveInfos =
                    context.packageManager.queryIntentActivities(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY,
                    )
                for (resolveInfo in resolveInfos) {
                    context.grantUriPermission(
                        resolveInfo.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }

            context.startActivity(intent)
        }
}
