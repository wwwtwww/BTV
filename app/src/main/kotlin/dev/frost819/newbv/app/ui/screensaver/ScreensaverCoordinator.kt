package dev.frost819.newbv.app.ui.screensaver

import android.os.SystemClock
import dev.frost819.newbv.app.di.ApplicationScope
import dev.frost819.newbv.data.datastore.ScreensaverTimeout
import dev.frost819.newbv.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用级屏保协调器。
 *
 * 全局追踪用户最后交互时间与播放状态，决定屏保是否允许激活。
 * 由 `MainActivity.onUserInteraction` 回调更新交互时间，
 * 由播放器页面通过 [setContentPlaying] 声明播放状态、通过 [releaseContentPlaying] 释放。
 *
 * 播放状态采用 **owner 聚合（引用计数）** 而非单一布尔量：每个调用方携带自己的
 * [Any] token，只要集合中还有任意 owner 声明「播放中」，屏保就被抑制。
 * 这样可避免 Compose Navigation 转场期间的「最后写入者获胜」竞态 ——
 * 新页 B 的 `LaunchedEffect` 先注册、旧页 A 的 `onDispose` 后执行时，
 * A 只移除自己的 owner，不会覆盖 B 的声明。
 *
 * 超时配置通过 [SettingsRepository.screensaverTimeout] 响应式获取，
 * 修改后立即生效，无需重启页面。
 *
 * 本类只负责「何时该亮屏保」的判定，不负责屏保的渲染与倒计时循环，
 * 后者由应用级 Overlay 组件的计时循环轮询 [elapsedSinceLastInteraction] 完成。
 *
 * @param settingsRepository 设置仓库，提供屏保超时设置的数据源。
 * @param applicationScope 应用级协程作用域，用于把设置流转换为常驻 [StateFlow]。
 * @see ScreensaverTimeout
 */
@Singleton
class ScreensaverCoordinator
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        @ApplicationScope applicationScope: CoroutineScope,
    ) {
        /**
         * 最后交互时间戳（毫秒）。
         *
         * 使用 [SystemClock.elapsedRealtime] 单调时钟，不受系统时间/时区修改影响，
         * 开机后一直递增，因此空闲时长计算不会出现负值。
         */
        private val lastInteractionElapsed = MutableStateFlow(SystemClock.elapsedRealtime())

        /**
         * 处于「需要亮屏」状态（活跃播放/缓冲/加载/出错）的 owner 集合。
         *
         * 集合非空时抑制屏保激活，避免观众正在看画面或被要求阅读错误提示时被屏保打断。
         * 用不可变集合 + [MutableStateFlow.update] 的 CAS 循环保证多调用方并发注册安全。
         */
        private val playingOwners = MutableStateFlow<Set<Any>>(emptySet())

        /**
         * 屏保超时毫秒数（来自用户设置，0 表示禁用屏保）。
         *
         * 响应式跟随 [SettingsRepository.screensaverTimeout]，用户在设置页修改后无需重启即生效。
         */
        val timeoutMs: StateFlow<Long> =
            settingsRepository.screensaverTimeout
                .map { it.timeoutMs }
                .stateIn(applicationScope, SharingStarted.Eagerly, ScreensaverTimeout.ThreeMinutes.timeoutMs)

        /**
         * 屏保是否允许激活（超时设置 > 0 且当前非播放状态）。
         *
         * 该标志只表达「是否允许」，不代表屏保已激活；是否激活还需由 Overlay 比较
         * [elapsedSinceLastInteraction] 与本流中的超时阈值。
         */
        val isScreensaverAllowed: StateFlow<Boolean> =
            combine(timeoutMs, playingOwners) { timeout, owners ->
                timeout > 0L && owners.isEmpty()
            }.stateIn(applicationScope, SharingStarted.Eagerly, false)

        /**
         * 距上次交互经过的毫秒数。
         *
         * 由 Overlay 的计时循环调用，与 [timeoutMs] 比较决定是否激活屏保。
         *
         * @return 非负空闲时长（毫秒）；若系统单调时钟异常回退（elapsedRealtime 不会回退，
         *   此处仅作防御），返回 0。
         */
        fun elapsedSinceLastInteraction(): Long =
            (SystemClock.elapsedRealtime() - lastInteractionElapsed.value).coerceAtLeast(0L)

        /**
         * 重置空闲计时器，把最后交互时间更新为当前时刻。
         *
         * 由 `MainActivity.onUserInteraction` 调用；屏保退出时也应调用一次，
         * 使屏保退出后的下一次交互重新从零开始计时。
         */
        fun onUserInteraction() {
            lastInteractionElapsed.value = SystemClock.elapsedRealtime()
        }

        /**
         * 声明某个 owner 的播放状态。
         *
         * 播放页采用「黑名单」约定：仅 Paused/Ended 传 `false`（允许屏保），
         * 其余状态（Ready/Loading/Playing/Buffering/Error）一律传 `true`（抑制屏保），
         * 避免全黑屏保遮挡加载提示、错误信息与重试入口。
         *
         * [owner] 参与聚合而非互相覆盖：转场期间新旧页面先后调用各自携带不同 token，
         * 最终是否抑制屏保取决于「是否还有任何 owner 声明播放」，与调用顺序无关。
         * 调用方必须在组合销毁（`DisposableEffect.onDispose`）时调用
         * [releaseContentPlaying]，否则退出播放页后主页将失去屏保能力。
         *
         * @param owner 调用方唯一标识，推荐用 Screen 级 `remember { Any() }` token；
         *   同一 owner 重复声明是幂等的。
         * @param playing 该 owner 是否处于活跃播放/缓冲/加载/出错状态。
         * @see releaseContentPlaying
         */
        fun setContentPlaying(
            owner: Any,
            playing: Boolean,
        ) {
            playingOwners.update { current ->
                if (playing) current + owner else current - owner
            }
        }

        /**
         * 释放某个 owner 的播放声明。
         *
         * 由 `DisposableEffect.onDispose` 调用，安全移除自身记录，
         * 不影响其他 owner（如转场期间新页已注册的 owner）。
         *
         * @param owner 之前传给 [setContentPlaying] 的同一 token；未注册过时为幂等空操作。
         */
        fun releaseContentPlaying(owner: Any) {
            playingOwners.update { current -> current - owner }
        }
    }
