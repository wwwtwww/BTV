package dev.frost819.newbv.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.frost819.newbv.app.ui.navigation.AppNavHost
import dev.frost819.newbv.app.ui.navigation.HomeRoute
import dev.frost819.newbv.app.ui.screensaver.GlobalScreensaverOverlay
import dev.frost819.newbv.app.ui.screensaver.ScreensaverCoordinator
import dev.frost819.newbv.core.interaction.InteractionTracker
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.SystemBarsEffect
import dev.frost819.newbv.core.theme.ThemeMode
import dev.frost819.newbv.data.datastore.Prefs
import javax.inject.Inject

/**
 * 主 Activity（单 Activity 架构）。
 *
 * 职责：
 * - SplashScreen 显示
 * - 交互模式追踪（触屏/遥控器）
 * - Navigation 宿主（[AppNavHost]）
 * - 主题模式 + density 从 Prefs 实时读取
 * - 全局屏保驱动：通过 [ScreensaverCoordinator] 追踪用户交互，配合根层
 *   [GlobalScreensaverOverlay] 在无操作超时后拉起屏保
 *
 * 所有页面通过 Navigation-Compose 导航，不启动新 Activity。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val logger = Loggers.get("MainActivity")

    @Inject
    lateinit var interactionTracker: InteractionTracker

    @Inject
    lateinit var screensaverCoordinator: ScreensaverCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by Prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = Prefs.themeMode)
            val density by Prefs.densityFlow.collectAsStateWithLifecycle(initialValue = Prefs.density)

            val coreThemeMode =
                when (themeMode) {
                    dev.frost819.newbv.data.datastore.ThemeMode.FollowSystem -> ThemeMode.FollowSystem
                    dev.frost819.newbv.data.datastore.ThemeMode.Dark -> ThemeMode.Dark
                    dev.frost819.newbv.data.datastore.ThemeMode.Light -> ThemeMode.Light
                }

            BVTheme(themeMode = coreThemeMode, density = density) {
                SystemBarsEffect()
                CompositionLocalProvider(
                    LocalInteractionTracker provides interactionTracker,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        // 根层 Box：AppNavHost 在底，全局屏保 Overlay 在顶，跨所有页面统一驱动
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppNavHost(startDestination = HomeRoute)
                            GlobalScreensaverOverlay(coordinator = screensaverCoordinator)
                        }
                    }
                }
            }
        }
    }

    /**
     * Activity 标准回调：仅在触摸/轨迹球/通用运动事件路径被系统回调，
     * **不会**在 [dispatchKeyEvent] 路径触发（Android 平台行为）。
     *
     * 用作全局屏保的空闲计时基准；遥控器按键的重置在 [dispatchKeyEvent] 中显式补齐。
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        screensaverCoordinator.onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        // 从后台/待机恢复时视为一次交互，避免刚回到前台就被历史空闲时长误触发屏保
        screensaverCoordinator.onUserInteraction()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            interactionTracker.onTouch()
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            interactionTracker.onDpadKey()
            // onUserInteraction() 不会在按键路径触发，TV 交互以遥控器按键为主，
            // 必须在此显式重置屏保空闲计时，否则按键操作无法阻止屏保拉起
            screensaverCoordinator.onUserInteraction()
            logger.info { "[INPUT] keyDown keyCode=${KeyEvent.keyCodeToString(event.keyCode)}" }
        }
        return super.dispatchKeyEvent(event)
    }
}
