package dev.frost819.newbv.core.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BVShapeTest {
    private val density = Density(density = 2f)
    private val size = Size(200f, 100f)

    @Test
    fun `shapes match standard design token specifications`() {
        val smallOutline = BVShape.small.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(smallOutline).isNotNull()

        val mediumOutline = BVShape.medium.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(mediumOutline).isNotNull()

        val largeOutline = BVShape.large.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(largeOutline).isNotNull()

        val pillOutline = BVShape.pill.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(pillOutline).isNotNull()
    }

    @Test
    fun `material theme shapes are properly wired`() {
        assertThat(BVShape.tv.small).isEqualTo(BVShape.small)
        assertThat(BVShape.tv.medium).isEqualTo(BVShape.medium)
        assertThat(BVShape.tv.large).isEqualTo(BVShape.large)

        assertThat(BVShape.common.small).isEqualTo(BVShape.small)
        assertThat(BVShape.common.medium).isEqualTo(BVShape.medium)
        assertThat(BVShape.common.large).isEqualTo(BVShape.large)
    }
}
