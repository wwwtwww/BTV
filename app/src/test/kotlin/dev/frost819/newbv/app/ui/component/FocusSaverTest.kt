package dev.frost819.newbv.app.ui.component

import androidx.compose.runtime.mutableStateOf
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [FocusSaver] 核心状态逻辑的单元测试。
 *
 * 验证焦点保存、清空、Requester 缓存及键值追踪功能。
 */
class FocusSaverTest {
    @Test
    fun `saveFocusedKey and savedKeyValue track current key correctly`() {
        val state = mutableStateOf("")
        val saver = FocusSaver(state)

        assertThat(saver.savedKeyValue()).isEmpty()

        saver.saveFocusedKey("item_5")
        assertThat(saver.savedKeyValue()).isEqualTo("item_5")

        saver.saveFocusedKey("cover")
        assertThat(saver.savedKeyValue()).isEqualTo("cover")
    }

    @Test
    fun `clearFocusedKey resets key to empty string`() {
        val state = mutableStateOf("initial_key")
        val saver = FocusSaver(state)

        saver.clearFocusedKey()
        assertThat(saver.savedKeyValue()).isEmpty()
    }

    @Test
    fun `focusRequesterFor returns same instance for same key`() {
        val state = mutableStateOf("")
        val saver = FocusSaver(state)

        val req1 = saver.focusRequesterFor("card_1")
        val req2 = saver.focusRequesterFor("card_1")
        val req3 = saver.focusRequesterFor("card_2")

        assertThat(req1).isSameInstanceAs(req2)
        assertThat(req1).isNotSameInstanceAs(req3)
    }
}
