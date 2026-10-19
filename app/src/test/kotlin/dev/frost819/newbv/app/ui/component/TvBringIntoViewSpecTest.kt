package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [TvBringIntoViewSpec] 的单元测试。
 *
 * 验证在各种 offset、size 和 containerSize 条件下的滚动距离计算契约：
 * - 处于锚点带时短路返回 0f
 * - 完全可见但偏离锚点时调整到锚点
 * - 位于视口上方 (offset < 0) 时按 offset 滚动
 * - 位于视口下方 (offset + size > containerSize) 时按超出量滚动
 */
@OptIn(ExperimentalFoundationApi::class)
class TvBringIntoViewSpecTest {
    @Test
    fun `returns 0 when item is completely visible and already at target position`() {
        val spec = TvBringIntoViewSpec(pivotFraction = 0.25f)
        val containerSize = 1000f
        val offset = 250f // 1000 * 0.25
        val size = 100f

        val scrollDistance = spec.calculateScrollDistance(offset, size, containerSize)

        assertThat(scrollDistance).isEqualTo(0f)
    }

    @Test
    fun `short-circuits to 0 when delta is within 1px threshold`() {
        val spec = TvBringIntoViewSpec(pivotFraction = 0.25f)
        val containerSize = 1000f
        val offset = 250.8f // delta = 0.8f <= 1f
        val size = 100f

        val scrollDistance = spec.calculateScrollDistance(offset, size, containerSize)

        assertThat(scrollDistance).isEqualTo(0f)
    }

    @Test
    fun `returns delta when item is completely visible but outside 1px threshold`() {
        val spec = TvBringIntoViewSpec(pivotFraction = 0.25f)
        val containerSize = 1000f
        val offset = 400f
        val size = 100f

        val scrollDistance = spec.calculateScrollDistance(offset, size, containerSize)

        // 400 - 250 = 150
        assertThat(scrollDistance).isEqualTo(150f)
    }

    @Test
    fun `returns offset when item is above viewport`() {
        val spec = TvBringIntoViewSpec(pivotFraction = 0.25f)
        val containerSize = 1000f
        val offset = -80f
        val size = 100f

        val scrollDistance = spec.calculateScrollDistance(offset, size, containerSize)

        assertThat(scrollDistance).isEqualTo(-80f)
    }

    @Test
    fun `returns overflow distance when item is below viewport`() {
        val spec = TvBringIntoViewSpec(pivotFraction = 0.25f)
        val containerSize = 1000f
        val offset = 950f
        val size = 100f

        val scrollDistance = spec.calculateScrollDistance(offset, size, containerSize)

        // 950 + 100 - 1000 = 50
        assertThat(scrollDistance).isEqualTo(50f)
    }

    @Test
    fun `respects custom pivotFraction`() {
        val spec = TvBringIntoViewSpec(pivotFraction = 0.5f)
        val containerSize = 1000f
        val offset = 500f // target = 500
        val size = 100f

        val scrollDistance = spec.calculateScrollDistance(offset, size, containerSize)

        assertThat(scrollDistance).isEqualTo(0f)
    }
}
