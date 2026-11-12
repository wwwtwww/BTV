package dev.frost819.newbv.app.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户锁输入状态机阶段。
 */
enum class LockInputPhase {
    /** 验证已有旧密码阶段。 */
    VerifyOld,

    /** 输入新密码阶段。 */
    InputNew,

    /** 二次确认新密码阶段。 */
    ConfirmNew,

    /** 密码已成功写入持久化阶段。 */
    SaveSuccess,

    /**
     * 密码写入失败阶段。
     *
     * 由保存流程在数据库写入抛出异常时进入；异常详情只记录到日志，不向大屏用户暴露，
     * 界面文案固定由 `R.string.lock_save_failed` 呈现；
     * 按确认键可复用待写入密码重试，按方向键则回到发起保存前的阶段继续编辑。
     */
    SaveFailed,
}

/**
 * 用户锁设置 UI 状态。
 *
 * @property currentUid 用户 UID。
 * @property hasExistingLock 用户当前是否已配置密码锁。
 * @property phase 当前输入阶段。
 * @property inputPassword 当前输入的按键序列（u/d/l/r 字符串）。
 * @property newPassword 暂存的新密码字符串。
 * @property message 界面提示或错误消息。
 * @property isSaving 是否正在执行数据库写入。
 */
data class UserLockUiState(
    val currentUid: Long = 0L,
    val hasExistingLock: Boolean = false,
    val phase: LockInputPhase = LockInputPhase.InputNew,
    val inputPassword: String = "",
    val newPassword: String = "",
    val message: String = "",
    val isSaving: Boolean = false,
)

/**
 * 用户锁设置 ViewModel。
 *
 * 负责管理用户方向键密码锁的验证、设置与持久化流程。
 * 遵循严格的状态机流转，防止已有密码时绕过验证直接修改，
 * 并在数据库操作完成后才派发成功状态。
 *
 * @param accountRepository 账户数据仓库。
 */
@HiltViewModel
class UserLockViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UserLockUiState())
        val uiState: StateFlow<UserLockUiState> = _uiState.asStateFlow()

        private val logger = Loggers.get("UserLockViewModel")

        private var existingLockPassword = ""

        /**
         * 发起本次保存时所处的阶段。
         *
         * 保存失败进入 [LockInputPhase.SaveFailed] 后，用户重新输入时回到该阶段，
         * 避免失败态把输入流程卡死在无法继续的分支上。
         */
        private var saveSourcePhase = LockInputPhase.InputNew

        /**
         * 最近一次待写入的密码串，`null` 表示没有等待重试的保存操作。
         *
         * [LockInputPhase.SaveFailed] 下按确认键时直接复用该值重试，无需用户重新输入。
         */
        private var pendingLock: String? = null

        /**
         * 初始化用户锁状态。
         *
         * 读取用户是否已存在锁密码：
         * - 若已设置，则进入 [LockInputPhase.VerifyOld] 阶段；
         * - 若未设置，则直接进入 [LockInputPhase.InputNew] 阶段。
         *
         * @param uid 目标用户 UID。
         */
        fun init(uid: Long) {
            viewModelScope.launch {
                val user = accountRepository.findUserByUid(uid)
                val lock = user?.lock.orEmpty()
                existingLockPassword = lock
                val hasLock = lock.isNotEmpty()

                _uiState.update {
                    it.copy(
                        currentUid = uid,
                        hasExistingLock = hasLock,
                        phase = if (hasLock) LockInputPhase.VerifyOld else LockInputPhase.InputNew,
                        inputPassword = "",
                        newPassword = "",
                        message = "",
                        isSaving = false,
                    )
                }
            }
        }

        /**
         * 追加一个方向按键。
         *
         * @param direction 按键方向标记（"u" / "d" / "l" / "r"）。
         */
        fun appendDirection(direction: String) {
            if (_uiState.value.isSaving || _uiState.value.phase == LockInputPhase.SaveSuccess) return
            // 失败态下继续输入视为放弃重试，回到发起保存前的阶段重新编辑
            restorePhaseIfSaveFailed()
            _uiState.update {
                it.copy(
                    inputPassword = it.inputPassword + direction,
                    message = "",
                )
            }
        }

        /**
         * 删除最后输入的按键（正向回退）。
         *
         * 严格使用 [dropLast]，防止从头部删除造成倒序错位。
         */
        fun deleteLastDirection() {
            if (_uiState.value.isSaving || _uiState.value.phase == LockInputPhase.SaveSuccess) return
            restorePhaseIfSaveFailed()
            _uiState.update {
                it.copy(
                    inputPassword = it.inputPassword.dropLast(1),
                    message = "",
                )
            }
        }

        /**
         * 处理确认键（DirectionCenter）按下事件。
         *
         * 根据当前所处阶段执行校验或持久化：
         * 1. [LockInputPhase.VerifyOld]：核对原密码，核对通过进入 [LockInputPhase.InputNew]，否则提示错误并清空；
         * 2. [LockInputPhase.InputNew]：若输入为空则清除密码锁，否则暂存并进入 [LockInputPhase.ConfirmNew]；
         * 3. [LockInputPhase.ConfirmNew]：核对与新密码是否一致，一致则异步持久化并在完成后进入 [LockInputPhase.SaveSuccess]；
         * 4. [LockInputPhase.SaveFailed]：复用上次的密码串重试写入。
         */
        fun confirm() {
            val state = _uiState.value
            if (state.isSaving || state.phase == LockInputPhase.SaveSuccess) return

            when (state.phase) {
                LockInputPhase.VerifyOld -> {
                    if (state.inputPassword == existingLockPassword) {
                        _uiState.update {
                            it.copy(
                                phase = LockInputPhase.InputNew,
                                inputPassword = "",
                                message = "",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                inputPassword = "",
                                message = "密码错误",
                            )
                        }
                    }
                }

                LockInputPhase.InputNew -> {
                    val input = state.inputPassword
                    if (input.isEmpty()) {
                        // 清空密码锁
                        saveLockInternal(state.currentUid, "", LockInputPhase.InputNew)
                    } else {
                        _uiState.update {
                            it.copy(
                                phase = LockInputPhase.ConfirmNew,
                                newPassword = input,
                                inputPassword = "",
                                message = "",
                            )
                        }
                    }
                }

                LockInputPhase.ConfirmNew -> {
                    if (state.inputPassword == state.newPassword) {
                        saveLockInternal(state.currentUid, state.inputPassword, LockInputPhase.ConfirmNew)
                    } else {
                        _uiState.update {
                            it.copy(
                                phase = LockInputPhase.InputNew,
                                newPassword = "",
                                inputPassword = "",
                                message = "两次密码不一致",
                            )
                        }
                    }
                }

                LockInputPhase.SaveSuccess -> {
                    // 已完成，忽略
                }

                LockInputPhase.SaveFailed -> retrySave()
            }
        }

        /**
         * 处理返回键逻辑。
         *
         * @return 是否已在 ViewModel 内部消费该返回事件；返回 false 表示可退出页面。
         */
        fun onBack(): Boolean {
            val state = _uiState.value
            if (state.isSaving) return true

            // SaveFailed 阶段一次 Back 即回退原阶段并清空输入，避免用户需多次 Back 才退出
            if (state.phase == LockInputPhase.SaveFailed) {
                restorePhaseIfSaveFailed()
                _uiState.update { it.copy(inputPassword = "") }
                return true
            }

            if (state.inputPassword.isNotEmpty()) {
                deleteLastDirection()
                return true
            }

            if (state.phase == LockInputPhase.ConfirmNew) {
                _uiState.update {
                    it.copy(
                        phase = LockInputPhase.InputNew,
                        newPassword = "",
                        inputPassword = "",
                        message = "",
                    )
                }
                return true
            }

            return false
        }

        /**
         * 异步写入数据库，完成后进入 [LockInputPhase.SaveSuccess]，失败则进入 [LockInputPhase.SaveFailed]。
         *
         * 取消信号必须原样重抛以保持结构化并发语义；[UserLockUiState.isSaving]
         * 在 `finally` 中无条件清除，否则异常会让 UI 永久停在“保存中”而无法再次交互。
         *
         * 失败时不将异常 `message` 写入 UI 状态（底层异常文本对电视用户不可读且可能含实现细节），
         * 只记录日志；展示文案由 UI 侧固定字符串资源提供。
         *
         * @param uid 目标用户 UID。
         * @param lock 待写入的密码串，空字符串表示清除用户锁。
         * @param sourcePhase 发起保存时所处阶段，供失败后回退使用。
         */
        private fun saveLockInternal(
            uid: Long,
            lock: String,
            sourcePhase: LockInputPhase,
        ) {
            saveSourcePhase = sourcePhase
            pendingLock = lock
            _uiState.update { it.copy(isSaving = true, message = "") }
            viewModelScope.launch {
                try {
                    accountRepository.updateUserLock(uid, lock)
                    pendingLock = null
                    _uiState.update { it.copy(phase = LockInputPhase.SaveSuccess) }
                } catch (e: CancellationException) {
                    // 仅超时等外部取消才走这里，吞掉会破坏协程取消语义，必须重抛
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "updateUserLock failed" }
                    _uiState.update { it.copy(phase = LockInputPhase.SaveFailed) }
                } finally {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }

        /**
         * 在 [LockInputPhase.SaveFailed] 下重试写入。
         *
         * 若待写入上下文已丢失（例如进程重建），则仅回退到发起保存前的阶段。
         */
        private fun retrySave() {
            val lock = pendingLock
            if (lock == null) {
                restorePhaseIfSaveFailed()
                return
            }
            saveLockInternal(_uiState.value.currentUid, lock, saveSourcePhase)
        }

        /**
         * 若当前处于 [LockInputPhase.SaveFailed]，则回退到发起保存前的阶段并清除错误提示。
         *
         * 非失败态时为空操作，以便在各输入入口无条件调用。
         */
        private fun restorePhaseIfSaveFailed() {
            if (_uiState.value.phase != LockInputPhase.SaveFailed) return
            pendingLock = null
            _uiState.update {
                it.copy(
                    phase = saveSourcePhase,
                    message = "",
                )
            }
        }
    }
