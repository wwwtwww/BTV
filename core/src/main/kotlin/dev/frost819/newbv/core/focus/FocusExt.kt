package dev.frost819.newbv.core.focus

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.core.interaction.InputMethod
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 获取焦点时显示边框。
 *
 * 仅在 [InputMethod.DPad] 模式下显示焦点边框，
 * 触屏模式下隐藏（触屏点击自带视觉反馈，无需额外焦点提示）。
 *
 * 当 [LocalInteractionTracker] 未注入时，始终显示边框（兼容未接入 tracker 的场景）。
 *
 * @param shape 边框形状，默认 [ShapeDefaults.Large]。
 * @param animate 是否启用呼吸动画。
 * @param color 边框颜色，默认使用当前主题的边框色。
 * @param width 边框宽度，默认 3dp。
 */
fun Modifier.focusedBorder(
    shape: Shape = ShapeDefaults.Large,
    animate: Boolean = false,
    color: Color? = null,
    width: Dp = 3.dp,
): Modifier =
    composed {
        val tracker = LocalInteractionTracker.current
        val resolvedColor = color ?: MaterialTheme.colorScheme.border
        var hasFocus by remember { mutableStateOf(false) }

        val showBorder =
            if (tracker != null) {
                val method by tracker.inputMethod.collectAsState()
                hasFocus && method == InputMethod.DPad
            } else {
                hasFocus
            }

        val infiniteTransition = rememberInfiniteTransition(label = "focused-border-transition")
        val animateColor by infiniteTransition.animateColor(
            initialValue = resolvedColor.copy(alpha = 1f),
            targetValue = resolvedColor.copy(alpha = 0.1f),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "focused-border-color",
        )
        val borderColor =
            if (showBorder) {
                if (animate) animateColor else resolvedColor
            } else {
                Color.Transparent
            }

        onFocusChanged { hasFocus = it.hasFocus }
            .border(width = width, color = borderColor, shape = shape)
    }

/**
 * 未获焦点时缩小，获焦点时恢复原大小。
 *
 * 触屏模式下保持显示缩放效果（D-Pad 用户看到放大，触屏用户看到缩小）。
 * 注意：与 [focusedBorder] 不同，[focusedScale] 不会根据输入方式隐藏。
 *
 * @param scale 未获焦点时的缩放比例，默认 0.9。
 */
fun Modifier.focusedScale(scale: Float = 0.9f): Modifier =
    composed {
        var hasFocus by remember { mutableStateOf(false) }
        val scaleValue by animateFloatAsState(
            targetValue = if (hasFocus) 1f else scale,
            label = "focused-scale",
        )

        onFocusChanged { hasFocus = it.hasFocus }
            .scale(scaleValue)
    }

/**
 * 安全的同步请求焦点扩展。
 *
 * 捕获节点尚未挂载或已销毁时的 [IllegalStateException]，杜绝大屏焦点闪退。
 *
 * @return 是否成功获得焦点
 */
fun FocusRequester.safeRequestFocus(): Boolean =
    runCatching {
        requestFocus()
        true
    }.getOrDefault(false)

/**
 * 具有重试机制的安全异步请求焦点扩展。
 *
 * 处理 Compose 节点尚未完成 Composition 或处于过渡动画中的时序问题。
 *
 * @param scope 协程作用域，在 Main 调度器执行。
 * @param maxRetries 最大重试次数（默认 3 次）。
 * @param retryDelayMs 重试间隔毫秒数（默认 60ms）。
 * @param onSuccess 成功获取焦点后的回调。
 *
 * 示例：
 * `kotlin
 * // 启动异步重试并在生命周期销毁或导航离开时提前取消
 * val focusJob = requester.safeRequestFocus(scope) { showFocusedHighlight() }
 * onDispose { focusJob.cancel() }
 * `
 * @return 负责执行重试的协程 [Job]，调用方可用于取消未完成的重试循环或追踪就绪状态。
 */
fun FocusRequester.safeRequestFocus(
    scope: CoroutineScope,
    maxRetries: Int = 3,
    retryDelayMs: Long = 60,
    onSuccess: (() -> Unit)? = null,
): Job =
    scope.launch(Dispatchers.Main) {
        repeat(maxRetries) { attempt ->
            if (safeRequestFocus()) {
                onSuccess?.invoke()
                return@launch
            }
            if (attempt < maxRetries - 1) {
                delay(retryDelayMs)
            }
        }
    }

/**
 * 改进的请求焦点方法（兼容旧接口）。
 *
 * 首次请求失败后重试，内部代理至 [safeRequestFocus]。
 *
 * @param scope 协程作用域，在 Main 调度器执行。
 * @return 负责执行重试的协程 [Job]。
 */
fun FocusRequester.requestFocus(scope: CoroutineScope): Job =
    safeRequestFocus(scope = scope, maxRetries = 2, retryDelayMs = 100)

/**
 * 电视端焦点丢失兜底修饰符。
 *
 * 当此容器及其子节点全部未获得焦点时（例如列表异步刷新重建、Tab/泳道切换、列表内容清空），
 * 用户按下遥控器任何 D-Pad 导航按键（上下左右或确认键）时，自动将焦点导向 [fallbackRequester]，
 * 避免电视大屏出现"光标消失、遥控器按键无响应假死"的严重体验缺陷。
 *
 * 典型应用场景：
 * 1. 首页/分栏根容器：当列表数据重新加载或由分页滑动导致焦点游离时，用户按键自动兜底恢复到导航栏或置顶分类。
 * 2. 播放器控制器（OSD）与选集抽屉：当抽屉切换或控制层浮现但子项未及时获焦时，兜底回主控制按钮。
 * 3. 对话框（如更新弹窗、评论列表）：当点击空白区或过渡期间失去焦点时，兜底到确定/取消按钮。
 * 4. 历史记录/收藏夹/稍后再看等列表页：当用户清空最后一条数据后，兜底至顶部筛选器或返回按钮。
 *
 * @param fallbackRequester 兜底获焦节点。
 * @param enabled 是否启用兜底（默认 true）。
 */
fun Modifier.dpadFocusFallback(
    fallbackRequester: FocusRequester,
    enabled: Boolean = true,
): Modifier =
    composed {
        if (!enabled) return@composed this
        var hasFocus by remember { mutableStateOf(false) }

        onFocusChanged { hasFocus = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (!hasFocus && event.isKeyDown() && event.isDpadNav()) {
                    if (fallbackRequester.safeRequestFocus()) {
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    }

/**
 * 构建「聚焦反色」的可点击 Surface 配色。
 *
 * TV 遥控器场景焦点必须足够醒目：聚焦时容器切换为
 * [androidx.tv.material3.ColorScheme.inverseSurface]，
 * 内容切换为 [androidx.tv.material3.ColorScheme.inverseOnSurface]，
 * 实现整体明暗反色。未聚焦时使用调用方传入的容器/内容色，
 * 以便表达「已选中 / 已激活」等常态。
 *
 * 注意：Surface 的内容色通过 [androidx.tv.material3.LocalContentColor] 下传，
 * 子组件（[androidx.tv.material3.Text] / [androidx.tv.material3.Icon]）**不要**再硬编码颜色，
 * 否则聚焦时不会随容器一起反色。
 *
 * @param containerColor 未聚焦时的容器色。
 * @param contentColor 未聚焦时的内容色。
 */
@Composable
@ReadOnlyComposable
fun focusInvertedColors(
    containerColor: Color,
    contentColor: Color,
): ClickableSurfaceColors =
    ClickableSurfaceDefaults.colors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )
