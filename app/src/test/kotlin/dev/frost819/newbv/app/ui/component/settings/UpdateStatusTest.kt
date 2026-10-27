package dev.frost819.newbv.app.ui.component.settings

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UpdateStatus] 与 [UpdateDialogMode] 枚举的单元测试。
 */
class UpdateStatusTest {
    @Test
    fun `UpdateStatus has eight active entries without pre-install blocking state`() {
        // Given & When
        val entries = UpdateStatus.entries

        // Then
        assertThat(entries).hasSize(8)
        assertThat(entries).containsExactly(
            UpdateStatus.UpdatingInfo,
            UpdateStatus.Ready,
            UpdateStatus.Downloading,
            UpdateStatus.Installing,
            UpdateStatus.NoAvailableUpdate,
            UpdateStatus.CheckError,
            UpdateStatus.DownloadError,
            UpdateStatus.InstallError,
        )
    }

    @Test
    fun `UpdateDialogMode contains CheckUpdate and ViewChangelog`() {
        // Given & When
        val modes = UpdateDialogMode.entries

        // Then
        assertThat(modes).containsExactly(
            UpdateDialogMode.CheckUpdate,
            UpdateDialogMode.ViewChangelog,
        )
    }
}
