package dev.frost819.newbv.core.focus

import androidx.compose.ui.input.key.KeyEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * [KeyEventExt] 的单元测试。
 *
 * 验证遥控器方向键、确认键、返回键、菜单键以及 KeyDown/KeyUp 的判断逻辑。
 */
class KeyEventExtTest {
    private fun mockKeyEvent(
        keyCode: Int,
        action: Int = android.view.KeyEvent.ACTION_DOWN,
    ): KeyEvent {
        val nativeEvent = mockk<android.view.KeyEvent>()
        every { nativeEvent.keyCode } returns keyCode
        every { nativeEvent.action } returns action
        return KeyEvent(nativeEvent)
    }

    @Test
    fun `isKeyDown and isKeyUp return expected boolean`() {
        val downEvent = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.ACTION_DOWN)
        val upEvent = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.ACTION_UP)

        assertThat(downEvent.isKeyDown()).isTrue()
        assertThat(downEvent.isKeyUp()).isFalse()
        assertThat(upEvent.isKeyDown()).isFalse()
        assertThat(upEvent.isKeyUp()).isTrue()
    }

    @Test
    fun `dpad directions match expected keycodes`() {
        val up = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_UP)
        val down = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
        val left = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_LEFT)
        val right = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)

        assertThat(up.isDpadUp()).isTrue()
        assertThat(up.isDpadDown()).isFalse()

        assertThat(down.isDpadDown()).isTrue()
        assertThat(down.isDpadUp()).isFalse()

        assertThat(left.isDpadLeft()).isTrue()
        assertThat(left.isDpadRight()).isFalse()

        assertThat(right.isDpadRight()).isTrue()
        assertThat(right.isDpadLeft()).isFalse()
    }

    @Test
    fun `isConfirm matches dpad center and enter`() {
        val center = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
        val enter = mockKeyEvent(android.view.KeyEvent.KEYCODE_ENTER)
        val other = mockKeyEvent(android.view.KeyEvent.KEYCODE_BACK)

        assertThat(center.isConfirm()).isTrue()
        assertThat(enter.isConfirm()).isTrue()
        assertThat(other.isConfirm()).isFalse()
    }

    @Test
    fun `isBack matches back key`() {
        val back = mockKeyEvent(android.view.KeyEvent.KEYCODE_BACK)
        val other = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_CENTER)

        assertThat(back.isBack()).isTrue()
        assertThat(other.isBack()).isFalse()
    }

    @Test
    fun `isMenuKey matches standard Menu key`() {
        val menu = mockKeyEvent(android.view.KeyEvent.KEYCODE_MENU)
        val other = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_UP)

        assertThat(menu.isMenuKey()).isTrue()
        assertThat(other.isMenuKey()).isFalse()
    }

    @Test
    fun `isMenuKey matches Key 763 and rejects non-menu keys`() {
        // 部分魔改电视遥控器将菜单键上报为 763
        val event763 = mockKeyEvent(763)
        val eventBack = mockKeyEvent(android.view.KeyEvent.KEYCODE_BACK)
        val eventDirectionUp = mockKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_UP)

        assertThat(event763.isMenuKey()).isTrue()
        assertThat(eventBack.isMenuKey()).isFalse()
        assertThat(eventDirectionUp.isMenuKey()).isFalse()
    }
}
