package dev.frost819.newbv.app.ui.screen.player

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.PlaceholderScreen
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.ui.navigation.SeasonPlayerRoute
import dev.frost819.newbv.app.ui.navigation.VideoPlayerRoute
import dev.frost819.newbv.app.ui.state.player.PlayerState
import dev.frost819.newbv.app.viewmodel.live.LivePlayerState
import dev.frost819.newbv.app.viewmodel.player.DanmakuViewModel
import dev.frost819.newbv.app.viewmodel.player.PlayerViewModel
import dev.frost819.newbv.app.viewmodel.player.SubtitleViewModel
import dev.frost819.newbv.app.viewmodel.player.VideoListViewModel

/**
 * 视频播放器页面注册。
 *
 * 从 [VideoPlayerRoute] 提取参数，初始化 PlayerViewModel 和 DanmakuViewModel，
 * 配置全屏 + 隐藏系统栏，然后渲染 [VideoPlayerScreen]。
 */
fun NavGraphBuilder.videoPlayerScreen(navController: NavController) {
    composable<VideoPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<VideoPlayerRoute>()
        val context = LocalContext.current
        val playerViewModel: PlayerViewModel = hiltViewModel()
        val danmakuViewModel: DanmakuViewModel = hiltViewModel()
        val subtitleViewModel: SubtitleViewModel = hiltViewModel()
        val videoListViewModel: VideoListViewModel = hiltViewModel()

        // 初始化播放器状态 + 加载视频资源（顺序执行，避免竞态）
        LaunchedEffect(route.aid) {
            if (!playerViewModel.hasInitializedSession) {
                // 1. 设置播放状态（aid/cid/title 等）
                playerViewModel.init(
                    aid = route.aid,
                    cid = route.cid,
                    epid = route.epid?.toInt(),
                    title = route.title,
                    lastPlayed = 0,
                    fromSeason = false,
                    subType = 0,
                    seasonId = 0,
                    authorName = "",
                )
                // 2. 设置当前视频 aid（过滤 repository 数据，防止叠加打开错位）
                videoListViewModel.setCurrentAid(route.aid)
                // 3. 初始化 ExoPlayer（同步，确保 videoPlayer 就绪）
                playerViewModel.initVideoPlayer(context)
                // 4. 初始化弹幕播放器
                danmakuViewModel.init()
                // 5. 加载视频详情（获取正确 cid、相关视频、历史进度）
                //    仅当历史 cid 与当前 cid 一致时才应用断点续播
                playerViewModel.loadVideoDetail(route.aid, route.bvid)
                // 6. 使用正确的 cid 加载弹幕、字幕（route.cid 可能为 0，需从详情获取）
                //    弹幕分段加载按历史进度（秒 → 毫秒）定位初始分段
                val actualCid = playerViewModel.uiState.value.cid
                danmakuViewModel.loadDanmaku(
                    aid = route.aid,
                    cid = actualCid,
                    initialPositionMs =
                        playerViewModel.uiState.value.lastPlayed
                            .toLong() * 1000,
                )
                danmakuViewModel.loadDanmakuMask(route.aid, actualCid)
                subtitleViewModel.loadSubtitleList(route.aid, actualCid)
                // 7. 获取播放地址并开始播放
                playerViewModel.loadVideoWithResources()
            } else {
                // 会话已存在（如从 UP 空间/详情页返回），恢复播放并保持当前集数与进度
                playerViewModel.resumePlayback()
            }
        }

        // 离开组合时暂停播放并保存当前进度（不销毁播放器，出栈时由 ViewModel.onCleared 彻底释放）
        DisposableEffect(Unit) {
            onDispose {
                playerViewModel.pausePlayback()
                playerViewModel.syncProgressToApplicationScope()
            }
        }

        // 全屏 + 隐藏系统栏；采用黑名单策略对齐屏保门控：
        // 仅 Paused/Ended 且非缓冲时允许系统熄屏，其他状态（含 Error/Ready/Loading）一律保持常亮，
        // 避免错误提示或加载态下屏幕自动熄灭导致用户无法操作。
        val playerUiState by playerViewModel.uiState.collectAsState()
        val allowScreenOff =
            (playerUiState.playerState == PlayerState.Paused || playerUiState.playerState == PlayerState.Ended) &&
                !playerUiState.isBuffering
        PlayerWindowEffect(keepScreenOn = !allowScreenOff)

        VideoPlayerScreen(
            navController = navController,
            playerViewModel = playerViewModel,
            danmakuViewModel = danmakuViewModel,
            subtitleViewModel = subtitleViewModel,
            videoListViewModel = videoListViewModel,
        )
    }
}

fun NavGraphBuilder.seasonPlayerScreen(navController: NavController) {
    composable<SeasonPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<SeasonPlayerRoute>()
        PlaceholderScreen(title = "Season Player (epid=${route.epid}, sid=${route.sid})")
    }
}

fun NavGraphBuilder.livePlayerScreen(navController: NavController) {
    composable<LivePlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LivePlayerRoute>()
        val context = LocalContext.current
        val viewModel: dev.frost819.newbv.app.viewmodel.live.LivePlayerViewModel = hiltViewModel()
        val danmakuViewModel: DanmakuViewModel = hiltViewModel()

        LaunchedEffect(route.roomId) {
            viewModel.init(
                roomId = route.roomId,
                title = route.title,
                cover = route.cover,
            )
            danmakuViewModel.init()
            viewModel.initVideoPlayer(context)
            viewModel.loadLive(route.roomId, danmakuViewModel.danmakuPlayer)
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.detachPlayer()
                danmakuViewModel.release()
            }
        }

        // 全屏 + 隐藏系统栏；采用黑名单策略对齐屏保门控：
        // 仅 Paused/Ended 且非缓冲时允许系统熄屏，其他状态（含 Error/Loading/Idle）一律保持常亮，
        // 避免错误提示或加载态下屏幕自动熄灭导致用户无法操作。
        val liveUiState by viewModel.uiState.collectAsState()
        val allowScreenOff =
            (liveUiState.playerState == LivePlayerState.Paused || liveUiState.playerState == LivePlayerState.Ended) &&
                !liveUiState.isBuffering
        PlayerWindowEffect(keepScreenOn = !allowScreenOff)

        LivePlayerScreen(
            navController = navController,
            viewModel = viewModel,
            danmakuViewModel = danmakuViewModel,
        )
    }
}

/**
 * 播放器页面通用窗口效果。
 *
 * 进入时全屏 + 隐藏系统栏，离开时恢复系统栏。
 * 屏幕常亮采用**黑名单策略**：仅当 [keepScreenOn] 为 false（即 Paused/Ended 且非缓冲）时
 * 清除 [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]，其余状态一律保持常亮。
 * 这样做是为了与屏保门控对齐——Error/Ready/Loading 态下如果熄屏，
 * 用户将无法看到错误提示或加载状态，体验严重受损。
 *
 * 全屏与常亮拆成两个 [DisposableEffect]：全屏仅在进入/离开时切换，
 * 常亮随 [keepScreenOn] 变化，避免暂停时反复 show/hide 系统栏造成闪烁。
 *
 * @param keepScreenOn 是否需要保持屏幕常亮。由调用方按黑名单策略计算：
 *   仅 Paused/Ended（且非 buffering）时传 false，其余一律 true。
 */
@Composable
private fun PlayerWindowEffect(keepScreenOn: Boolean) {
    val window = (LocalContext.current as? Activity)?.window

    // 全屏 + 隐藏系统栏
    DisposableEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowInsetsControllerCompat(it, it.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 屏幕常亮（黑名单策略：仅 Paused/Ended 且非缓冲时由调用方传入 false，其余保持常亮）
    DisposableEffect(window, keepScreenOn) {
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
