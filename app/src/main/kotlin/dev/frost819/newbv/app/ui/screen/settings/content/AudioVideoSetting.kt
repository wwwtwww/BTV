package dev.frost819.newbv.app.ui.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.entity.player.shortcut.PlayerCustomShortcutsStore
import dev.frost819.newbv.app.ui.component.settings.OptionDialog
import dev.frost819.newbv.app.ui.component.settings.SettingListItem
import dev.frost819.newbv.app.ui.component.settings.SettingSwitchListItem
import dev.frost819.newbv.app.ui.component.settings.displayName
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.app.viewmodel.settings.SettingsViewModel
import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.ScreensaverTimeout
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 音视频设置页。
 *
 * 画质/编码/音轨/倍速/播放结束动作/快捷键/软解/FFmpeg 音频。
 *
 * @param viewModel 设置页 ViewModel，通过 Hilt 注入。
 */
@Composable
fun AudioVideoSetting(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var showResolutionDialog by remember { mutableStateOf(false) }
    var showAudioCodecDialog by remember { mutableStateOf(false) }
    var showVideoCodecDialog by remember { mutableStateOf(false) }
    var showPlaySpeedDialog by remember { mutableStateOf(false) }
    var showActionAfterPlayDialog by remember { mutableStateOf(false) }
    var showScreensaverTimeoutDialog by remember { mutableStateOf(false) }
    var showPlayerCustomShortcutsDialog by remember { mutableStateOf(false) }

    var playerCustomShortcuts by remember { mutableStateOf(PlayerCustomShortcutsStore.get()) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = SettingsMenuNavItem.AudioVideo.displayName,
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingListItem(
            title = "默认分辨率",
            supportText = "当前：${uiState.defaultQuality.displayName}",
            onClick = { showResolutionDialog = true },
        )
        SettingListItem(
            title = "默认视频编码",
            supportText = "当前：${uiState.defaultVideoCodec.displayName}",
            onClick = { showVideoCodecDialog = true },
        )
        SettingListItem(
            title = "默认音频编码",
            supportText = "当前：${uiState.defaultAudio.displayName}",
            onClick = { showAudioCodecDialog = true },
        )
        SettingListItem(
            title = "默认播放速度",
            supportText = "当前：${uiState.defaultPlaySpeed.displayName}",
            onClick = { showPlaySpeedDialog = true },
        )
        SettingListItem(
            title = "播放结束动作",
            supportText = "当前：${uiState.actionAfterPlay.displayName}",
            onClick = { showActionAfterPlayDialog = true },
        )
        SettingListItem(
            title = "防烧屏屏保超时",
            supportText = "当前：${uiState.screensaverTimeout.displayName}",
            onClick = { showScreensaverTimeoutDialog = true },
        )
        SettingListItem(
            title = "自定义播放快捷键",
            supportText = "当前：${playerCustomShortcuts.size} 个绑定",
            onClick = { showPlayerCustomShortcutsDialog = true },
        )
        SettingSwitchListItem(
            title = "软解视频",
            supportText = "使用软件解码器（兼容性更好但性能较低）",
            checked = uiState.enableSoftwareVideoDecoder,
            onCheckedChange = { viewModel.updateEnableSoftwareVideoDecoder(it) },
        )
        SettingSwitchListItem(
            title = "FFmpeg 音频渲染",
            supportText = "使用 FFmpeg 进行音频解码渲染",
            checked = uiState.enableFfmpegAudioRenderer,
            onCheckedChange = { viewModel.updateEnableFfmpegAudioRenderer(it) },
        )
        SettingSwitchListItem(
            title = "播放器调试信息",
            supportText = "在播放画面上显示分辨率、编码、码率等信息",
            checked = uiState.showPlayerDebugInfo,
            onCheckedChange = { viewModel.updateShowPlayerDebugInfo(it) },
        )
        SettingSwitchListItem(
            title = "自动选择最优 CDN",
            supportText = "起播前对候选节点测速并选择最快者，失败时自动切换；可能略微增加起播等待",
            checked = uiState.autoSelectCdn,
            onCheckedChange = { viewModel.updateAutoSelectCdn(it) },
        )
    }

    if (showResolutionDialog) {
        OptionDialog(
            options = Resolution.entries.toTypedArray(),
            selectedOption = uiState.defaultQuality,
            onDismiss = { showResolutionDialog = false },
            onSelect = { viewModel.updateDefaultQuality(it) },
            getDisplayName = { it.displayName },
        )
    }

    if (showVideoCodecDialog) {
        OptionDialog(
            options = VideoCodec.entries.toTypedArray(),
            selectedOption = uiState.defaultVideoCodec,
            onDismiss = { showVideoCodecDialog = false },
            onSelect = { viewModel.updateDefaultVideoCodec(it) },
            getDisplayName = { it.displayName },
        )
    }

    if (showAudioCodecDialog) {
        OptionDialog(
            options = Audio.entries.toTypedArray(),
            selectedOption = uiState.defaultAudio,
            onDismiss = { showAudioCodecDialog = false },
            onSelect = { viewModel.updateDefaultAudio(it) },
            getDisplayName = { it.displayName },
        )
    }

    if (showPlaySpeedDialog) {
        OptionDialog(
            options = PlaySpeed.entries.toTypedArray(),
            selectedOption = uiState.defaultPlaySpeed,
            onDismiss = { showPlaySpeedDialog = false },
            onSelect = { viewModel.updateDefaultPlaySpeed(it) },
            getDisplayName = { it.displayName },
        )
    }

    if (showActionAfterPlayDialog) {
        OptionDialog(
            options = ActionAfterPlay.entries.toTypedArray(),
            selectedOption = uiState.actionAfterPlay,
            onDismiss = { showActionAfterPlayDialog = false },
            onSelect = { viewModel.updateActionAfterPlay(it) },
            getDisplayName = { it.displayName },
        )
    }

    if (showScreensaverTimeoutDialog) {
        OptionDialog(
            options = ScreensaverTimeout.entries.toTypedArray(),
            selectedOption = uiState.screensaverTimeout,
            onDismiss = { showScreensaverTimeoutDialog = false },
            onSelect = { viewModel.updateScreensaverTimeout(it) },
            getDisplayName = { it.displayName },
        )
    }

    if (showPlayerCustomShortcutsDialog) {
        PlayerCustomShortcutsDialog(
            onDismiss = { showPlayerCustomShortcutsDialog = false },
            onShortcutsChanged = { playerCustomShortcuts = it },
        )
    }
}
