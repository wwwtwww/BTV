package dev.frost819.newbv.core.log

/**
 * 日志脱敏工具。
 *
 * 对日志文本中的敏感凭证字段值进行掩码替换（替换为 `***`），
 * 防止登录态 Cookie、Token 等泄露到日志文件与上传通道。
 *
 * 支持三种常见格式：
 * - Cookie 串：`SESSDATA=abc123; bili_jct=def456`
 * - URL query：`?access_key=xxx&other=yyy`
 * - JSON：`{"SESSDATA": "abc123", "bili_jct": "def456"}`
 *
 * 敏感字段列表（大小写不敏感匹配）：
 * `SESSDATA`、`bili_jct`、`access_key`、`access_token`、`refresh_token`
 *
 * 使用方式：
 * ```kotlin
 * val safe = LogSanitizer.sanitize(rawLog)
 * ```
 */
object LogSanitizer {
    /**
     * 需要脱敏的敏感字段名列表。
     */
    private val SENSITIVE_KEYS =
        listOf(
            "SESSDATA",
            "bili_jct",
            "access_key",
            "access_token",
            "refresh_token",
        )

    /**
     * 匹配 Cookie / URL query 格式：`key=value`（值到 `;`、`&`、空白或串尾截止）。
     *
     * 示例匹配：
     * - `SESSDATA=abc123` → `SESSDATA=***`
     * - `access_key=xxx&other=yyy` → `access_key=***&other=yyy`
     */
    private val cookieOrQueryPattern: Regex by lazy {
        val keys = SENSITIVE_KEYS.joinToString("|") { Regex.escape(it) }
        Regex("(?i)($keys)\\s*=\\s*[^;&\\s\"}]*", RegexOption.IGNORE_CASE)
    }

    /**
     * 匹配 JSON 格式：`"key" : "value"`。
     *
     * 示例匹配：
     * - `{"SESSDATA": "abc123"}` → `{"SESSDATA": "***"}`
     */
    private val jsonPattern: Regex by lazy {
        val keys = SENSITIVE_KEYS.joinToString("|") { Regex.escape(it) }
        Regex("(?i)(\"($keys)\"\\s*:\\s*)\"[^\"]*\"", RegexOption.IGNORE_CASE)
    }

    /**
     * 对输入字符串中的敏感字段值进行脱敏。
     *
     * 依次处理 JSON 格式与 Cookie / URL query 格式，保证三种输入均能正确掩码。
     * 若输入不包含敏感字段，原样返回。
     *
     * @param input 待脱敏的原始字符串（可能为 Cookie 串、URL query 或 JSON 文本）。
     * @return 脱敏后的字符串；敏感值已替换为 `***`。
     */
    fun sanitize(input: String): String {
        if (input.isEmpty()) return input

        // 先处理 JSON 格式（"key": "value"）
        val afterJson =
            jsonPattern.replace(input) { matchResult ->
                "${matchResult.groupValues[1]}\"***\""
            }

        // 再处理 Cookie / URL query 格式（key=value）
        return cookieOrQueryPattern.replace(afterJson) { matchResult ->
            val key = matchResult.groupValues[1]
            "$key=***"
        }
    }
}
