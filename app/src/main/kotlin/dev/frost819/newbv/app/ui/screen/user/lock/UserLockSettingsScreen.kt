package dev.frost819.newbv.app.ui.screen.user.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.app.viewmodel.user.LockInputPhase
import dev.frost819.newbv.app.viewmodel.user.UserLockViewModel
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.data.datastore.Prefs

/**
 * 用户锁设置页面。
 *
 * 遵循三阶段状态机流转：
 * 1. [LockInputPhase.VerifyOld]（用户已有密码时）：强制验证原密码；
 * 2. [LockInputPhase.InputNew]：输入新密码（若直接确认则视为清除密码）；
 * 3. [LockInputPhase.ConfirmNew]：二次确认新密码。
 *
 * 密码为 D-Pad 方向按键序列（u/d/l/r），DirectionCenter 确认，Back 回退上一位。
 *
 * @param modifier 修饰符。
 * @param viewModel 用户锁 ViewModel。
 * @param onSaved 保存成功退出回调。
 */
@Composable
fun UserLockSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: UserLockViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
) {
    val currentUid = Prefs.uid
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentUid) {
        viewModel.init(currentUid)
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == LockInputPhase.SaveSuccess) {
            onSaved()
        } else {
            focusRequester.safeRequestFocus()
        }
    }

    BackHandler(enabled = true) {
        if (!viewModel.onBack()) {
            onSaved()
        }
    }

    Surface(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            viewModel.appendDirection("u")
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.appendDirection("d")
                            true
                        }
                        Key.DirectionLeft -> {
                            viewModel.appendDirection("l")
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.appendDirection("r")
                            true
                        }
                        Key.DirectionCenter -> {
                            viewModel.confirm()
                            true
                        }
                        Key.Back -> {
                            if (viewModel.onBack()) {
                                true
                            } else {
                                onSaved()
                                true
                            }
                        }
                        else -> false
                    }
                },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(48.dp),
            ) {
                Text(
                    text =
                        when (uiState.phase) {
                            LockInputPhase.VerifyOld -> stringResource(R.string.lock_input_old)
                            LockInputPhase.InputNew -> stringResource(R.string.lock_set_new)
                            LockInputPhase.ConfirmNew -> stringResource(R.string.lock_confirm_new)
                            LockInputPhase.SaveSuccess -> ""
                        },
                    style = MaterialTheme.typography.displaySmall,
                )

                val displayPassword =
                    uiState.inputPassword
                        .replace("u", "↑")
                        .replace("d", "↓")
                        .replace("l", "←")
                        .replace("r", "→")
                Text(
                    text = displayPassword.ifEmpty { " " },
                    style = MaterialTheme.typography.displayLarge,
                )

                if (uiState.message.isNotEmpty()) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (uiState.phase == LockInputPhase.InputNew) {
                    Text(
                        text = stringResource(R.string.lock_hint_blank_remove),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
