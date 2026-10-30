package dev.frost819.newbv.core.focus

import androidx.compose.ui.focus.FocusRequester
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [FocusExt] 中安全焦点请求工具的单元测试。
 *
 * 验证在未挂载节点、脱钩节点上的安全调用能力与重试机制。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusExtTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `safeRequestFocus on unattached requester returns false without throwing exception`() {
        val requester = FocusRequester()
        // 未绑定到任何 Composable 节点的 FocusRequester 原生调用会抛出 IllegalStateException
        // safeRequestFocus 必须吞掉异常并安全返回 false
        val result = requester.safeRequestFocus()
        assertThat(result).isFalse()
    }

    @Test
    fun `async safeRequestFocus retries specified times and executes cleanly`() =
        runTest(testDispatcher) {
            val requester = FocusRequester()
            var successCallbackInvoked = false

            val job =
                requester.safeRequestFocus(
                    scope = this,
                    maxRetries = 3,
                    retryDelayMs = 50,
                    onSuccess = { successCallbackInvoked = true },
                )

            // 第 1 次尝试执行失败，进入第 1 次重试延迟（50ms）
            runCurrent()
            assertThat(job.isActive).isTrue()

            // 时间前进 40ms（尚未达到第 1 次重试间隔 50ms），job 仍处于活跃等待中
            advanceTimeBy(40)
            runCurrent()
            assertThat(job.isActive).isTrue()

            // 时间再前进 20ms（共 60ms，超过第 1 次重试，正在进行第 2 次重试延迟）
            advanceTimeBy(20)
            runCurrent()
            assertThat(job.isActive).isTrue()

            // 时间再前进 50ms（共 110ms，达到 (maxRetries - 1) * retryDelayMs = 100ms），第 3 次重试失败并退出协程
            advanceTimeBy(50)
            runCurrent()
            assertThat(job.isCompleted).isTrue()

            // 因 requester 从未被绑定，所有 3 次重试失败后退出，不崩溃，回调未被触发
            assertThat(successCallbackInvoked).isFalse()
        }

    @Test
    fun `async safeRequestFocus triggers onSuccess and completes immediately on success`() =
        runTest(testDispatcher) {
            val requester = mockk<FocusRequester>()
            every { requester.requestFocus() } returns true

            var successCallbackInvoked = false
            val job =
                requester.safeRequestFocus(
                    scope = this,
                    maxRetries = 3,
                    retryDelayMs = 50,
                    onSuccess = { successCallbackInvoked = true },
                )

            runCurrent()

            // 首次尝试成功，立即触发 onSuccess 回调
            assertThat(successCallbackInvoked).isTrue()
            // 协程已立即完成，无需等待后续 retryDelayMs
            assertThat(job.isCompleted).isTrue()
        }

    @Test
    fun `async safeRequestFocus retries on failure and completes when subsequent attempt succeeds`() =
        runTest(testDispatcher) {
            val requester = mockk<FocusRequester>()
            var callCount = 0
            every { requester.requestFocus() } answers {
                callCount++
                if (callCount == 1) {
                    throw IllegalStateException("FocusRequester is not initialized")
                }
                // 第 2 次尝试成功
                true
            }

            var successCallbackInvoked = false
            val job =
                requester.safeRequestFocus(
                    scope = this,
                    maxRetries = 3,
                    retryDelayMs = 50,
                    onSuccess = { successCallbackInvoked = true },
                )

            // 第 1 次失败，进入重试延迟
            runCurrent()
            assertThat(successCallbackInvoked).isFalse()
            assertThat(job.isActive).isTrue()

            // 时间前进 50ms，触发第 2 次尝试并成功
            advanceTimeBy(50)
            runCurrent()
            assertThat(successCallbackInvoked).isTrue()
            assertThat(job.isCompleted).isTrue()
            assertThat(callCount).isEqualTo(2)
        }
}
