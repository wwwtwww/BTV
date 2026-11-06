package dev.frost819.newbv.biliapi.util

import bilibili.rpc.Status
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.grpc.utils.getDetail
import dev.frost819.newbv.biliapi.grpc.utils.getTypeClass
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [runSuspendCatching] 与 gRPC 异常基础设施的单元测试。
 *
 * 验证协程取消异常的重新抛出、超时与常规异常的捕获包装，以及空 gRPC 业务详情防御。
 */
class SuspendCatchingTest {
    @Test
    fun `runSuspendCatching returns success for normal result`() {
        val result = runSuspendCatching { "hello" }
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("hello")
    }

    @Test
    fun `runSuspendCatching catches normal exception and wraps as failure`() {
        val exception = IllegalStateException("business error")
        val result = runSuspendCatching { throw exception }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isSameInstanceAs(exception)
    }

    @Test
    fun `runSuspendCatching rethrows CancellationException`() {
        val cancelException = CancellationException("coroutine cancelled")
        assertThrows<CancellationException> {
            runSuspendCatching { throw cancelException }
        }
    }

    @Test
    fun `runSuspendCatching catches TimeoutCancellationException as failure`() =
        runTest {
            val result =
                runSuspendCatching {
                    withTimeout(10) {
                        delay(100)
                    }
                }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(TimeoutCancellationException::class.java)
        }

    @Test
    fun `handleGrpcException rethrows CancellationException`() {
        val cancelException = CancellationException("coroutine cancelled")
        assertThrows<CancellationException> {
            handleGrpcException(cancelException)
        }
    }

    @Test
    fun `getTypeClass and getDetail return null for empty detailsList without crashing`() {
        val status =
            Status
                .newBuilder()
                .setCode(0)
                .setMessage("ok")
                .build()
        assertThat(status.getTypeClass()).isNull()
        assertThat(status.getDetail()).isNull()
    }
}
