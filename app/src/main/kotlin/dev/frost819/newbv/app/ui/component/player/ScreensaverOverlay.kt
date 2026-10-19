package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 屏保状态管理器。
 *
 * 跟踪无操作交互时间并在超时后激活屏保。用户交互时可调用 [onUserInteraction] 重置计时。
 *
 * 设计边界约定：当前屏保针对 10-foot TV 遥控器交互标准设计，无操作倒计时主要由遥控器按键与
 * OSD 控制器交互驱动；在纯触摸设备上的手势操作不会自动推迟倒计时（TV 场景无影响）。
 * 激活屏保后，按任意遥控器按键或点击屏幕均可立即退出。
 *
 * @property isScreensaverActive 屏保是否处于激活展示状态。
 */
@Stable
class ScreensaverState internal constructor(
    initialActive: Boolean = false,
    private val timeProvider: () -> Long = System::currentTimeMillis,
) {
    /** 屏保是否处于激活展示状态。 */
    var isScreensaverActive by mutableStateOf(initialActive)
        internal set

    /** 最近一次用户交互的时间戳（毫秒）。 */
    var lastInteractionTime by mutableLongStateOf(timeProvider())
        internal set

    /**
     * 收到用户主动交互（按键、触屏、手势等）时调用，重置无操作倒计时。
     */
    fun onUserInteraction() {
        lastInteractionTime = timeProvider()
    }

    /**
     * 退出屏保。
     *
     * 重置激活状态，并将交互时间更新为当前时间，使得屏保退出后重新开始计时。
     */
    fun dismissScreensaver() {
        isScreensaverActive = false
        lastInteractionTime = timeProvider()
    }
}

/**
 * 创建并记住 [ScreensaverState]，随播放状态与超时设置自动调度倒计时。
 *
 * @param isPausedOrEnded 当前播放器是否处于暂停或播放结束状态。
 * @param timeoutMs 超时时长（毫秒）。若为 0 或负数则关闭屏保计时。
 */
@Composable
fun rememberScreensaverState(
    isPausedOrEnded: Boolean,
    timeoutMs: Long,
): ScreensaverState {
    val state = remember { ScreensaverState() }

    LaunchedEffect(isPausedOrEnded, timeoutMs) {
        if (!isPausedOrEnded || timeoutMs <= 0L) {
            state.isScreensaverActive = false
            return@LaunchedEffect
        }
        state.lastInteractionTime = System.currentTimeMillis()
        while (isActive) {
            val elapsed = System.currentTimeMillis() - state.lastInteractionTime
            val remaining = timeoutMs - elapsed
            if (remaining <= 0) {
                state.isScreensaverActive = true
                // 屏保展示期间挂起协程，暂停每秒轮询以节省 CPU；
                // 直到用户按键退出屏保（isScreensaverActive 重置为 false）再恢复并重置计时
                snapshotFlow { state.isScreensaverActive }.first { !it }
                state.lastInteractionTime = System.currentTimeMillis()
            } else {
                delay(remaining.coerceAtLeast(500L))
            }
        }
    }

    return state
}

/**
 * 防烧屏微光漂浮屏保组件。
 *
 * 全黑背景（OLED 像素彻底熄灭），中心区域展示微光时间、日期与当前视频标题，
 * 每隔 30 秒平滑漂移变换屏幕位置，避免局部像素长期发光导致烧屏。
 *
 * 顶层拦截所有遥控器按键（D-Pad、确认、返回等），任意按键触发 [onDismiss] 并消费事件。
 *
 * @param modifier 外部修饰符。
 * @param title 当前视频或直播标题。
 * @param onDismiss 唤醒退出屏保回调。
 */
@Composable
fun ScreensaverOverlay(
    modifier: Modifier = Modifier,
    title: String = "",
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // 自动请求焦点以拦截遥控器按键
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 时间与日期格式化
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
    var currentTimeStr by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDateStr by remember { mutableStateOf(dateFormat.format(Date())) }

    // 时间定时刷新（每秒检查一次）
    LaunchedEffect(Unit) {
        while (isActive) {
            val now = Date()
            currentTimeStr = timeFormat.format(now)
            currentDateStr = dateFormat.format(now)
            delay(1000L)
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    when (event.type) {
                        KeyEventType.KeyDown -> true
                        KeyEventType.KeyUp -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                }.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onDismiss()
                },
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        // 安全活动区域：水平 10%~60%，垂直 10%~65%
        val minX = (widthPx * 0.1f).toInt().coerceAtLeast(0)
        val maxX = (widthPx * 0.6f).toInt().coerceAtLeast(minX)
        val minY = (heightPx * 0.1f).toInt().coerceAtLeast(0)
        val maxY = (heightPx * 0.65f).toInt().coerceAtLeast(minY)

        var targetOffset by remember {
            mutableStateOf(
                IntOffset(
                    x = if (maxX > minX) Random.nextInt(minX, maxX) else minX,
                    y = if (maxY > minY) Random.nextInt(minY, maxY) else minY,
                ),
            )
        }

        // 每隔 30 秒平滑移动到新坐标
        LaunchedEffect(minX, maxX, minY, maxY) {
            while (isActive) {
                delay(30_000L)
                val newX = if (maxX > minX) Random.nextInt(minX, maxX) else minX
                val newY = if (maxY > minY) Random.nextInt(minY, maxY) else minY
                targetOffset = IntOffset(newX, newY)
            }
        }

        val animatedOffset by animateIntOffsetAsState(
            targetValue = targetOffset,
            animationSpec =
                tween(
                    durationMillis = 3500,
                    easing = FastOutSlowInEasing,
                ),
            label = "ScreensaverDriftAnimation",
        )

        Column(
            modifier =
                Modifier
                    .offset { animatedOffset }
                    .widthIn(max = 480.dp)
                    .padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // 微光数字时钟（粗体，低透明度）
            Text(
                text = currentTimeStr,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 日期与星期
            Text(
                text = currentDateStr,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.4f),
            )

            // 当前视频/直播标题
            if (title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
