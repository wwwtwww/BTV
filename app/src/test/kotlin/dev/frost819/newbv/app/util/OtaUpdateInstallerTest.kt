package dev.frost819.newbv.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * [OtaUpdateInstaller] 的单元测试。
 *
 * 验证首选安装通道决策、安装广播回调分发、未知来源权限检测、
 * 空壳设置页秒退判定与多级降级跳转以及空文件防护逻辑。
 */
class OtaUpdateInstallerTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        OtaUpdateInstaller.clearInstallEvents()
    }

    @Test
    fun `selectPreferredInstallMode chooses LegacyActionView when permission not granted`() {
        // Given & When
        val mode =
            OtaUpdateInstaller.selectPreferredInstallMode(
                canRequestInstalls = false,
                forceLegacy = false,
            )

        // Then: 未授予未知来源权限时直接走 ACTION_VIEW 调起系统安装器，不再前置阻断
        assertThat(mode).isEqualTo(OtaInstallMode.LegacyActionView)
    }

    @Test
    fun `selectPreferredInstallMode chooses PackageInstallerSession when permission granted`() {
        // Given & When
        val mode =
            OtaUpdateInstaller.selectPreferredInstallMode(
                canRequestInstalls = true,
                forceLegacy = false,
            )

        // Then
        assertThat(mode).isEqualTo(OtaInstallMode.PackageInstallerSession)
    }

    @Test
    fun `selectPreferredInstallMode honors forceLegacy even when permission granted`() {
        // Given & When
        val mode =
            OtaUpdateInstaller.selectPreferredInstallMode(
                canRequestInstalls = true,
                forceLegacy = true,
            )

        // Then
        assertThat(mode).isEqualTo(OtaInstallMode.LegacyActionView)
    }

    @Test
    fun `shouldFallbackFromSettingsStub detects fast bounce back without permission`() {
        // Given & When: 300ms 内秒退回前台且仍未获授权
        val shouldFallback =
            OtaUpdateInstaller.shouldFallbackFromSettingsStub(
                elapsedMs = 300L,
                hasInstallPermission = false,
                alreadyUsedFallback = false,
            )

        // Then
        assertThat(shouldFallback).isTrue()
    }

    @Test
    fun `shouldFallbackFromSettingsStub returns false after normal user dwell or fallback already used`() {
        // Given & When
        val normalStay =
            OtaUpdateInstaller.shouldFallbackFromSettingsStub(
                elapsedMs = 1_500L,
                hasInstallPermission = false,
                alreadyUsedFallback = false,
            )
        val alreadyFellBack =
            OtaUpdateInstaller.shouldFallbackFromSettingsStub(
                elapsedMs = 200L,
                hasInstallPermission = false,
                alreadyUsedFallback = true,
            )
        val granted =
            OtaUpdateInstaller.shouldFallbackFromSettingsStub(
                elapsedMs = 200L,
                hasInstallPermission = true,
                alreadyUsedFallback = false,
            )

        // Then
        assertThat(normalStay).isFalse()
        assertThat(alreadyFellBack).isFalse()
        assertThat(granted).isFalse()
    }

    @Test
    fun `canRequestPackageInstalls returns packageManager result on Oreo and above`() {
        // Given
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.canRequestPackageInstalls() } returns false

        // When
        val allowed =
            OtaUpdateInstaller.canRequestPackageInstalls(
                context = context,
                sdkInt = Build.VERSION_CODES.R,
            )

        // Then
        assertThat(allowed).isFalse()
    }

    @Test
    fun `canRequestPackageInstalls returns true below Oreo`() {
        // Given
        val context = mockk<Context>(relaxed = true)

        // When
        val allowed =
            OtaUpdateInstaller.canRequestPackageInstalls(
                context = context,
                sdkInt = Build.VERSION_CODES.N,
            )

        // Then
        assertThat(allowed).isTrue()
    }

    @Test
    fun `openUnknownSourceSettings falls back when initial intent fails`() {
        // Given
        val context = mockk<Context>()
        every { context.packageName } returns "dev.frost819.btv"
        val attemptedActions = mutableListOf<Pair<String, String?>>()

        // When
        val launched =
            OtaUpdateInstaller.openUnknownSourceSettings(
                context = context,
                sdkInt = Build.VERSION_CODES.R,
            ) { action, dataUri ->
                attemptedActions.add(action to dataUri)
                if (attemptedActions.size == 1) {
                    throw SecurityException("ROM blocked package specific unknown sources")
                }
            }

        // Then
        assertThat(launched).isTrue()
        assertThat(attemptedActions).hasSize(2)
        assertThat(attemptedActions[0].second).isEqualTo("package:dev.frost819.btv")
        assertThat(attemptedActions[1].second).isNull()
    }

    @Test
    fun `openUnknownSourceSettings skips MANAGE_UNKNOWN_APP_SOURCES when skipUnknownSourcesAction is true`() {
        // Given
        val context = mockk<Context>()
        every { context.packageName } returns "dev.frost819.btv"
        val attemptedActions = mutableListOf<Pair<String, String?>>()

        // When
        val launched =
            OtaUpdateInstaller.openUnknownSourceSettings(
                context = context,
                sdkInt = Build.VERSION_CODES.R,
                skipUnknownSourcesAction = true,
            ) { action, dataUri ->
                attemptedActions.add(action to dataUri)
            }

        // Then: 直接进入应用详情设置页，不再重复调用空壳的 MANAGE_UNKNOWN_APP_SOURCES
        assertThat(launched).isTrue()
        assertThat(attemptedActions).hasSize(1)
        assertThat(attemptedActions[0].first).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(attemptedActions[0].second).isEqualTo("package:dev.frost819.btv")
    }

    @Test
    fun `openUnknownSourceSettings returns false when all candidates fail`() {
        // Given
        val context = mockk<Context>()
        every { context.packageName } returns "dev.frost819.btv"

        // When
        val launched =
            OtaUpdateInstaller.openUnknownSourceSettings(
                context = context,
                sdkInt = Build.VERSION_CODES.R,
                skipUnknownSourcesAction = true,
            ) { _, _ ->
                throw SecurityException("All settings activities blocked")
            }

        // Then
        assertThat(launched).isFalse()
    }

    @Test
    fun `onInstallCallback emits Success when status is STATUS_SUCCESS`() {
        runTest {
            // Given
            val context = mockk<Context>(relaxed = true)
            val intent = mockk<Intent>(relaxed = true)

            // When & Then
            OtaUpdateInstaller.installEvents.test {
                OtaUpdateInstaller.onInstallCallback(
                    context = context,
                    status = PackageInstaller.STATUS_SUCCESS,
                    statusMessage = "Success",
                    rawIntent = intent,
                )
                assertThat(awaitItem()).isEqualTo(OtaInstallCallbackEvent.Success)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `onInstallCallback emits Failure when status is STATUS_FAILURE_BLOCKED`() {
        runTest {
            // Given
            val context = mockk<Context>(relaxed = true)
            val intent = mockk<Intent>(relaxed = true)

            // When & Then
            OtaUpdateInstaller.installEvents.test {
                OtaUpdateInstaller.onInstallCallback(
                    context = context,
                    status = PackageInstaller.STATUS_FAILURE_BLOCKED,
                    statusMessage = "Install blocked by TV ROM policy",
                    rawIntent = intent,
                )
                val event = awaitItem()
                assertThat(event).isInstanceOf(OtaInstallCallbackEvent.Failure::class.java)
                val failure = event as OtaInstallCallbackEvent.Failure
                assertThat(failure.status).isEqualTo(PackageInstaller.STATUS_FAILURE_BLOCKED)
                assertThat(failure.message).contains("blocked")
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `installViaPackageInstallerSession fails fast when apk file does not exist`() {
        // Given
        val context = mockk<Context>(relaxed = true)
        val missingFile = File(tempDir, "non_existent.apk")

        // When
        val result = OtaUpdateInstaller.installViaPackageInstallerSession(context, missingFile)

        // Then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `clearInstallEvents prevents replay of stale callback event`() {
        runTest {
            // Given
            val context = mockk<Context>(relaxed = true)
            val intent = mockk<Intent>(relaxed = true)

            OtaUpdateInstaller.onInstallCallback(
                context = context,
                status = PackageInstaller.STATUS_FAILURE_BLOCKED,
                statusMessage = "Stale failure",
                rawIntent = intent,
            )

            // When
            OtaUpdateInstaller.clearInstallEvents()

            // Then
            OtaUpdateInstaller.installEvents.test {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `exportApkToPublicDownloads returns null when source file is empty`() {
        // Given
        val context = mockk<Context>(relaxed = true)
        val emptyFile = File(tempDir, "empty.apk").apply { createNewFile() }

        // When
        val exportedPath = OtaUpdateInstaller.exportApkToPublicDownloads(context, emptyFile)

        // Then
        assertThat(exportedPath).isNull()
    }
}
