package dev.frost819.newbv.data.repository

import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.HomeHeroMode
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.LeftNaviItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.PrefKeys
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.ScreensaverTimeout
import dev.frost819.newbv.data.datastore.ThemeMode
import dev.frost819.newbv.data.datastore.VideoCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * [SettingsRepository] 的默认实现。
 *
 * 内部委托给 [Prefs] 单例进行读写，通过 [Prefs.flowOf] 获取原始 [MutableStateFlow][kotlinx.coroutines.flow.MutableStateFlow]，
 * 经类型映射后暴露为业务层类型的 [StateFlow]。
 *
 * 所有写入操作直接调用 Prefs 属性赋值，由 [Prefs] 内部保证异步持久化。
 *
 * @see Prefs
 * @see SettingsRepository
 */
class SettingsRepositoryImpl : SettingsRepository {
    /** 仓库内部使用的协程作用域，用于 stateIn 转换。 */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ===== 音视频设置 =====

    override val defaultQuality: StateFlow<Resolution> by lazy {
        intEnumFlow(PrefKeys.defaultQuality, Resolution.entries, Resolution::fromCode, Prefs.defaultQuality)
    }

    override val defaultVideoCodec: StateFlow<VideoCodec> by lazy {
        intEnumFlow(PrefKeys.defaultVideoCodec, VideoCodec.entries, VideoCodec::fromCode, Prefs.defaultVideoCodec)
    }

    override val defaultAudio: StateFlow<Audio> by lazy {
        intEnumFlow(PrefKeys.defaultAudio, Audio.entries, Audio::fromCode, Prefs.defaultAudio)
    }

    override val defaultPlaySpeed: StateFlow<PlaySpeed> by lazy {
        intEnumFlow(PrefKeys.defaultPlaySpeed, PlaySpeed.entries, PlaySpeed::fromCode, Prefs.defaultPlaySpeed)
    }

    override val actionAfterPlay: StateFlow<ActionAfterPlay> by lazy {
        intEnumFlow(PrefKeys.actionAfterPlay, ActionAfterPlay.entries, ActionAfterPlay::fromCode, Prefs.actionAfterPlay)
    }

    override val screensaverTimeout: StateFlow<ScreensaverTimeout> by lazy {
        intEnumFlow(
            PrefKeys.screensaverTimeout,
            ScreensaverTimeout.entries,
            ScreensaverTimeout::fromCode,
            Prefs.screensaverTimeout,
        )
    }

    override val enableFfmpegAudioRenderer: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.enableFfmpegAudioRenderer, Prefs.enableFfmpegAudioRenderer)
    }

    override val enableSoftwareVideoDecoder: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.enableSoftwareVideoDecoder, Prefs.enableSoftwareVideoDecoder)
    }

    override val showPlayerDebugInfo: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.showPlayerDebugInfo, Prefs.showPlayerDebugInfo)
    }

    override val autoSelectCdn: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.autoSelectCdn, Prefs.autoSelectCdn)
    }

    // ===== 界面设置 =====

    override val density: StateFlow<Float> by lazy {
        floatFlow(PrefKeys.density, Prefs.density)
    }

    override val showVideoInfo: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.showVideoInfo, Prefs.showVideoInfo)
    }

    override val showPersistentSeek: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.showPersistentSeek, Prefs.showPersistentSeek)
    }

    override val homeLeftNavItem: StateFlow<LeftNaviItem> by lazy {
        intEnumFlow(PrefKeys.homeLeftNavItem, LeftNaviItem.entries, LeftNaviItem::fromOrdinal, Prefs.homeLeftNavItem)
    }

    override val firstHomeTopNavItem: StateFlow<HomeTopNavItem> by lazy {
        intEnumFlow(
            PrefKeys.firstHomeTopNavItem,
            HomeTopNavItem.entries,
            HomeTopNavItem::fromCode,
            Prefs.firstHomeTopNavItem,
        )
    }

    override val homeHeroMode: StateFlow<HomeHeroMode> by lazy {
        intEnumFlow(PrefKeys.homeHeroMode, HomeHeroMode.entries, HomeHeroMode::fromOrdinal, Prefs.homeHeroMode)
    }

    override val firstPersonalTopNavItem: StateFlow<PersonalTopNavItem> by lazy {
        intEnumFlow(
            PrefKeys.firstPersonalTopNavItem,
            PersonalTopNavItem.entries,
            PersonalTopNavItem::fromOrdinal,
            Prefs.firstPersonalTopNavItem,
        )
    }

    override val themeMode: StateFlow<ThemeMode> by lazy {
        intEnumFlow(PrefKeys.themeMode, ThemeMode.entries, ThemeMode::fromOrdinal, Prefs.themeMode)
    }

    // ===== 其他设置 =====

    override val apiType: StateFlow<ApiType> by lazy {
        intEnumFlow(PrefKeys.apiType, ApiType.entries, ApiType::fromOrdinal, Prefs.apiType)
    }

    override val crashReportEnabled: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.crashReportEnabled, Prefs.crashReportEnabled)
    }

    // ===== 存储设置 =====

    override val cacheThreshold: StateFlow<Int> by lazy {
        intFlow(PrefKeys.cacheThreshold, Prefs.cacheThreshold)
    }

    override val cacheAutoClean: StateFlow<Boolean> by lazy {
        booleanFlow(PrefKeys.cacheAutoClean, Prefs.cacheAutoClean)
    }

    // ===== 更新方法 =====

    override fun updateDefaultQuality(value: Resolution) {
        Prefs.defaultQuality = value
    }

    override fun updateDefaultVideoCodec(value: VideoCodec) {
        Prefs.defaultVideoCodec = value
    }

    override fun updateDefaultAudio(value: Audio) {
        Prefs.defaultAudio = value
    }

    override fun updateDefaultPlaySpeed(value: PlaySpeed) {
        Prefs.defaultPlaySpeed = value
    }

    override fun updateActionAfterPlay(value: ActionAfterPlay) {
        Prefs.actionAfterPlay = value
    }

    override fun updateScreensaverTimeout(value: ScreensaverTimeout) {
        Prefs.screensaverTimeout = value
    }

    override fun updateEnableFfmpegAudioRenderer(value: Boolean) {
        Prefs.enableFfmpegAudioRenderer = value
    }

    override fun updateEnableSoftwareVideoDecoder(value: Boolean) {
        Prefs.enableSoftwareVideoDecoder = value
    }

    override fun updateShowPlayerDebugInfo(value: Boolean) {
        Prefs.showPlayerDebugInfo = value
    }

    override fun updateAutoSelectCdn(value: Boolean) {
        Prefs.autoSelectCdn = value
    }

    override fun updateDensity(value: Float) {
        Prefs.density = value
    }

    override fun updateShowVideoInfo(value: Boolean) {
        Prefs.showVideoInfo = value
    }

    override fun updateShowPersistentSeek(value: Boolean) {
        Prefs.showPersistentSeek = value
    }

    override fun updateHomeLeftNavItem(value: LeftNaviItem) {
        Prefs.homeLeftNavItem = value
    }

    override fun updateFirstHomeTopNavItem(value: HomeTopNavItem) {
        Prefs.firstHomeTopNavItem = value
    }

    override fun updateHomeHeroMode(value: HomeHeroMode) {
        Prefs.homeHeroMode = value
    }

    override fun updateFirstPersonalTopNavItem(value: PersonalTopNavItem) {
        Prefs.firstPersonalTopNavItem = value
    }

    override fun updateThemeMode(value: ThemeMode) {
        Prefs.themeMode = value
    }

    override fun updateApiType(value: ApiType) {
        Prefs.apiType = value
    }

    override fun updateCrashReportEnabled(value: Boolean) {
        Prefs.crashReportEnabled = value
    }

    override fun updateCacheThreshold(value: Int) {
        Prefs.cacheThreshold = value
    }

    override fun updateCacheAutoClean(value: Boolean) {
        Prefs.cacheAutoClean = value
    }

    // ===== 内部辅助方法 =====

    /**
     * 从 Prefs.flowOf 获取 Int 类型原始流，映射为枚举 StateFlow。
     *
     * @param key DataStore 键。
     * @param entries 枚举值列表。
     * @param fromInt 整数转枚举的映射函数。
     * @param initialValue 初始值（从 Prefs 当前值获取）。
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Enum<T>> intEnumFlow(
        key: androidx.datastore.preferences.core.Preferences.Key<Int>,
        entries: List<T>,
        fromInt: (Int) -> T,
        initialValue: T,
    ): StateFlow<T> {
        val rawFlow = Prefs.flowOf(key) ?: return kotlinx.coroutines.flow.MutableStateFlow(initialValue)
        return rawFlow
            .map { value ->
                val intValue = value as? Int ?: return@map initialValue
                runCatching { entries[intValue] }.getOrDefault(initialValue)
            }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialValue)
    }

    /** 从 Prefs.flowOf 获取 Boolean 类型 StateFlow。 */
    @Suppress("UNCHECKED_CAST")
    private fun booleanFlow(
        key: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
        initialValue: Boolean,
    ): StateFlow<Boolean> {
        val rawFlow = Prefs.flowOf(key) ?: return kotlinx.coroutines.flow.MutableStateFlow(initialValue)
        return rawFlow
            .map { (it as? Boolean) ?: initialValue }
            .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialValue)
    }

    /** 从 Prefs.flowOf 获取 Int 类型 StateFlow。 */
    @Suppress("UNCHECKED_CAST")
    private fun intFlow(
        key: androidx.datastore.preferences.core.Preferences.Key<Int>,
        initialValue: Int,
    ): StateFlow<Int> {
        val rawFlow = Prefs.flowOf(key) ?: return kotlinx.coroutines.flow.MutableStateFlow(initialValue)
        return rawFlow
            .map { (it as? Int) ?: initialValue }
            .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialValue)
    }

    /** 从 Prefs.flowOf 获取 Float 类型 StateFlow。 */
    @Suppress("UNCHECKED_CAST")
    private fun floatFlow(
        key: androidx.datastore.preferences.core.Preferences.Key<Float>,
        initialValue: Float,
    ): StateFlow<Float> {
        val rawFlow = Prefs.flowOf(key) ?: return kotlinx.coroutines.flow.MutableStateFlow(initialValue)
        return rawFlow
            .map { (it as? Float) ?: initialValue }
            .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialValue)
    }
}
