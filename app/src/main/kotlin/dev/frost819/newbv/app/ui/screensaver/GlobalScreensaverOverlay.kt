package dev.frost819.newbv.app.ui.screensaver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.frost819.newbv.app.MainActivity
import dev.frost819.newbv.app.ui.component.player.ScreensaverOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

/**
 * 应用级全局屏保 Overlay。
 *
 * 挂载于 [MainActivity] 根 Box 顶层，跨所有页面（主页 / 详情 / 播放器 / 设置等）统一驱动屏保。
 * 通过轮询 [ScreensaverCoordinator.elapsedSinceLastInteraction] 与 [ScreensaverCoordinator.timeoutMs]
 * 判定超时；超时到达且 [ScreensaverCoordinator.isScreensaverAllowed] 为 `true` 时激活屏保渲染。
 *
 * 交互重置分两条输入路径覆盖（Android 平台行为差异）：
 * - 触摸/轨迹球/通用运动事件走 Activity 标准回调 [MainActivity.onUserInteraction]；
 * - 遥控器按键（D-pad/确认/返回等）**不会**触发 [MainActivity.onUserInteraction]，
 *   由 [MainActivity.dispatchKeyEvent] 显式调用 [ScreensaverCoordinator.onUserInteraction] 补齐。
 * 两条路径最终都会归零空闲计时，Overlay 侧无需重复拦键。
 * 屏保激活时按任意键唤醒——上述回调先把计时归零，随后本 Composable 消费 dismiss 事件将
 * 激活状态复位，Overlay 撤下；下一轮 500ms 轮询重新从零开始。
 *
 * 渲染部分完全复用 [ScreensaverOverlay]（防烧屏微光漂浮 + 时钟 + 日期），
 * 保持与旧播放页局部屏保一致的视觉与拦焦点行为。
 * 全局屏保不绑定具体播放内容，因此不展示标题。
 *
 * @param coordinator 应用级屏保协调器，提供超时/是否允许/最近交互时间戳等状态。
 * @param modifier 附加到内部 [ScreensaverOverlay] 的外部修饰符。
 * @see ScreensaverCoordinator
 * @see ScreensaverOverlay
 */
@Composable
fun GlobalScreensaverOverlay(
    coordinator: ScreensaverCoordinator,
    modifier: Modifier = Modifier,
) {
    val isAllowed by coordinator.isScreensaverAllowed.collectAsStateWithLifecycle()
    val timeoutMs by coordinator.timeoutMs.collectAsStateWithLifecycle()

    // 使用 screensaverActive 而非 isActive，避免与 kotlinx.coroutines.isActive 扩展属性同名遮蔽
    var screensaverActive by remember { mutableStateOf(false) }

    // 计时循环：isAllowed / timeoutMs 变化时重启；不允许时立即撤下屏保。
    // 每次循环间隔 500ms 检查空闲时长，超时则激活屏保并挂起等待用户唤醒。
    LaunchedEffect(isAllowed, timeoutMs) {
        if (!isAllowed || timeoutMs <= 0L) {
            screensaverActive = false
            return@LaunchedEffect
        }
        // 重启计时基准，避免设置变更后立即被历史空闲时长误触发
        coordinator.onUserInteraction()
        while (isActive) {
            delay(500L)
            if (coordinator.elapsedSinceLastInteraction() >= timeoutMs) {
                screensaverActive = true
                // 屏保展示期间挂起轮询协程以节省 CPU，直到 dismiss 将 screensaverActive 复位
                snapshotFlow { screensaverActive }.first { !it }
                // 唤醒后重新从零计时（输入回调已刷新时间戳，此处防御性再置一次）
                coordinator.onUserInteraction()
            }
        }
    }

    if (screensaverActive) {
        ScreensaverOverlay(
            modifier = modifier,
            onDismiss = { screensaverActive = false },
        )
    }
}
