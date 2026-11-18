/**
 * 屏保渲染层。
 *
 * 本文件现在**仅提供纯渲染 Composable**（[ScreensaverOverlay]）：负责微光时钟、日期
 * 的绘制，30 秒防烧屏漂移，以及激活期间的拦焦点与唤醒回调。
 *
 * 屏保的「何时激活」决策不属于本文件：无操作计时、超时判定与播放状态允许性已由应用级
 * `ScreensaverCoordinator`（Hilt Singleton）与根层 `GlobalScreensaverOverlay` 统一驱动。
 * 旧的局部状态机（`ScreensaverState` / `rememberScreensaverState`）随播放页局部屏保一起移除。
 */
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.safeRequestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 防烧屏微光漂浮屏保组件（纯渲染）。
 *
 * 全黑背景（OLED 像素彻底熄灭），中心区域展示微光时间与日期星期，
 * 每隔 30 秒平滑漂移变换屏幕位置，避免局部像素长期发光导致烧屏。
 *
 * 顶层拦截所有遥控器按键（D-Pad、确认、返回等），任意按键触发 [onDismiss] 并消费事件。
 * 组件本身不做任何超时判定：是否展示、何时撤下全部由调用方（根层 `GlobalScreensaverOverlay`）决定。
 *
 * @param modifier 外部修饰符。
 * @param onDismiss 唤醒退出屏保回调。
 */
@Composable
fun ScreensaverOverlay(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // 自动请求焦点以拦截遥控器按键
    LaunchedEffect(Unit) {
        focusRequester.safeRequestFocus()
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
        }
    }
}
