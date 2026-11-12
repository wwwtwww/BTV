package dev.frost819.newbv.biliapi.util

/**
 * bili-api 日志级别。
 *
 * 按 [ordinal] 从小到大排列，[BiliLogger.minimumLevel] 用于过滤低于阈值的日志。
 */
enum class BiliLogLevel {
    /** 调试信息，仅在排查问题时输出。 */
    Debug,

    /** 关键流程节点（接口调用、状态切换）。 */
    Info,

    /** 可恢复异常（fallback、重试、解析降级）。 */
    Warn,

    /** 影响功能的失败（请求失败、数据解析失败）。 */
    Error,
}

/**
 * 日志输出器，[BiliLogger] 的输出委托接口。
 *
 * `bili-api` 是纯 JVM 模块，不能依赖 `android.util.Log`，因此由模块内部提供默认的
 * 标准输出实现，Android 运行时在应用启动时替换为本接口的 Logcat 实现。
 *
 * 用法（Android 侧）：
 * ```kotlin
 * BiliLogger.sink = BiliLogSink { level, tag, message, throwable ->
 *     val priority = when (level) {
 *         BiliLogLevel.Error -> Log.ERROR
 *         BiliLogLevel.Warn -> Log.WARN
 *         BiliLogLevel.Info -> Log.INFO
 *         BiliLogLevel.Debug -> Log.DEBUG
 *     }
 *     Log.println(priority, "BiliAPI/$tag", message)
 * }
 * ```
 */
fun interface BiliLogSink {
    /**
     * 输出一条日志。
     *
     * @param level 日志级别
     * @param tag 业务标签（如 `bili-api`、`VideoPlayRepository`）
     * @param message 已由调用方求值的日志文本
     * @param throwable 关联异常，可为 null；实现应自行输出堆栈
     */
    fun log(
        level: BiliLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    )
}

/**
 * 标准输出实现（JVM 测试友好）。
 *
 * 输出格式为 `[tag][LEVEL] message`，异常堆栈打印到 `System.out`，
 * 因此单元测试可通过重定向 `System.out` 断言输出内容。
 */
internal object ConsoleLogSink : BiliLogSink {
    override fun log(
        level: BiliLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        println("[$tag][${level.name.uppercase()}] $message")
        throwable?.printStackTrace(System.out)
    }
}

/**
 * bili-api 的轻量 JVM 日志包装。
 *
 * 采用委托模式：所有级别方法都把实际输出交给可替换的 [BiliLogger.sink]，
 * 默认 sink 为 [ConsoleLogSink]（`println`，不进 Logcat）。Android 侧必须在
 * `Application.onCreate` 中把 [sink] 替换为 `android.util.Log` 实现，否则接口层
 * 的排障信息无法在大屏设备上被 Logcat / CrashHandler 采集。
 *
 * 惰性求值：消息使用 lambda 传入，被 [minimumLevel] 过滤时不会构造字符串。
 */
object BiliLogger {
    /** 模块默认日志标签。 */
    const val DEFAULT_TAG: String = "bili-api"

    /**
     * 日志输出器。
     *
     * Android 运行时替换为 Logcat 实现；测试可注入自定义实现做断言，
     * 用完调用 [resetSink] 复原，避免污染其他用例。
     */
    @Volatile
    var sink: BiliLogSink = ConsoleLogSink

    /**
     * 最低输出级别，低于该级别的日志被丢弃（比较依据为 [BiliLogLevel.ordinal]）。
     *
     * 默认 [BiliLogLevel.Debug]，即全部输出，保持与历史行为一致。
     */
    @Volatile
    var minimumLevel: BiliLogLevel = BiliLogLevel.Debug

    /** 将 [sink] 恢复为 JVM 默认的 [ConsoleLogSink]，主要供测试清理使用。 */
    fun resetSink() {
        sink = ConsoleLogSink
    }

    /** 记录 Debug 级别日志。 */
    fun debug(message: () -> String) {
        log(DEFAULT_TAG, BiliLogLevel.Debug, null, message)
    }

    /** 记录指定标签的 Debug 级别日志。 */
    fun debug(
        tag: String,
        message: () -> String,
    ) {
        log(tag, BiliLogLevel.Debug, null, message)
    }

    /** 记录 Info 级别日志。 */
    fun info(message: () -> String) {
        log(DEFAULT_TAG, BiliLogLevel.Info, null, message)
    }

    /** 记录指定标签的 Info 级别日志。 */
    fun info(
        tag: String,
        message: () -> String,
    ) {
        log(tag, BiliLogLevel.Info, null, message)
    }

    /** 记录 Warn 级别日志。 */
    fun warn(message: () -> String) {
        log(DEFAULT_TAG, BiliLogLevel.Warn, null, message)
    }

    /** 记录带异常的 Warn 级别日志。 */
    fun warn(
        throwable: Throwable,
        message: () -> String,
    ) {
        log(DEFAULT_TAG, BiliLogLevel.Warn, throwable, message)
    }

    /** 记录指定标签的 Warn 级别日志。 */
    fun warn(
        tag: String,
        message: () -> String,
    ) {
        log(tag, BiliLogLevel.Warn, null, message)
    }

    /** 记录指定标签、带异常的 Warn 级别日志。 */
    fun warn(
        tag: String,
        throwable: Throwable,
        message: () -> String,
    ) {
        log(tag, BiliLogLevel.Warn, throwable, message)
    }

    /** 记录 Error 级别日志。 */
    fun error(message: () -> String) {
        log(DEFAULT_TAG, BiliLogLevel.Error, null, message)
    }

    /** 记录带异常的 Error 级别日志。 */
    fun error(
        throwable: Throwable,
        message: () -> String,
    ) {
        log(DEFAULT_TAG, BiliLogLevel.Error, throwable, message)
    }

    /** 记录指定标签的 Error 级别日志。 */
    fun error(
        tag: String,
        message: () -> String,
    ) {
        log(tag, BiliLogLevel.Error, null, message)
    }

    /** 记录指定标签、带异常的 Error 级别日志。 */
    fun error(
        tag: String,
        throwable: Throwable,
        message: () -> String,
    ) {
        log(tag, BiliLogLevel.Error, throwable, message)
    }

    /**
     * 统一入口：级别过滤 + 惰性求值 + 委托给 [sink]。
     *
     * sink 抛出的异常被吞掉——日志基础设施不应影响业务流程，
     * 与 `:core` 的 `AndroidLogger` 保持一致的容错策略。
     *
     * @param tag 日志标签
     * @param level 日志级别
     * @param throwable 关联异常，可为 null
     * @param message 惰性消息构造器
     */
    private fun log(
        tag: String,
        level: BiliLogLevel,
        throwable: Throwable?,
        message: () -> String,
    ) {
        if (level.ordinal < minimumLevel.ordinal) return
        runCatching { sink.log(level, tag, message(), throwable) }
    }
}
