package dev.frost819.newbv.core.log

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [LogSanitizer] 的单元测试。
 *
 * 验证 Cookie 串、URL query、JSON 三种格式的脱敏能力，
 * 以及空串、无敏感信息、大小写不敏感等边界条件。
 */
class LogSanitizerTest {
    // -----------------------------------------------------------------------
    // Cookie 串脱敏
    // -----------------------------------------------------------------------

    @Test
    fun `sanitize cookie string replaces SESSDATA and bili_jct values`() {
        // Given / When
        val result = LogSanitizer.sanitize("SESSDATA=abc123; bili_jct=def456")

        // Then
        assertThat(result).contains("SESSDATA=***")
        assertThat(result).contains("bili_jct=***")
        assertThat(result).doesNotContain("abc123")
        assertThat(result).doesNotContain("def456")
    }

    @Test
    fun `sanitize cookie string with all sensitive keys`() {
        // Given
        val input =
            "SESSDATA=sess1; bili_jct=jct1; access_key=ak1; access_token=at1; refresh_token=rt1"

        // When
        val result = LogSanitizer.sanitize(input)

        // Then
        assertThat(result).contains("SESSDATA=***")
        assertThat(result).contains("bili_jct=***")
        assertThat(result).contains("access_key=***")
        assertThat(result).contains("access_token=***")
        assertThat(result).contains("refresh_token=***")
    }

    // -----------------------------------------------------------------------
    // URL query 脱敏
    // -----------------------------------------------------------------------

    @Test
    fun `sanitize URL query replaces access_key but keeps other params`() {
        // Given / When
        val result = LogSanitizer.sanitize("?access_key=secret123&other=visible")

        // Then
        assertThat(result).contains("access_key=***")
        assertThat(result).contains("other=visible")
        assertThat(result).doesNotContain("secret123")
    }

    @Test
    fun `sanitize URL query with multiple sensitive params`() {
        // Given / When
        val result =
            LogSanitizer.sanitize(
                "https://api.example.com/login?access_token=tok1&refresh_token=ref1&uid=123",
            )

        // Then
        assertThat(result).contains("access_token=***")
        assertThat(result).contains("refresh_token=***")
        assertThat(result).contains("uid=123")
    }

    // -----------------------------------------------------------------------
    // JSON 脱敏
    // -----------------------------------------------------------------------

    @Test
    fun `sanitize JSON replaces access_token value`() {
        // Given / When
        val result = LogSanitizer.sanitize("""{"access_token":"secret_value"}""")

        // Then
        assertThat(result).contains("\"access_token\"")
        assertThat(result).contains("\"***\"")
        assertThat(result).doesNotContain("secret_value")
    }

    @Test
    fun `sanitize JSON with multiple sensitive fields`() {
        // Given
        val input =
            """{"SESSDATA":"abc","bili_jct":"def","other":"keep"}"""

        // When
        val result = LogSanitizer.sanitize(input)

        // Then
        assertThat(result).contains("\"SESSDATA\":\"***\"")
        assertThat(result).contains("\"bili_jct\":\"***\"")
        assertThat(result).contains("\"other\":\"keep\"")
    }

    // -----------------------------------------------------------------------
    // 边界条件
    // -----------------------------------------------------------------------

    @Test
    fun `sanitize returns original string when no sensitive info present`() {
        // Given
        val input = "This is a normal log message without any sensitive data."

        // When
        val result = LogSanitizer.sanitize(input)

        // Then
        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `sanitize handles empty string without crashing`() {
        // Given / When
        val result = LogSanitizer.sanitize("")

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `sanitize is case insensitive for key matching`() {
        // Given
        val input = "sessdata=abc123; BILI_JCT=def456; Access_Key=ghi789"

        // When
        val result = LogSanitizer.sanitize(input)

        // Then
        assertThat(result).contains("sessdata=***")
        assertThat(result).contains("BILI_JCT=***")
        assertThat(result).contains("Access_Key=***")
        assertThat(result).doesNotContain("abc123")
        assertThat(result).doesNotContain("def456")
        assertThat(result).doesNotContain("ghi789")
    }

    @Test
    fun `sanitize preserves surrounding text`() {
        // Given
        val input = "Login request with SESSDATA=abc123 completed successfully"

        // When
        val result = LogSanitizer.sanitize(input)

        // Then
        assertThat(result).startsWith("Login request with")
        assertThat(result).endsWith("completed successfully")
        assertThat(result).contains("SESSDATA=***")
    }
}
