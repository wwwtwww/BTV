package dev.frost819.newbv.app.ui.state.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [PlayerUiState] 与播放器状态模型的单元测试。
 *
 * 验证番剧判定、错误类型分类、画质降级标识与 CDN 地址等状态字段的正确性。
 */
class PlayerUiStateTest {
    @Test
    fun `isPgc is false when epid is null`() {
        assertThat(PlayerUiState(epid = null).isPgc).isFalse()
    }

    @Test
    fun `isPgc is false when epid is zero`() {
        assertThat(PlayerUiState(epid = 0).isPgc).isFalse()
    }

    @Test
    fun `isPgc is true when epid is positive`() {
        assertThat(PlayerUiState(epid = 12345).isPgc).isTrue()
    }

    @Test
    fun `isPgc is independent of fromSeason`() {
        assertThat(PlayerUiState(epid = null, fromSeason = true).isPgc).isFalse()
        assertThat(PlayerUiState(epid = 1, fromSeason = false).isPgc).isTrue()
    }

    @Test
    fun `playerState Error preserves type and canDowngradeQuality`() {
        val error =
            PlayerState.Error(
                message = "缓冲超时",
                type = PlayerErrorType.Timeout,
                canDowngradeQuality = true,
            )

        assertThat(error.type).isEqualTo(PlayerErrorType.Timeout)
        assertThat(error.canDowngradeQuality).isTrue()
        assertThat(error.message).isEqualTo("缓冲超时")
    }

    @Test
    fun `playerUiState stores currentCdnUrl`() {
        val state = PlayerUiState(currentCdnUrl = "https://upos-sz-mirror08c.bilivideo.com/live/test.m3u8")
        assertThat(state.currentCdnUrl).isEqualTo("https://upos-sz-mirror08c.bilivideo.com/live/test.m3u8")
    }

    @Test
    fun `playerErrorType contains expected enum values`() {
        val names = PlayerErrorType.entries.map { it.name }
        assertThat(names).containsExactly(
            "NetworkDisconnected",
            "Timeout",
            "SourceResolutionFailed",
            "DecoderError",
            "Generic",
        )
    }
}
