package dev.frost819.newbv.biliapi.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * 协程安全的挂起函数异常捕获。
 *
 * 与标准库 [runCatching] 相比，遇到外部协程取消（[CancellationException] 且不是 [TimeoutCancellationException]）
 * 时会主动重新抛出，保证结构化并发的取消机制不被静默吞掉。
 *
 * 超时（[TimeoutCancellationException]）和其他常规异常均会被安全捕获并包装为 [Result.failure]。
 */
inline fun <R> runSuspendCatching(block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
