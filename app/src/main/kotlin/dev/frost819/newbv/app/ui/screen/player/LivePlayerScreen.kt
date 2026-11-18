package dev.frost819.newbv.app.ui.screen.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import dev.frost819.newbv.app.MainActivity
import dev.frost819.newbv.app.ui.component.player.LivePlayerController
import dev.frost819.newbv.app.ui.component.rememberDoublePressExit
import dev.frost819.newbv.app.viewmodel.live.LivePlayerState
import dev.frost819.newbv.app.viewmodel.live.LivePlayerViewModel
import dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
import dev.frost819.newbv.danmaku.component.DanmakuPlayerCompose
import dev.frost819.newbv.player.BvVideoPlayer

/**
 * 直播播放器页面。
 *
 * 使用 [LivePlayerController] 作为根控制器，整合 [LivePlayerViewModel]（播放控制）
 * 和 [DanmakuViewModel]（弹幕配置）。
 *
 * UI 层负责协调两者：
 * - 播放/暂停 → 同步弹幕播放/暂停
 * - 弹幕开关 → 调用 DanmakuViewModel.toggleDanmaku()
 * - 弹幕设置 → 调用 DanmakuViewModel.updateDanmakuState()
 */
@Composable
fun LivePlayerScreen(
    navController: NavController,
    viewModel: LivePlayerViewModel,
    danmakuViewModel: DanmakuViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val danmakuState by danmakuViewModel.danmakuState.collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()
    val videoPlayer = viewModel.videoPlayer
    val danmakuPlayer = danmakuViewModel.danmakuPlayer

    val danmakuEnabled = danmakuState.enabledTypes.isNotEmpty()

    // 双击退出：TV 遥控器（Controller onBack）和非 TV（BackHandler）共用同一计时器
    val handleBack =
        rememberDoublePressExit(
            onExit = { navController.popBackStack() },
            message = "再按一次退出播放",
        )
    BackHandler { handleBack() }

    // 屏保桥接：全局屏保由 MainActivity 根层 GlobalScreensaverOverlay 统一驱动，
    // 本页只声明「允许屏保 / 抑制屏保」的状态。
    // 采用黑名单策略：仅 Paused/Ended 才允许屏保，其余状态（Idle/Loading/Buffering/Error）
    // 一律抑制，避免全黑屏保遮挡加载状态、错误提示与重试入口。
    // LivePlayerState 虽定义了 Buffering 枚举，但 VM 实际也通过独立的 isBuffering 标志报缓冲，
    // 因此两个信号都需纳入“活跃”判定，避免暂停后缓冲看门狗未清除时屏保误强。
    // 转场竞态：新页 B 的 LaunchedEffect 可能先于旧页 A 的 onDispose 执行，若协调器只用单个布尔量，
    // 最后写入者 A 的 release 会覆盖 B 的「播放中」声明。因此用 owner token 聚合：
    // 每个页面实例携带自己的 token 注册/释放，互不覆盖。
    // dispose 时 releaseContentPlaying，避免退出直播页后主页失去屏保能力。
    val screensaverCoordinator = (LocalContext.current as? MainActivity)?.screensaverCoordinator
    val screensaverOwner = remember { Any() }
    LaunchedEffect(uiState.playerState, uiState.isBuffering, screensaverCoordinator) {
        val allowScreensaver =
            (uiState.playerState == LivePlayerState.Paused || uiState.playerState == LivePlayerState.Ended) &&
                !uiState.isBuffering
        screensaverCoordinator?.setContentPlaying(screensaverOwner, !allowScreensaver)
    }
    DisposableEffect(screensaverCoordinator) {
        onDispose {
            screensaverCoordinator?.releaseContentPlaying(screensaverOwner)
        }
    }

    LaunchedEffect(uiState.playerState) {
        when (uiState.playerState) {
            LivePlayerState.Playing -> danmakuViewModel.play()
            LivePlayerState.Paused, LivePlayerState.Error, LivePlayerState.Ended -> danmakuViewModel.pause()
            else -> {}
        }
    }

    LaunchedEffect(uiState.isBuffering) {
        if (uiState.isBuffering) {
            danmakuViewModel.pause()
        } else if (uiState.playerState == LivePlayerState.Playing) {
            danmakuViewModel.play()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        LivePlayerController(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            isPlaying = uiState.playerState == LivePlayerState.Playing,
            isBuffering = uiState.isBuffering,
            isError = uiState.playerState == LivePlayerState.Error,
            errorMessage = uiState.errorMessage,
            title = uiState.title,
            areaName = uiState.areaName,
            onlineCount = uiState.onlineCount,
            clock = getClock(),
            danmakuEnabled = danmakuEnabled,
            danmakuState = danmakuState,
            availableQualities = uiState.availableQualities,
            currentQuality = uiState.currentQuality,
            availableLines = uiState.availableLines,
            currentLine = uiState.currentLine,
            onBack = handleBack,
            onPlayPause = { viewModel.togglePlayPause() },
            onRefresh = { viewModel.refresh() },
            onToggleDanmaku = { danmakuViewModel.toggleDanmaku() },
            onQualityChange = { viewModel.changeQuality(it) },
            onLineChange = { viewModel.changeLine(it) },
            onDanmakuSettingChange = { danmakuViewModel.updateDanmakuState(it) },
            debugInfo = debugInfo,
        ) {
            if (videoPlayer != null) {
                val aspectRatio =
                    if (videoPlayer.videoWidth > 0 && videoPlayer.videoHeight > 0) {
                        videoPlayer.videoWidth.toFloat() / videoPlayer.videoHeight.toFloat()
                    } else {
                        16f / 9f
                    }
                BvVideoPlayer(
                    modifier = Modifier.align(Alignment.Center).fillMaxHeight().aspectRatio(aspectRatio),
                    videoPlayer = videoPlayer,
                )
            }

            if (danmakuPlayer != null && danmakuEnabled) {
                DanmakuPlayerCompose(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(danmakuState.alpha),
                    danmakuPlayer = danmakuPlayer,
                )
            }
        }
    }
}

/**
 * 获取当前时钟（小时，分钟）。
 */
private fun getClock(): Pair<Int, Int> {
    val calendar = java.util.Calendar.getInstance()
    return calendar.get(java.util.Calendar.HOUR_OF_DAY) to calendar.get(java.util.Calendar.MINUTE)
}
