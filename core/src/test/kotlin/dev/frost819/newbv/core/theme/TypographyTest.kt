package dev.frost819.newbv.core.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TypographyTest {
    @Test
    fun `tv typography enforces 10-foot legibility with proper font weights`() {
        assertThat(BVTypography.tv.titleLarge.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(BVTypography.tv.titleMedium.fontWeight).isEqualTo(FontWeight.SemiBold)
        assertThat(BVTypography.tv.titleSmall.fontWeight).isEqualTo(FontWeight.SemiBold)

        assertThat(BVTypography.tv.bodyMedium.fontWeight).isEqualTo(FontWeight.Medium)
        assertThat(BVTypography.tv.bodySmall.fontWeight).isEqualTo(FontWeight.Medium)

        assertThat(BVTypography.tv.labelLarge.fontWeight).isEqualTo(FontWeight.Medium)
        assertThat(BVTypography.tv.labelMedium.fontWeight).isEqualTo(FontWeight.Medium)
        assertThat(BVTypography.tv.labelSmall.fontWeight).isEqualTo(FontWeight.Medium)
    }

    @Test
    fun `common typography matches tv typography font weights`() {
        assertThat(BVTypography.common.titleMedium.fontWeight).isEqualTo(FontWeight.SemiBold)
        assertThat(BVTypography.common.bodyMedium.fontWeight).isEqualTo(FontWeight.Medium)
        assertThat(BVTypography.common.labelMedium.fontWeight).isEqualTo(FontWeight.Medium)
    }

    @Test
    fun `timecode and tabular function enable tabular figures`() {
        assertThat(BVTypography.timecode.fontFeatureSettings).contains("tnum")

        val modified = BVTypography.tabular(BVTypography.tv.bodyMedium)
        assertThat(modified.fontFeatureSettings).contains("tnum")
        assertThat(modified.fontSize).isEqualTo(14.sp)
        assertThat(modified.fontWeight).isEqualTo(FontWeight.Medium)
    }
}
