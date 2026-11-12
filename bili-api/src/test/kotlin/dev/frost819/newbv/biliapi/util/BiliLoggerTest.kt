package dev.frost819.newbv.biliapi.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * [BiliLogger] 的单元测试。
 *
 * 验证 JVM 默认输出使用标准输出并保留级别与异常上下文，
 * 同时验证可替换 sink（Android 侧接 Logcat）的委托与级别过滤行为。
 */
class BiliLoggerTest {
    private lateinit var originalOut: PrintStream
    private lateinit var output: ByteArrayOutputStream

    /** 记录自定义 sink 收到的日志条目。 */
    private data class Entry(
        val level: BiliLogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )

    @BeforeEach
    fun setUp() {
        originalOut = System.out
        output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        BiliLogger.resetSink()
        BiliLogger.minimumLevel = BiliLogLevel.Debug
    }

    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        BiliLogger.resetSink()
        BiliLogger.minimumLevel = BiliLogLevel.Debug
    }

    @Test
    fun `info and warn include bili api prefix`() {
        BiliLogger.info { "request started" }
        BiliLogger.warn { "fallback" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][INFO] request started")
        assertThat(text).contains("[bili-api][WARN] fallback")
    }

    @Test
    fun `warn with throwable includes stack trace`() {
        BiliLogger.warn(IllegalStateException("warn-broken")) { "warn with exception" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][WARN] warn with exception")
        assertThat(text).contains("IllegalStateException: warn-broken")
    }

    @Test
    fun `error without throwable includes prefix`() {
        BiliLogger.error { "simple error" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][ERROR] simple error")
    }

    @Test
    fun `error with throwable includes stack trace`() {
        BiliLogger.error(IllegalStateException("broken")) { "request failed" }

        val text = output.toString()
        assertThat(text).contains("[bili-api][ERROR] request failed")
        assertThat(text).contains("IllegalStateException: broken")
    }

    @Test
    fun `debug uses default tag and level`() {
        BiliLogger.debug { "heartbeat result" }

        assertThat(output.toString()).contains("[bili-api][DEBUG] heartbeat result")
    }

    @Test
    fun `custom sink receives level tag message and throwable`() {
        val entries = mutableListOf<Entry>()
        BiliLogger.sink =
            BiliLogSink { level, tag, message, throwable ->
                entries.add(Entry(level, tag, message, throwable))
            }
        val failure = IllegalStateException("sink-broken")

        BiliLogger.info { "opened" }
        BiliLogger.warn("VideoPlayRepository") { "fallback" }
        BiliLogger.error("VideoPlayRepository", failure) { "get play data failed" }

        assertThat(entries.map { it.level to it.tag })
            .containsExactly(
                BiliLogLevel.Info to "bili-api",
                BiliLogLevel.Warn to "VideoPlayRepository",
                BiliLogLevel.Error to "VideoPlayRepository",
            )
        assertThat(entries.map { it.message })
            .containsExactly("opened", "fallback", "get play data failed")
        assertThat(entries.last().throwable).isSameInstanceAs(failure)
    }

    @Test
    fun `error without throwable passes null throwable to sink`() {
        val entries = mutableListOf<Entry>()
        BiliLogger.sink =
            BiliLogSink { level, tag, message, throwable ->
                entries.add(Entry(level, tag, message, throwable))
            }

        BiliLogger.error { "simple error" }

        assertThat(entries).hasSize(1)
        assertThat(entries.single().throwable).isNull()
    }

    @Test
    fun `minimum level filters lower priority messages`() {
        BiliLogger.minimumLevel = BiliLogLevel.Info
        val entries = mutableListOf<Entry>()
        BiliLogger.sink =
            BiliLogSink { level, tag, message, throwable ->
                entries.add(Entry(level, tag, message, throwable))
            }

        BiliLogger.debug { "hidden" }
        BiliLogger.warn { "kept" }

        assertThat(entries.map { it.message }).containsExactly("kept")
    }

    @Test
    fun `filtered message lambda is not evaluated`() {
        BiliLogger.minimumLevel = BiliLogLevel.Error
        var evaluated = false

        BiliLogger.info {
            evaluated = true
            "expensive"
        }

        assertThat(evaluated).isFalse()
    }

    @Test
    fun `failing sink does not break caller`() {
        BiliLogger.sink =
            BiliLogSink { _, _, _, _ ->
                throw IllegalStateException("sink broken")
            }

        BiliLogger.error { "still returns" }

        assertThat(output.toString()).isEmpty()
    }

    @Test
    fun `reset sink restores console output`() {
        BiliLogger.sink = BiliLogSink { _, _, _, _ -> }

        BiliLogger.resetSink()
        BiliLogger.info { "back to console" }

        assertThat(output.toString()).contains("[bili-api][INFO] back to console")
    }
}
