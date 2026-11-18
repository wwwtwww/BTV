package dev.frost819.newbv.app.ui.screensaver

import android.os.SystemClock
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.ScreensaverTimeout
import dev.frost819.newbv.data.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [ScreensaverCoordinator] 的单元测试。
 *
 * 用 MockK 静态桩替换 [SystemClock.elapsedRealtime] 获得可控的单调时钟，
 * 用 Mock 的 [SettingsRepository] 验证超时设置的响应式传递，
 * 不依赖真实 Android 运行环境。
 *
 * 覆盖：超时映射与动态变更、空闲计时单调递增与交互重置、播放状态对屏保许可的抑制与释放
 * （含播放器 dispose 场景）、owner 聚合下的转场竞态（新页注册先于旧页释放不被覆盖）、
 * 密集播放态抖动后的稳定态、时钟回退防御、禁用（Never）时的许可。
 *
 * TODO(Task#24 遗留)：[GlobalScreensaverOverlay] 的 500ms 轮询计时循环未做 Compose 插桩测试。
 *   原因：Overlay 直接读取真实 SystemClock.elapsedRealtime 与真实 delay，而可选超时最小为
 *   OneMinute（60s），插桩测试无法在合理时间内推进真实时钟，成本收益不匹配。
 *   若后续为 Overlay 注入可控时钟（timeProvider/ticker），再补 compose 层的「超时拉起屏保 →
 *   按键 dismiss 归零」用例。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreensaverCoordinatorTest {
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    /** 可控单调时钟，替代 SystemClock.elapsedRealtime。 */
    private var fakeElapsed: Long = 5_000L

    /** 模拟页面 A 的 Screen 级 owner token（真实代码里是 `remember { Any() }`）。 */
    private val ownerA = Any()

    /** 模拟转场后的页面 B 的 owner token。 */
    private val ownerB = Any()

    private lateinit var timeoutFlow: MutableStateFlow<ScreensaverTimeout>
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var applicationScope: CoroutineScope
    private lateinit var coordinator: ScreensaverCoordinator

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { fakeElapsed }

        timeoutFlow = MutableStateFlow(ScreensaverTimeout.ThreeMinutes)
        settingsRepository = mockk()
        every { settingsRepository.screensaverTimeout } returns timeoutFlow

        applicationScope = CoroutineScope(testDispatcher + SupervisorJob())
        coordinator = ScreensaverCoordinator(settingsRepository, applicationScope)
    }

    @AfterEach
    fun tearDown() {
        applicationScope.cancel()
        unmockkAll()
    }

    @Test
    fun `timeoutMs maps settings enum to milliseconds and follows updates`() =
        runTest(testScheduler) {
            // Given：初始为默认三分钟
            advanceUntilIdle()
            assertThat(coordinator.timeoutMs.value).isEqualTo(180_000L)

            // When：用户把超时改为一分钟
            timeoutFlow.value = ScreensaverTimeout.OneMinute
            advanceUntilIdle()

            // Then：无需重启即响应式生效
            assertThat(coordinator.timeoutMs.value).isEqualTo(60_000L)
        }

    @Test
    fun `elapsedSinceLastInteraction counts monotonic clock and resets on interaction`() =
        runTest(testScheduler) {
            // Given：构造时刻为 5000，时钟推进到 8000
            fakeElapsed = 8_000L

            // Then：空闲时长为 3000
            assertThat(coordinator.elapsedSinceLastInteraction()).isEqualTo(3_000L)

            // When：用户交互后时钟再推进 1000
            fakeElapsed = 9_000L
            coordinator.onUserInteraction()
            fakeElapsed = 10_000L

            // Then：计时器归零重新累计
            assertThat(coordinator.elapsedSinceLastInteraction()).isEqualTo(1_000L)
        }

    @Test
    fun `elapsedSinceLastInteraction never returns negative when clock appears to go back`() =
        runTest(testScheduler) {
            // Given：交互记录在 9000
            fakeElapsed = 9_000L
            coordinator.onUserInteraction()

            // When：极端情况下读到更小的时钟值
            fakeElapsed = 8_000L

            // Then：防御性 clamp 到 0，Overlay 不会因负值误判超时
            assertThat(coordinator.elapsedSinceLastInteraction()).isEqualTo(0L)
        }

    @Test
    fun `isScreensaverAllowed is suppressed while content is playing`() =
        runTest(testScheduler) {
            coordinator.isScreensaverAllowed.test {
                // Given：超时 > 0 且未播放 → 允许
                assertThat(awaitItem()).isTrue()

                // When：进入播放/缓冲/加载
                coordinator.setContentPlaying(ownerA, true)

                // Then：抑制屏保
                assertThat(awaitItem()).isFalse()

                // When：暂停或播放结束
                coordinator.setContentPlaying(ownerA, false)

                // Then：重新允许
                assertThat(awaitItem()).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isScreensaverAllowed is false when screensaver is disabled`() =
        runTest(testScheduler) {
            coordinator.isScreensaverAllowed.test {
                assertThat(awaitItem()).isTrue()

                // When：用户选择「关闭」屏保（timeoutMs = 0）
                timeoutFlow.value = ScreensaverTimeout.Never

                // Then：即使非播放状态也不允许激活
                assertThat(awaitItem()).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `elapsedSinceLastInteraction grows monotonically while user stays idle`() =
        runTest(testScheduler) {
            // Given：在 9000 时刻交互一次，作为计时基准
            fakeElapsed = 9_000L
            coordinator.onUserInteraction()
            var previous = coordinator.elapsedSinceLastInteraction()
            assertThat(previous).isEqualTo(0L)

            // When/Then：时钟只增不减，空闲时长必须严格递增且等于与时钟的差值
            listOf(9_500L, 12_000L, 30_000L, 120_000L).forEach { now ->
                fakeElapsed = now
                val current = coordinator.elapsedSinceLastInteraction()
                assertThat(current).isEqualTo(now - 9_000L)
                assertThat(current).isGreaterThan(previous)
                previous = current
            }
        }

    @Test
    fun `timeout change updates allowed flag without resetting the idle timer`() =
        runTest(testScheduler) {
            // Given：基准 5000、当前 8000 → 已空闲 3000
            fakeElapsed = 8_000L
            val idleBefore = coordinator.elapsedSinceLastInteraction()
            assertThat(idleBefore).isEqualTo(3_000L)

            coordinator.isScreensaverAllowed.test {
                assertThat(awaitItem()).isTrue()

                // When：用户关闭屏保
                timeoutFlow.value = ScreensaverTimeout.Never

                // Then：许可翻转，但 Coordinator 不动计时基准
                // （契约：重启计时的责任在 Overlay 的 LaunchedEffect，避免设置页改一次就把空闲清零）
                assertThat(awaitItem()).isFalse()
                assertThat(coordinator.elapsedSinceLastInteraction()).isEqualTo(idleBefore)

                // When：重新选回一分钟
                timeoutFlow.value = ScreensaverTimeout.OneMinute

                // Then：许可恢复且阈值同步生效
                assertThat(awaitItem()).isTrue()
                assertThat(coordinator.timeoutMs.value).isEqualTo(60_000L)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setContentPlaying does not disturb the idle timer`() =
        runTest(testScheduler) {
            // Given：构造基准 5000，当前 20000 → 已空闲 15000
            fakeElapsed = 20_000L
            val idleBefore = coordinator.elapsedSinceLastInteraction()
            assertThat(idleBefore).isEqualTo(15_000L)

            // When：进入播放页声明播放，随后 dispose 释放
            coordinator.setContentPlaying(ownerA, true)
            coordinator.releaseContentPlaying(ownerA)
            fakeElapsed = 25_000L
            advanceUntilIdle()

            // Then：播放状态不得污染 lastInteractionElapsed，空闲时长继续累计，
            //   释放后屏保许可恢复为 true（退出播放器后主页重新具备屏保能力）
            assertThat(coordinator.elapsedSinceLastInteraction()).isEqualTo(idleBefore + 5_000L)
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()
        }

    @Test
    fun `playing state wins over any timeout update and restores allowance once released`() =
        runTest(testScheduler) {
            // Given：播放器活跃播放
            coordinator.setContentPlaying(ownerA, true)
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            // When：播放中用户来回改超时（含 Never），两个因子任意顺序组合都不得误放开
            timeoutFlow.value = ScreensaverTimeout.Never
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            timeoutFlow.value = ScreensaverTimeout.FiveMinutes
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            // When：播放器 dispose 释放（正常退出播放页）
            coordinator.releaseContentPlaying(ownerA)
            advanceUntilIdle()

            // Then：许可立即恢复
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()
        }

    @Test
    fun `rapid setContentPlaying bursts settle at the last requested state`() =
        runTest(testScheduler) {
            // When：以 false 结尾的密集抖动（模拟 Playing/Buffering 边界来回跳变）
            repeat(32) {
                coordinator.setContentPlaying(ownerA, true)
                coordinator.setContentPlaying(ownerA, false)
            }
            advanceUntilIdle()

            // Then：收敛到「允许」，不存在被卡在抑制态的中间态泄漏
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()

            // When：以 true 结尾的密集抖动
            repeat(32) {
                coordinator.setContentPlaying(ownerA, false)
                coordinator.setContentPlaying(ownerA, true)
            }
            advanceUntilIdle()

            // Then：收敛到「抑制」
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()
        }

    @Test
    fun `stale page release during transition does not override the new page playing declaration`() =
        runTest(testScheduler) {
            // Given：旧页 A 正在播放（活跃抑制），转场到新页 B 后 B 也声明播放中
            coordinator.setContentPlaying(ownerA, true)
            coordinator.setContentPlaying(ownerB, true)
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            // When：A 的 onDispose 晚于 B 的 LaunchedEffect 执行（Task#30 竞态现场）
            coordinator.releaseContentPlaying(ownerA)
            advanceUntilIdle()

            // Then：A 只移除自身记录，B 的声明仍然生效，屏保继续被抑制
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            // When/Then：B 也退出后才真正释放屏保能力
            coordinator.releaseContentPlaying(ownerB)
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()
        }

    @Test
    fun `any remaining playing owner keeps the screensaver suppressed`() =
        runTest(testScheduler) {
            // Given：两个 owner 同时声明播放（如播放页与其上叠加的另一个播放组合）
            coordinator.setContentPlaying(ownerA, true)
            coordinator.setContentPlaying(ownerB, true)
            advanceUntilIdle()

            // When：其一转为暂停态（playing=false），集合仍非空
            coordinator.setContentPlaying(ownerA, false)
            advanceUntilIdle()

            // Then：只要还有一个 owner 播放中，屏保不得激活
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            // When/Then：最后一个 owner 释放后恢复允许
            coordinator.releaseContentPlaying(ownerB)
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()
        }

    @Test
    fun `repeated declaration and unknown owner release are idempotent`() =
        runTest(testScheduler) {
            // When：同一 owner 重复声明播放
            coordinator.setContentPlaying(ownerA, true)
            coordinator.setContentPlaying(ownerA, true)
            advanceUntilIdle()
            assertThat(coordinator.isScreensaverAllowed.value).isFalse()

            // When：重复释放、以及释放从未注册过的 owner
            coordinator.releaseContentPlaying(ownerA)
            coordinator.releaseContentPlaying(ownerA)
            coordinator.releaseContentPlaying(ownerB)
            advanceUntilIdle()

            // Then：幂等，不会因重复操作被永久卡在抑制态
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()
        }

    @Test
    fun `owner declaring playing false never suppresses the screensaver`() =
        runTest(testScheduler) {
            // Given：转场到新页，新页处于 Paused 且非缓冲（黑名单策略允许屏保）
            coordinator.setContentPlaying(ownerB, false)
            advanceUntilIdle()

            // Then：该 owner 不得进入抑制集合
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()

            // When：旧页稍后 dispose 释放自身 token
            coordinator.releaseContentPlaying(ownerA)
            advanceUntilIdle()

            // Then：许可仍为允许，不会被多余的 release 反向影响
            assertThat(coordinator.isScreensaverAllowed.value).isTrue()
        }
}
