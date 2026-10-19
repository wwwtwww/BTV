package dev.frost819.newbv.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 封装了 TV 焦点定轴逻辑的 [LazyColumn]。
 *
 * TV 端 D-Pad 导航时，聚焦的 item 会自动滚动到屏幕上方 [pivotFraction] 比例处，
 * 避免焦点跳跃或被边缘遮挡。默认 0.25f，符合 10-foot 大屏视觉标准。
 *
 * @param modifier 外部修饰符。
 * @param state 滚动列表状态 [LazyListState]。
 * @param contentPadding 列表内边距。
 * @param verticalArrangement 垂直子项排列间距。
 * @param pivotFraction 焦点 item 在屏幕上的停留位置比例 (0.0 - 1.0)。
 * @param content 列表项 DSL 构造 lambda。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    pivotFraction: Float = 0.25f,
    content: LazyListScope.() -> Unit,
) {
    val bringIntoViewSpec = remember(pivotFraction) { TvBringIntoViewSpec(pivotFraction) }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides bringIntoViewSpec,
    ) {
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** 避免亚像素精度误差导致列表微小抖动的死区阈值（像素）。 */
internal const val SCROLL_DEADZONE_PX = 1f

/**
 * TV 端焦点定轴滚动的 [BringIntoViewSpec] 实现。
 *
 * 遵循 TV 滚动契约：
 * 1. 当 item 已完全处于屏幕可见范围内时，计算与目标锚点（pivot）的偏离量，并在接近时短路为 0f 避免抖动；
 * 2. 当 item 位于视口上方（offset < 0）时，只滚动 offset 使其完全展示；
 * 3. 当 item 位于视口下方时，考虑 item 尺寸 [size]，只滚动必要距离使其完全可见。
 *
 * @param pivotFraction 焦点 item 在屏幕上的停留位置比例 (0.0 - 1.0)。
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TvBringIntoViewSpec(
    private val pivotFraction: Float = 0.25f,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        val target = containerSize * pivotFraction
        if (offset >= 0f && offset + size <= containerSize) {
            val delta = offset - target
            return if (kotlin.math.abs(delta) <= SCROLL_DEADZONE_PX) 0f else delta
        }
        if (offset < 0f) return offset
        return offset + size - containerSize
    }
}
