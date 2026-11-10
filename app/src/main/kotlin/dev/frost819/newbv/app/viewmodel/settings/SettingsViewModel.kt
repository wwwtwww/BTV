package dev.frost819.newbv.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.HomeHeroMode
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.LeftNaviItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.ScreensaverTimeout
import dev.frost819.newbv.data.datastore.ThemeMode
import dev.frost819.newbv.data.datastore.VideoCodec
import dev.frost819.newbv.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 设置页 ViewModel。
 *
 * 统一管理所有设置项的状态，通过 [SettingsRepository] 读写数据。
 * UI 层通过 [uiState] 观察设置变化，调用 `updateXxx()` 方法更新设置。
 *
 * 设计要点：
 * - 使用 [combine] 合并多个 [StateFlow]，任一设置变更时自动触发 UI 刷新。
 * - 写入操作立即生效（通过 Repository 委托给 Prefs），无需额外 save 调用。
 *
 * @param repository 设置数据仓库。
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : ViewModel() {

    /**
     * 设置页 UI 状态。
     *
     * 聚合所有设置项当前值，UI 层通过 collect 观察变化。
     */
    val uiState: StateFlow<SettingsUiState> =
        combine<Any, SettingsUiState>(
            repository.defaultQuality,
            repository.defaultVideoCodec,
            repository.defaultAudio,
            repository.defaultPlaySpeed,
            repository.actionAfterPlay,
            repository.screensaverTimeout,
            repository.enableFfmpegAudioRenderer,
            repository.enableSoftwareVideoDecoder,
            repository.showPlayerDebugInfo,
            repository.autoSelectCdn,
            repository.density,
            repository.showVideoInfo,
            repository.showPersistentSeek,
            repository.homeLeftNavItem,
            repository.firstHomeTopNavItem,
            repository.homeHeroMode,
            repository.firstPersonalTopNavItem,
            repository.themeMode,
            repository.apiType,
            repository.crashReportEnabled,
            repository.cacheThreshold,
            repository.cacheAutoClean,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            SettingsUiState(
                defaultQuality = values[0] as Resolution,
                defaultVideoCodec = values[1] as VideoCodec,
                defaultAudio = values[2] as Audio,
                defaultPlaySpeed = values[3] as PlaySpeed,
                actionAfterPlay = values[4] as ActionAfterPlay,
                screensaverTimeout = values[5] as ScreensaverTimeout,
                enableFfmpegAudioRenderer = values[6] as Boolean,
                enableSoftwareVideoDecoder = values[7] as Boolean,
                showPlayerDebugInfo = values[8] as Boolean,
                autoSelectCdn = values[9] as Boolean,
                density = values[10] as Float,
                showVideoInfo = values[11] as Boolean,
                showPersistentSeek = values[12] as Boolean,
                homeLeftNavItem = values[13] as LeftNaviItem,
                firstHomeTopNavItem = values[14] as HomeTopNavItem,
                homeHeroMode = values[15] as HomeHeroMode,
                firstPersonalTopNavItem = values[16] as PersonalTopNavItem,
                themeMode = values[17] as ThemeMode,
                apiType = values[18] as ApiType,
                crashReportEnabled = values[19] as Boolean,
                cacheThreshold = values[20] as Int,
                cacheAutoClean = values[21] as Boolean,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsUiState(),
        )

    // ===== 音视频设置更新 =====

    /** 更新默认画质。 */
    fun updateDefaultQuality(value: Resolution) = repository.updateDefaultQuality(value)

    /** 更新默认视频编码。 */
    fun updateDefaultVideoCodec(value: VideoCodec) = repository.updateDefaultVideoCodec(value)

    /** 更新默认音频编码。 */
    fun updateDefaultAudio(value: Audio) = repository.updateDefaultAudio(value)

    /** 更新默认播放速度。 */
    fun updateDefaultPlaySpeed(value: PlaySpeed) = repository.updateDefaultPlaySpeed(value)

    /** 更新播放结束动作。 */
    fun updateActionAfterPlay(value: ActionAfterPlay) = repository.updateActionAfterPlay(value)

    /** 更新防烧屏屏保超时。 */
    fun updateScreensaverTimeout(value: ScreensaverTimeout) = repository.updateScreensaverTimeout(value)

    /** 更新 FFmpeg 音频渲染开关。 */
    fun updateEnableFfmpegAudioRenderer(value: Boolean) = repository.updateEnableFfmpegAudioRenderer(value)

    /** 更新软件视频解码开关。 */
    fun updateEnableSoftwareVideoDecoder(value: Boolean) = repository.updateEnableSoftwareVideoDecoder(value)

    /** 更新播放器调试信息开关。 */
    fun updateShowPlayerDebugInfo(value: Boolean) = repository.updateShowPlayerDebugInfo(value)

    /** 更新自动选择 CDN 开关。 */
    fun updateAutoSelectCdn(value: Boolean) = repository.updateAutoSelectCdn(value)

    // ===== 界面设置更新 =====

    /** 更新界面缩放密度。 */
    fun updateDensity(value: Float) = repository.updateDensity(value)

    /** 更新显示视频详情开关。 */
    fun updateShowVideoInfo(value: Boolean) = repository.updateShowVideoInfo(value)

    /** 更新常显进度条开关。 */
    fun updateShowPersistentSeek(value: Boolean) = repository.updateShowPersistentSeek(value)

    /** 更新启动页。 */
    fun updateHomeLeftNavItem(value: LeftNaviItem) = repository.updateHomeLeftNavItem(value)

    /** 更新首页置顶 Tab。 */
    fun updateFirstHomeTopNavItem(value: HomeTopNavItem) = repository.updateFirstHomeTopNavItem(value)

    /** 更新首页沉浸看板模式。 */
    fun updateHomeHeroMode(value: HomeHeroMode) = repository.updateHomeHeroMode(value)

    /** 更新个人页置顶 Tab。 */
    fun updateFirstPersonalTopNavItem(value: PersonalTopNavItem) = repository.updateFirstPersonalTopNavItem(value)

    /** 更新主题模式。 */
    fun updateThemeMode(value: ThemeMode) = repository.updateThemeMode(value)

    // ===== 其他设置更新 =====

    /** 更新接口类型。 */
    fun updateApiType(value: ApiType) = repository.updateApiType(value)

    /** 更新崩溃日志上传开关。 */
    fun updateCrashReportEnabled(value: Boolean) = repository.updateCrashReportEnabled(value)

    // ===== 存储设置更新 =====

    /** 更新缓存阈值。 */
    fun updateCacheThreshold(value: Int) = repository.updateCacheThreshold(value)

    /** 更新缓存自动清空开关。 */
    fun updateCacheAutoClean(value: Boolean) = repository.updateCacheAutoClean(value)
}

/**
 * 设置页 UI 状态数据类。
 *
 * 聚合所有设置项当前值，供 Composable 层 collect 观察。
 * 初始值从 [Prefs][dev.frost819.newbv.data.datastore.Prefs] 当前值获取。
 */
data class SettingsUiState(
    // 音视频设置
    val defaultQuality: Resolution = Resolution.R1080P,
    val defaultVideoCodec: VideoCodec = VideoCodec.AVC,
    val defaultAudio: Audio = Audio.A192K,
    val defaultPlaySpeed: PlaySpeed = PlaySpeed.X1,
    val actionAfterPlay: ActionAfterPlay = ActionAfterPlay.PlayNext,
    val screensaverTimeout: ScreensaverTimeout = ScreensaverTimeout.ThreeMinutes,
    val enableFfmpegAudioRenderer: Boolean = false,
    val enableSoftwareVideoDecoder: Boolean = false,
    val showPlayerDebugInfo: Boolean = false,
    val autoSelectCdn: Boolean = false,
    // 界面设置
    val density: Float = 2f,
    val showVideoInfo: Boolean = false,
    val showPersistentSeek: Boolean = false,
    val homeLeftNavItem: LeftNaviItem = LeftNaviItem.Home,
    val firstHomeTopNavItem: HomeTopNavItem = HomeTopNavItem.Dynamics,
    val homeHeroMode: HomeHeroMode = HomeHeroMode.Auto,
    val firstPersonalTopNavItem: PersonalTopNavItem = PersonalTopNavItem.ToView,
    val themeMode: ThemeMode = ThemeMode.Dark,
    // 其他设置
    val apiType: ApiType = ApiType.Web,
    val crashReportEnabled: Boolean = false,
    // 存储设置
    val cacheThreshold: Int = 0,
    val cacheAutoClean: Boolean = true,
)
