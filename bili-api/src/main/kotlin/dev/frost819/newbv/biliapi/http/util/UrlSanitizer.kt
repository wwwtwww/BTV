package dev.frost819.newbv.biliapi.http.util

/**
 * URL 脱敏工具。
 *
 * 对 URL 字符串中的敏感查询参数值进行掩码替换（替换为 `***`），
 * 防止 `access_key`、`access_token` 等凭证泄露到 HTTP 日志。
 *
 * 本类是 [dev.frost819.newbv.core.log.LogSanitizer] 在纯 JVM 模块 `:bili-api` 中的轻量等价实现，
 * 因 `:bili-api` 为 JVM 模块、无法依赖 Android 库 `:core`，故独立维护。
 *
 * 使用方式：
 * ```kotlin
 * val safeUrl = UrlSanitizer.sanitize(rawUrl)
 * ```
 */
object UrlSanitizer {
    /**
     * 需要脱敏的 URL 查询参数名列表。
     */
    private val SENSITIVE_PARAMS =
        listOf(
            "SESSDATA",
            "bili_jct",
            "access_key",
            "access_token",
            "refresh_token",
        )

    /**
     * 匹配 URL query 或 Cookie 格式中的 `key=value` 对。
     *
     * 值截止到 `&`、`;`、空白或串尾。
     */
    private val pattern: Regex by lazy {
        val keys = SENSITIVE_PARAMS.joinToString("|") { Regex.escape(it) }
        Regex("(?i)($keys)\\s*=\\s*[^;&\\s\"}]*", RegexOption.IGNORE_CASE)
    }

    /**
     * 对 URL 字符串中的敏感参数值进行脱敏。
     *
     * @param input 待脱敏的 URL 或 Cookie 字符串。
     * @return 脱敏后的字符串；敏感值已替换为 `***`。空串原样返回。
     */
    fun sanitize(input: String): String {
        if (input.isEmpty()) return input
        return pattern.replace(input) { matchResult ->
            val key = matchResult.groupValues[1]
            "$key=***"
        }
    }
}
