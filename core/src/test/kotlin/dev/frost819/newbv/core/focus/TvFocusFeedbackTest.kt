package dev.frost819.newbv.core.focus

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.core.theme.BVShape
import org.junit.jupiter.api.Test

class TvFocusFeedbackTest {
    @Test
    fun `tv focus defaults follow 10-foot design standards`() {
        assertThat(TvFocusDefaults.FOCUSED_SCALE).isEqualTo(1.06f)
        assertThat(TvFocusDefaults.borderWidth).isEqualTo(2.5.dp)
        assertThat(TvFocusDefaults.glowRadius).isEqualTo(8.dp)
    }

    @Test
    fun `tvFocusFeedback modifier chains without exceptions`() {
        val modifier = Modifier.tvFocusFeedback(
            shape = BVShape.large,
            focusedScale = 1.06f,
            borderWidth = 2.5.dp,
        )
        assertThat(modifier).isNotNull()
    }
}
