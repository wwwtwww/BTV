package dev.frost819.newbv.data.repository

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
import kotlinx.coroutines.flow.StateFlow

/**
 * 设置数据仓库接口。
 *
 * 封装 [Prefs][dev.frost819.newbv.data.datastore.Prefs] 的所有读写操作，
 * 提供 [StateFlow] 形式的设置变更通知，供 ViewModel 层观察与更新。
 *
 * 设计要点：
 * - 每个设置项暴露只读 [StateFlow]，ViewModel 通过 collect 观察变化。
 * - 写入通过 `updateXxx(value)` 方法，内部委托给 Prefs 属性委托。
 * - 不直接暴露 Prefs 属性，隔离数据层实现细节。
 */
interface SettingsRepository {

    // ===== 音视频设置 =====

    /** 默认画质。 */
    val defaultQuality: StateFlow<Resolution>

    /** 默认视频编码。 */
    val defaultVideoCodec: StateFlow<VideoCodec>

    /** 默认音频编码。 */
    val defaultAudio: StateFlow<Audio>

    /** 默认播放速度。 */
    val defaultPlaySpeed: StateFlow<PlaySpeed>

    /** 播放结束动作。 */
    val actionAfterPlay: StateFlow<ActionAfterPlay>

    /** 防烧屏屏保超时。 */
    val screensaverTimeout: StateFlow<ScreensaverTimeout>

    /** 启用 FFmpeg 音频渲染。 */
    val enableFfmpegAudioRenderer: StateFlow<Boolean>

    /** 启用软件视频解码。 */
    val enableSoftwareVideoDecoder: StateFlow<Boolean>

    /** 显示播放器调试信息。 */
    val showPlayerDebugInfo: StateFlow<Boolean>

    /** 自动选择最优 CDN。 */
    val autoSelectCdn: StateFlow<Boolean>

    // ===== 界面设置 =====

    /** 界面缩放密度。 */
    val density: StateFlow<Float>

    /** 显示视频详情页。 */
    val showVideoInfo: StateFlow<Boolean>

    /** 显示常显进度条。 */
    val showPersistentSeek: StateFlow<Boolean>

    /** 启动页（左侧导航项）。 */
    val homeLeftNavItem: StateFlow<LeftNaviItem>

    /** 首页置顶 Tab。 */
    val firstHomeTopNavItem: StateFlow<HomeTopNavItem>

    /** 首页沉浸式 Hero 看板模式。 */
    val homeHeroMode: StateFlow<HomeHeroMode>

    /** 个人页置顶 Tab。 */
    val firstPersonalTopNavItem: StateFlow<PersonalTopNavItem>

    /** 主题模式。 */
    val themeMode: StateFlow<ThemeMode>

    // ===== 其他设置 =====

    /** 接口类型。 */
    val apiType: StateFlow<ApiType>

    /** 崩溃日志上传开关。 */
    val crashReportEnabled: StateFlow<Boolean>

    // ===== 存储设置 =====

    /** 缓存阈值（MB）。 */
    val cacheThreshold: StateFlow<Int>

    /** 缓存自动清空开关。 */
    val cacheAutoClean: StateFlow<Boolean>

    // ===== 更新方法 =====

    /** 更新默认画质。 */
    fun updateDefaultQuality(value: Resolution)

    /** 更新默认视频编码。 */
    fun updateDefaultVideoCodec(value: VideoCodec)

    /** 更新默认音频编码。 */
    fun updateDefaultAudio(value: Audio)

    /** 更新默认播放速度。 */
    fun updateDefaultPlaySpeed(value: PlaySpeed)

    /** 更新播放结束动作。 */
    fun updateActionAfterPlay(value: ActionAfterPlay)

    /** 更新防烧屏屏保超时。 */
    fun updateScreensaverTimeout(value: ScreensaverTimeout)

    /** 更新 FFmpeg 音频渲染开关。 */
    fun updateEnableFfmpegAudioRenderer(value: Boolean)

    /** 更新软件视频解码开关。 */
    fun updateEnableSoftwareVideoDecoder(value: Boolean)

    /** 更新播放器调试信息开关。 */
    fun updateShowPlayerDebugInfo(value: Boolean)

    /** 更新自动选择 CDN 开关。 */
    fun updateAutoSelectCdn(value: Boolean)

    /** 更新界面缩放密度。 */
    fun updateDensity(value: Float)

    /** 更新显示视频详情开关。 */
    fun updateShowVideoInfo(value: Boolean)

    /** 更新常显进度条开关。 */
    fun updateShowPersistentSeek(value: Boolean)

    /** 更新启动页。 */
    fun updateHomeLeftNavItem(value: LeftNaviItem)

    /** 更新首页置顶 Tab。 */
    fun updateFirstHomeTopNavItem(value: HomeTopNavItem)

    /** 更新首页沉浸看板模式。 */
    fun updateHomeHeroMode(value: HomeHeroMode)

    /** 更新个人页置顶 Tab。 */
    fun updateFirstPersonalTopNavItem(value: PersonalTopNavItem)

    /** 更新主题模式。 */
    fun updateThemeMode(value: ThemeMode)

    /** 更新接口类型。 */
    fun updateApiType(value: ApiType)

    /** 更新崩溃日志上传开关。 */
    fun updateCrashReportEnabled(value: Boolean)

    /** 更新缓存阈值。 */
    fun updateCacheThreshold(value: Int)

    /** 更新缓存自动清空开关。 */
    fun updateCacheAutoClean(value: Boolean)
}
