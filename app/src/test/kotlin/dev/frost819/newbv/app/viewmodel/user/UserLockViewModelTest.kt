package dev.frost819.newbv.app.viewmodel.user

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.data.db.entity.UserEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [UserLockViewModel] 的单元测试。
 *
 * 验证三阶段状态机流转、旧密码强校验、正向回退删除、异步写入防竞态与写入失败恢复。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserLockViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var viewModel: UserLockViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = mockk(relaxed = true)
        viewModel = UserLockViewModel(accountRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init enters VerifyOld when user already has lock`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1001L, username = "test", avatar = "", auth = "{}", lock = "udlr")
            coEvery { accountRepository.findUserByUid(1001L) } returns user

            viewModel.init(1001L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.hasExistingLock).isTrue()
            assertThat(state.phase).isEqualTo(LockInputPhase.VerifyOld)
            assertThat(state.inputPassword).isEmpty()
        }

    @Test
    fun `init enters InputNew directly when user has no lock`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user

            viewModel.init(1002L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.hasExistingLock).isFalse()
            assertThat(state.phase).isEqualTo(LockInputPhase.InputNew)
        }

    @Test
    fun `appendDirection appends key and deleteLastDirection drops from the end`() =
        runTest(testDispatcher) {
            viewModel.appendDirection("u")
            viewModel.appendDirection("d")
            viewModel.appendDirection("l")
            assertThat(viewModel.uiState.value.inputPassword).isEqualTo("udl")

            // 正向退格应删除末尾的 'l'，剩余 'ud'
            viewModel.deleteLastDirection()
            assertThat(viewModel.uiState.value.inputPassword).isEqualTo("ud")
        }

    @Test
    fun `confirm in VerifyOld with wrong password shows error and clears input`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1001L, username = "test", avatar = "", auth = "{}", lock = "udlr")
            coEvery { accountRepository.findUserByUid(1001L) } returns user
            viewModel.init(1001L)
            advanceUntilIdle()

            viewModel.appendDirection("u")
            viewModel.appendDirection("u")
            viewModel.confirm()

            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.VerifyOld)
            assertThat(state.inputPassword).isEmpty()
            assertThat(state.message).isEqualTo("密码错误")
        }

    @Test
    fun `confirm in VerifyOld with correct password transitions to InputNew`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1001L, username = "test", avatar = "", auth = "{}", lock = "udlr")
            coEvery { accountRepository.findUserByUid(1001L) } returns user
            viewModel.init(1001L)
            advanceUntilIdle()

            "udlr".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()

            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.InputNew)
            assertThat(state.inputPassword).isEmpty()
            assertThat(state.message).isEmpty()
        }

    @Test
    fun `confirm in InputNew with empty password clears lock in repository`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1001L, username = "test", avatar = "", auth = "{}", lock = "udlr")
            coEvery { accountRepository.findUserByUid(1001L) } returns user
            viewModel.init(1001L)
            advanceUntilIdle()

            // 先正确验证旧密码进入 InputNew
            "udlr".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()

            // 在 InputNew 下直接确认（空密码）
            viewModel.confirm()
            advanceUntilIdle()

            coVerify { accountRepository.updateUserLock(1001L, "") }
            assertThat(viewModel.uiState.value.phase).isEqualTo(LockInputPhase.SaveSuccess)
        }

    @Test
    fun `confirm in ConfirmNew with mismatched password resets to InputNew`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user
            viewModel.init(1002L)
            advanceUntilIdle()

            // 第一次输入 "uudd"
            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            assertThat(viewModel.uiState.value.phase).isEqualTo(LockInputPhase.ConfirmNew)

            // 第二次输入 "uudl"（不匹配）
            "uudl".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()

            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.InputNew)
            assertThat(state.inputPassword).isEmpty()
            assertThat(state.newPassword).isEmpty()
            assertThat(state.message).isEqualTo("两次密码不一致")
        }

    @Test
    fun `confirm in ConfirmNew with matching password saves to repository and transitions to SaveSuccess`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user
            viewModel.init(1002L)
            advanceUntilIdle()

            // 第一次输入 "uudd"
            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()

            // 第二次输入 "uudd"
            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            advanceUntilIdle()

            coVerify { accountRepository.updateUserLock(1002L, "uudd") }
            assertThat(viewModel.uiState.value.phase).isEqualTo(LockInputPhase.SaveSuccess)
        }

    @Test
    fun `onBack handles backspace and phase transitions correctly`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user
            viewModel.init(1002L)
            advanceUntilIdle()

            // 1. 当有输入时，onBack 消费事件并删除末位字符
            viewModel.appendDirection("u")
            val consumed1 = viewModel.onBack()
            assertThat(consumed1).isTrue()
            assertThat(viewModel.uiState.value.inputPassword).isEmpty()

            // 2. 当无输入且处于 InputNew 时，onBack 不消费并允许退出
            val consumed2 = viewModel.onBack()
            assertThat(consumed2).isFalse()

            // 3. 当处于 ConfirmNew 时，无输入时 onBack 退回 InputNew
            viewModel.appendDirection("u")
            viewModel.confirm()
            assertThat(viewModel.uiState.value.phase).isEqualTo(LockInputPhase.ConfirmNew)

            val consumed3 = viewModel.onBack()
            assertThat(consumed3).isTrue()
            assertThat(viewModel.uiState.value.phase).isEqualTo(LockInputPhase.InputNew)
        }

    @Test
    fun `save failure enters SaveFailed and always releases isSaving`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user
            // Given：写入抛出异常
            coEvery { accountRepository.updateUserLock(any(), any()) } throws
                IllegalStateException("db down")
            viewModel.init(1002L)
            advanceUntilIdle()

            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            advanceUntilIdle()

            // Then：finally 保证 isSaving 复位，UI 不会永久卡住
            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.SaveFailed)
            assertThat(state.isSaving).isFalse()
            // 异常详情只记日志，不写入 UI 状态（展示文案由 UI 侧固定字符串资源提供）
            assertThat(state.message).isEmpty()
        }

    @Test
    fun `confirm in SaveFailed retries the pending write and succeeds`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user
            coEvery { accountRepository.updateUserLock(any(), any()) } throws
                IllegalStateException("db down")
            viewModel.init(1002L)
            advanceUntilIdle()

            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.phase).isEqualTo(LockInputPhase.SaveFailed)

            // Given：故障恢复
            coEvery { accountRepository.updateUserLock(any(), any()) } returns Unit

            // When：失败态下直接按确认键重试，无需重新输入密码
            viewModel.confirm()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { accountRepository.updateUserLock(1002L, "uudd") }
            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.SaveSuccess)
            assertThat(state.isSaving).isFalse()
            assertThat(state.message).isEmpty()
        }

    @Test
    fun `appendDirection in SaveFailed falls back to the phase before saving`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1002L, username = "test", avatar = "", auth = "{}", lock = "")
            coEvery { accountRepository.findUserByUid(1002L) } returns user
            coEvery { accountRepository.updateUserLock(any(), any()) } throws
                IllegalStateException("db down")
            viewModel.init(1002L)
            advanceUntilIdle()

            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            "uudd".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()
            advanceUntilIdle()

            // When：失败态下继续输入，视为放弃重试、回到 ConfirmNew 编辑
            viewModel.appendDirection("l")

            // Then
            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.ConfirmNew)
            assertThat(state.message).isEmpty()
            assertThat(state.inputPassword).isEqualTo("uuddl")
        }

    @Test
    fun `clearing lock in InputNew enters SaveFailed when repository throws`() =
        runTest(testDispatcher) {
            val user = UserEntity(uid = 1001L, username = "test", avatar = "", auth = "{}", lock = "udlr")
            coEvery { accountRepository.findUserByUid(1001L) } returns user
            viewModel.init(1001L)
            advanceUntilIdle()

            // 先验证旧密码进入 InputNew
            "udlr".forEach { viewModel.appendDirection(it.toString()) }
            viewModel.confirm()

            // Given：清锁（空密码确认）写入失败
            coEvery { accountRepository.updateUserLock(any(), any()) } throws
                IllegalStateException("db down")
            viewModel.confirm()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.phase).isEqualTo(LockInputPhase.SaveFailed)
            assertThat(state.isSaving).isFalse()
            assertThat(state.message).isEmpty()
        }
}
