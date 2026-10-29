package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.data.datastore.Audio

/**
 * 声音与音轨设置面板（二级菜单：直接显示音轨与码率列表）。
 *
 * @param modifier 修饰符
 * @param availableAudio 当前视频可用的音轨列表
 * @param currentAudio 当前正在播放的音轨
 * @param onAudioChange 音轨切换回调
 * @param onFocusStateChange 焦点状态变化回调
 */
@Composable
fun AudioMenuList(
    modifier: Modifier = Modifier,
    availableAudio: List<Audio>,
    currentAudio: Audio,
    onAudioChange: (Audio) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    val audioList =
        remember(availableAudio) {
            val list = if (availableAudio.isNotEmpty()) availableAudio else Audio.entries
            list.sortedByDescending { it.ordinal }
        }

    RadioMenuList(
        modifier = modifier,
        items = audioList.map { it.detailedDisplayName },
        selected = audioList.indexOf(currentAudio).coerceAtLeast(0),
        onSelectedChanged = { onAudioChange(audioList[it]) },
        onFocusBackToParent = { onFocusStateChange(MenuFocusState.MenuNav) },
    )
}

/** 音轨详细说明与码率。 */
private val Audio.detailedDisplayName: String
    get() =
        when (this) {
            Audio.ADolbyAtoms -> "杜比全景声 (Dolby Atmos)"
            Audio.AHiRes -> "Hi-Res 无损音质"
            Audio.A192K -> "高品质 192K"
            Audio.A132K -> "标准 132K"
            Audio.A64K -> "流畅 64K"
        }
