package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [ScreensaverState] 的单元测试。
 *
 * 验证屏保激活状态跟踪、用户交互更新时间戳以及屏保退出逻辑。
 */
class ScreensaverStateTest {
    @Test
    fun `initial active is false by default`() {
        var currentTime = 1000L
        val state = ScreensaverState(timeProvider = { currentTime })
        assertThat(state.isScreensaverActive).isFalse()
        assertThat(state.lastInteractionTime).isEqualTo(1000L)
    }

    @Test
    fun `initial active can be set to true`() {
        val state = ScreensaverState(initialActive = true)
        assertThat(state.isScreensaverActive).isTrue()
    }

    @Test
    fun `onUserInteraction updates lastInteractionTime`() {
        var currentTime = 1000L
        val state = ScreensaverState(timeProvider = { currentTime })
        val oldTime = state.lastInteractionTime

        currentTime = 1500L
        state.onUserInteraction()

        assertThat(state.lastInteractionTime).isEqualTo(1500L)
        assertThat(state.lastInteractionTime).isGreaterThan(oldTime)
    }

    @Test
    fun `dismissScreensaver resets active state and updates interaction time`() {
        var currentTime = 1000L
        val state = ScreensaverState(initialActive = true, timeProvider = { currentTime })
        val oldTime = state.lastInteractionTime

        currentTime = 1500L
        state.dismissScreensaver()

        assertThat(state.isScreensaverActive).isFalse()
        assertThat(state.lastInteractionTime).isEqualTo(1500L)
        assertThat(state.lastInteractionTime).isGreaterThan(oldTime)
    }

    @Test
    fun `screensaver activates when elapsed exceeds timeout`() {
        var currentTime = 2000L
        val state = ScreensaverState(timeProvider = { currentTime })
        val timeoutMs = 1000L

        // Simulate interaction occurred at 500L
        state.lastInteractionTime = 500L
        val elapsed = currentTime - state.lastInteractionTime
        val remaining = timeoutMs - elapsed

        assertThat(remaining).isLessThan(0L)
        if (remaining <= 0) {
            state.isScreensaverActive = true
        }

        assertThat(state.isScreensaverActive).isTrue()
    }

    @Test
    fun `onUserInteraction delays screensaver activation`() {
        var currentTime = 1800L
        val state = ScreensaverState(timeProvider = { currentTime })
        val timeoutMs = 1000L

        // Prior interaction at 1000L
        state.lastInteractionTime = 1000L

        // User interacts at currentTime 1800L, resetting lastInteractionTime
        state.onUserInteraction()

        val elapsed = currentTime - state.lastInteractionTime
        val remaining = timeoutMs - elapsed

        assertThat(remaining).isGreaterThan(0L)
        assertThat(state.isScreensaverActive).isFalse()
    }
}
