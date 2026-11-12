package dev.frost819.newbv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertWithMessage
import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.HomeHeroMode
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.LeftNaviItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.ScreensaverTimeout
import dev.frost819.newbv.data.datastore.ThemeMode
import dev.frost819.newbv.data.datastore.VideoCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * 枚举偏好持久化往返一致性回归测试。
 *
 * 背景 bug：`SettingsRepositoryImpl.intEnumFlow` 曾经忽略传入的 `fromInt` 参数，
 * 改用「按 entries 下标位置查找」解码。对以 code 持久化的枚举（如 [Resolution] 的
 * `R1080P60.code = 116`、[Audio] 的 `A64K.code = 30216`）来说，code 远大于
 * `entries.size`，位置查找必然越界并被 `runCatching` 静默吞掉，导致对应
 * [StateFlow] 永远停留在默认值（设置项改了不生效）。
 *
 * 本测试分两层防护：
 * 1. **纯函数层**：逐个枚举验证「写入值（code / ordinal）→ 对应 `fromCode` /
 *    `fromOrdinal` 解码 → 原枚举」往返一致，并断言 code 型枚举的 code 确实落在
 *    `entries` 下标范围之外（位置查找不可行的根因），以及非法值安全兜底。
 * 2. **集成层**：用真实 DataStore 初始化 [Prefs]，通过 [SettingsRepositoryImpl]
 *    的公开 `update*` 写入每个枚举值，断言其 `intEnumFlow` 产出的 StateFlow
 *    确实跟随变更（直接覆盖被修复的那段逻辑）。
 *
 * 说明：枚举定义见 `PrefEnums.kt`；`intEnumFlow` 是 private 方法，只能经由
 * 仓库的公开属性间接验证。
 *
 * @see SettingsRepositoryImpl
 * @see Prefs
 */
class SettingsEnumRoundTripTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir =
            kotlin.io.path
                .createTempDirectory(prefix = "enum_round_trip")
                .toFile()
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { File(tempDir, "EnumRoundTrip.preferences_pb") },
            )
        Prefs.resetForTesting()
        Prefs.init(dataStore)
    }

    @AfterEach
    fun tearDown() {
        Prefs.resetForTesting()
        tempDir.deleteRecursively()
    }

    // ===== 第 1 层：code / ordinal 纯函数往返 =====

    @Test
    fun `Resolution round trips through fromCode`() {
        assertRoundTrip("Resolution", Resolution.entries.toTypedArray(), Resolution::code, Resolution::fromCode)
    }

    @Test
    fun `Audio round trips through fromCode`() {
        assertRoundTrip("Audio", Audio.entries.toTypedArray(), Audio::code, Audio::fromCode)
    }

    @Test
    fun `PlaySpeed round trips through fromCode`() {
        assertRoundTrip("PlaySpeed", PlaySpeed.entries.toTypedArray(), PlaySpeed::code, PlaySpeed::fromCode)
    }

    @Test
    fun `ActionAfterPlay round trips through fromCode`() {
        assertRoundTrip(
            "ActionAfterPlay",
            ActionAfterPlay.entries.toTypedArray(),
            ActionAfterPlay::code,
            ActionAfterPlay::fromCode,
        )
    }

    @Test
    fun `ScreensaverTimeout round trips through fromCode`() {
        assertRoundTrip(
            "ScreensaverTimeout",
            ScreensaverTimeout.entries.toTypedArray(),
            ScreensaverTimeout::code,
            ScreensaverTimeout::fromCode,
        )
    }

    @Test
    fun `HomeTopNavItem round trips through fromCode`() {
        assertRoundTrip(
            "HomeTopNavItem",
            HomeTopNavItem.entries.toTypedArray(),
            HomeTopNavItem::code,
            HomeTopNavItem::fromCode,
        )
    }

    @Test
    fun `PersonalTopNavItem round trips through fromOrdinal`() {
        assertRoundTrip(
            "PersonalTopNavItem",
            PersonalTopNavItem.entries.toTypedArray(),
            { it.ordinal },
            PersonalTopNavItem::fromOrdinal,
        )
    }

    @Test
    fun `VideoCodec round trips through fromOrdinal`() {
        assertRoundTrip("VideoCodec", VideoCodec.entries.toTypedArray(), { it.ordinal }, VideoCodec::fromOrdinal)
    }

    @Test
    fun `ApiType round trips through fromOrdinal`() {
        assertRoundTrip("ApiType", ApiType.entries.toTypedArray(), { it.ordinal }, ApiType::fromOrdinal)
    }

    @Test
    fun `ThemeMode round trips through fromOrdinal`() {
        assertRoundTrip("ThemeMode", ThemeMode.entries.toTypedArray(), { it.ordinal }, ThemeMode::fromOrdinal)
    }

    @Test
    fun `HomeHeroMode round trips through fromOrdinal`() {
        assertRoundTrip("HomeHeroMode", HomeHeroMode.entries.toTypedArray(), { it.ordinal }, HomeHeroMode::fromOrdinal)
    }

    @Test
    fun `LeftNaviItem round trips through fromOrdinal`() {
        assertRoundTrip("LeftNaviItem", LeftNaviItem.entries.toTypedArray(), { it.ordinal }, LeftNaviItem::fromOrdinal)
    }

    // ===== code 越界断言：位置查找为什么不可行 =====

    @Test
    fun `Resolution high codes exceed entries size`() {
        // 修复前的 intEnumFlow 若按 entries 下标查找，以下 code 全部越界（size = 12）。
        assertWithMessage("Resolution.entries.size 应为 12")
            .that(Resolution.entries.size)
            .isEqualTo(12)

        val outOfBoundsCodes =
            listOf(
                Resolution.R1080PPlus to 112,
                Resolution.R1080P60 to 116,
                Resolution.R4K to 120,
                Resolution.RHdr to 125,
                Resolution.RDolby to 126,
                Resolution.R8K to 127,
            )
        for ((expected, code) in outOfBoundsCodes) {
            assertWithMessage("code $code 应超出 entries.size(${Resolution.entries.size})，只能靠 fromCode 解析")
                .that(code)
                .isGreaterThan(Resolution.entries.size)
            assertWithMessage("fromCode($code) 应解析回 $expected")
                .that(Resolution.fromCode(code))
                .isEqualTo(expected)
        }

        // R1080P60 的 code 与其 ordinal 不同，二者不可混用（防止再次误用位置查找）。
        assertWithMessage("R1080P60 的 code(116) 与 ordinal 必须不同")
            .that(Resolution.R1080P60.code)
            .isNotEqualTo(Resolution.R1080P60.ordinal)
    }

    @Test
    fun `Audio codes are far larger than entries size`() {
        assertWithMessage("Audio 的所有 code 均远大于 entries.size，位置查找必然越界")
            .that(Audio.entries.map { it.code }.all { it > Audio.entries.size })
            .isTrue()
        assertWithMessage("fromCode(30216) 应解析回 A64K")
            .that(Audio.fromCode(30216))
            .isEqualTo(Audio.A64K)
    }

    @Test
    fun `code based enums do not rely on ordinal equality`() {
        // 以 code 持久化的枚举中，只要存在 code != ordinal 的项，位置查找就会产生
        // 「值正确但枚举错误」的静默错位，比越界更难发现。此处固化该前提。
        val misaligned = Resolution.entries.filter { it.code != it.ordinal }
        assertWithMessage("Resolution 应存在 code 与 ordinal 不一致的项")
            .that(misaligned)
            .isNotEmpty()

        val audioMisaligned = Audio.entries.filter { it.code != it.ordinal }
        assertWithMessage("Audio 应存在 code 与 ordinal 不一致的项")
            .that(audioMisaligned)
            .isNotEmpty()
    }

    // ===== 非法值兜底 =====

    @Test
    fun `fromCode functions fall back to default on illegal values`() {
        assertFallback("Resolution.fromCode(999)", { Resolution.fromCode(it) }, Resolution.R1080P)
        assertFallback("Audio.fromCode(999)", { Audio.fromCode(it) }, Audio.A192K)
        assertFallback("PlaySpeed.fromCode(999)", { PlaySpeed.fromCode(it) }, PlaySpeed.X1)
        assertFallback("ActionAfterPlay.fromCode(999)", { ActionAfterPlay.fromCode(it) }, ActionAfterPlay.PlayNext)
        assertFallback(
            "ScreensaverTimeout.fromCode(999)",
            { ScreensaverTimeout.fromCode(it) },
            ScreensaverTimeout.ThreeMinutes,
        )
        assertFallback("HomeTopNavItem.fromCode(999)", { HomeTopNavItem.fromCode(it) }, HomeTopNavItem.Dynamics)
    }

    @Test
    fun `fromOrdinal functions fall back to default on illegal values`() {
        assertFallback("ApiType.fromOrdinal(999)", { ApiType.fromOrdinal(it) }, ApiType.Web)
        assertFallback("VideoCodec.fromOrdinal(999)", { VideoCodec.fromOrdinal(it) }, VideoCodec.AVC)
        assertFallback("ThemeMode.fromOrdinal(999)", { ThemeMode.fromOrdinal(it) }, ThemeMode.FollowSystem)
        assertFallback("HomeHeroMode.fromOrdinal(999)", { HomeHeroMode.fromOrdinal(it) }, HomeHeroMode.Auto)
        assertFallback("LeftNaviItem.fromOrdinal(999)", { LeftNaviItem.fromOrdinal(it) }, LeftNaviItem.Home)
        assertFallback(
            "PersonalTopNavItem.fromOrdinal(999)",
            { PersonalTopNavItem.fromOrdinal(it) },
            PersonalTopNavItem.ToView,
        )
    }

    // ===== 第 2 层：Prefs 写入 → intEnumFlow 读回 =====

    @Test
    fun `settings repository enum flows follow every value written through Prefs`() {
        // Unconfined 让 stateIn 的映射在当前线程同步执行，写入后立即可断言。
        val repository = SettingsRepositoryImpl(Dispatchers.Unconfined)

        val cases: List<RoundTripCase> = repositoryCases(repository)
        for (case in cases) {
            val flow = case.flow
            for (value in case.values) {
                // 写入必须经过 Prefs 委托（与生产路径一致），持久化值是 code/ordinal。
                case.update(value)
                assertWithMessage("${case.label} 写入 $value 后 StateFlow 未同步")
                    .that(awaitValue(flow, value))
                    .isEqualTo(value)
            }
        }
    }

    @Test
    fun `settings repository enum flows decode persisted high codes`() {
        // 专项回归：高 code 值（116 / 30216）必须被解码为对应枚举，而不是回落默认值。
        val repository = SettingsRepositoryImpl(Dispatchers.Unconfined)

        repository.updateDefaultQuality(Resolution.R1080P60)
        assertThatValue("defaultQuality(R1080P60/code=116)", repository.defaultQuality, Resolution.R1080P60)

        repository.updateDefaultAudio(Audio.A64K)
        assertThatValue("defaultAudio(A64K/code=30216)", repository.defaultAudio, Audio.A64K)

        repository.updateDefaultQuality(Resolution.R8K)
        assertThatValue("defaultQuality(R8K/code=127)", repository.defaultQuality, Resolution.R8K)

        repository.updateDefaultVideoCodec(VideoCodec.AV1)
        assertThatValue("defaultVideoCodec(AV1/ordinal=2)", repository.defaultVideoCodec, VideoCodec.AV1)

        repository.updateHomeHeroMode(HomeHeroMode.Off)
        assertThatValue("homeHeroMode(Off/ordinal=3)", repository.homeHeroMode, HomeHeroMode.Off)
    }

    @Test
    fun `intEnumFlow initial value is restored from persisted code`() {
        // 先写入高 code，再新建仓库实例：验证首帧（initialValue 来自 Prefs 当前值）也正确。
        Prefs.defaultQuality = Resolution.R1080P60

        val repository = SettingsRepositoryImpl(Dispatchers.Unconfined)
        assertThatValue("defaultQuality 首帧解码", repository.defaultQuality, Resolution.R1080P60)
    }

    // ===== 辅助 =====

    /**
     * 一个「仓库枚举属性」的往返用例：可选值列表 + StateFlow + 写入函数。
     *
     * @param label 用例名（断言消息用）。
     * @param values 需要逐个写入的枚举值。
     * @param flow 仓库暴露的 StateFlow（[StateFlow] 协变，可直接作为 `StateFlow<Any?>`）。
     * @param update 写入函数，内部调用仓库的 `update*`。
     */
    private class RoundTripCase(
        val label: String,
        val values: List<Any?>,
        val flow: StateFlow<Any?>,
        val update: (Any?) -> Unit,
    )

    /** 构造全部枚举仓库属性用例。 */
    private fun repositoryCases(repository: SettingsRepositoryImpl): List<RoundTripCase> =
        listOf(
            RoundTripCase("defaultQuality", Resolution.entries.toList(), repository.defaultQuality) {
                repository.updateDefaultQuality(it as Resolution)
            },
            RoundTripCase("defaultVideoCodec", VideoCodec.entries.toList(), repository.defaultVideoCodec) {
                repository.updateDefaultVideoCodec(it as VideoCodec)
            },
            RoundTripCase("defaultAudio", Audio.entries.toList(), repository.defaultAudio) {
                repository.updateDefaultAudio(it as Audio)
            },
            RoundTripCase("defaultPlaySpeed", PlaySpeed.entries.toList(), repository.defaultPlaySpeed) {
                repository.updateDefaultPlaySpeed(it as PlaySpeed)
            },
            RoundTripCase("actionAfterPlay", ActionAfterPlay.entries.toList(), repository.actionAfterPlay) {
                repository.updateActionAfterPlay(it as ActionAfterPlay)
            },
            RoundTripCase("screensaverTimeout", ScreensaverTimeout.entries.toList(), repository.screensaverTimeout) {
                repository.updateScreensaverTimeout(it as ScreensaverTimeout)
            },
            RoundTripCase("homeLeftNavItem", LeftNaviItem.entries.toList(), repository.homeLeftNavItem) {
                repository.updateHomeLeftNavItem(it as LeftNaviItem)
            },
            RoundTripCase("firstHomeTopNavItem", HomeTopNavItem.entries.toList(), repository.firstHomeTopNavItem) {
                repository.updateFirstHomeTopNavItem(it as HomeTopNavItem)
            },
            RoundTripCase("homeHeroMode", HomeHeroMode.entries.toList(), repository.homeHeroMode) {
                repository.updateHomeHeroMode(it as HomeHeroMode)
            },
            RoundTripCase(
                "firstPersonalTopNavItem",
                PersonalTopNavItem.entries.toList(),
                repository.firstPersonalTopNavItem,
            ) {
                repository.updateFirstPersonalTopNavItem(it as PersonalTopNavItem)
            },
            RoundTripCase("themeMode", ThemeMode.entries.toList(), repository.themeMode) {
                repository.updateThemeMode(it as ThemeMode)
            },
            RoundTripCase("apiType", ApiType.entries.toList(), repository.apiType) {
                repository.updateApiType(it as ApiType)
            },
        )

    /**
     * 断言「枚举 → 持久化 Int → fromInt → 枚举」往返一致。
     *
     * 同时校验持久化映射是单射（两个枚举项共用一个 Int 会让往返无法还原）。
     */
    private fun <T : Enum<T>> assertRoundTrip(
        label: String,
        values: Array<T>,
        save: (T) -> Int,
        fromInt: (Int) -> T,
    ) {
        assertWithMessage("$label 的持久化值必须一一对应（单射）")
            .that(values.map(save).distinct())
            .hasSize(values.size)

        for (value in values) {
            val persisted = save(value)
            assertWithMessage("$label: $value 写入 $persisted 后应能解码回自身")
                .that(fromInt(persisted))
                .isEqualTo(value)
        }
    }

    /** 断言解码函数对非法输入（999 / -1 / 极值）返回默认值且不抛异常。 */
    private fun <T : Enum<T>> assertFallback(
        label: String,
        fromInt: (Int) -> T,
        expected: T,
    ) {
        for (illegal in listOf(999, -1, Int.MAX_VALUE, Int.MIN_VALUE)) {
            assertWithMessage("$label 对非法输入 $illegal 应回落到 $expected")
                .that(runCatching { fromInt(illegal) }.getOrNull())
                .isEqualTo(expected)
        }
    }

    /** 断言 StateFlow 的值（带超时等待，防御异步派发）。 */
    private fun assertThatValue(
        label: String,
        flow: StateFlow<Any?>,
        expected: Any?,
    ) {
        assertWithMessage("$label 解码结果不符（若为默认值说明 intEnumFlow 未使用 fromInt）")
            .that(awaitValue(flow, expected))
            .isEqualTo(expected)
    }

    /** 轮询等待 StateFlow 出现期望值，最多 2s，返回最终值。 */
    private fun awaitValue(
        flow: StateFlow<Any?>,
        expected: Any?,
    ): Any? {
        val deadline = System.currentTimeMillis() + 2_000L
        while (flow.value != expected && System.currentTimeMillis() < deadline) {
            runBlocking { delay(5) }
        }
        return flow.value
    }
}
