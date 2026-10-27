package dev.frost819.newbv.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
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
 * 验证安装广播回调分发、未知来源权限检测、设置页降级跳转以及空文件防护逻辑。
 */
class OtaUpdateInstallerTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        OtaUpdateInstaller.clearInstallEvents()
    }

    @Test
    fun `canRequestPackageInstalls returns packageManager result on Oreo and above`() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.canRequestPackageInstalls() } returns false

        val allowed =
            OtaUpdateInstaller.canRequestPackageInstalls(
                context = context,
                sdkInt = Build.VERSION_CODES.R,
            )

        assertThat(allowed).isFalse()
    }

    @Test
    fun `canRequestPackageInstalls returns true below Oreo`() {
        val context = mockk<Context>(relaxed = true)

        val allowed =
            OtaUpdateInstaller.canRequestPackageInstalls(
                context = context,
                sdkInt = Build.VERSION_CODES.N,
            )

        assertThat(allowed).isTrue()
    }

    @Test
    fun `openUnknownSourceSettings falls back when initial intent fails`() {
        val context = mockk<Context>()
        every { context.packageName } returns "dev.frost819.btv"
        val attemptedActions = mutableListOf<Pair<String, String?>>()

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

        assertThat(launched).isTrue()
        assertThat(attemptedActions).hasSize(2)
        assertThat(attemptedActions[0].second).isEqualTo("package:dev.frost819.btv")
        assertThat(attemptedActions[1].second).isNull()
    }

    @Test
    fun `onInstallCallback emits Success when status is STATUS_SUCCESS`() =
        runTest {
            val context = mockk<Context>(relaxed = true)
            val intent = mockk<Intent>(relaxed = true)

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

    @Test
    fun `onInstallCallback emits Failure when status is STATUS_FAILURE_BLOCKED`() =
        runTest {
            val context = mockk<Context>(relaxed = true)
            val intent = mockk<Intent>(relaxed = true)

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

    @Test
    fun `installViaPackageInstallerSession fails fast when apk file does not exist`() {
        val context = mockk<Context>(relaxed = true)
        val missingFile = File(tempDir, "non_existent.apk")

        val result = OtaUpdateInstaller.installViaPackageInstallerSession(context, missingFile)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `exportApkToPublicDownloads returns null when source file is empty`() {
        val context = mockk<Context>(relaxed = true)
        val emptyFile = File(tempDir, "empty.apk").apply { createNewFile() }

        val exportedPath = OtaUpdateInstaller.exportApkToPublicDownloads(context, emptyFile)

        assertThat(exportedPath).isNull()
    }
}
