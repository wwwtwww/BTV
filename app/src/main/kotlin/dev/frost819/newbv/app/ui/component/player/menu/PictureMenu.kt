package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.ui.component.player.menu.component.PlayerThreeLevelMenu
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerPictureMenuItem
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 画质设置面板。
 *
 * 双列布局：左侧为选项值列表（[RadioMenuList]），右侧为子项列表。
 * 子项包括：分辨率、编码、宽高比。
 *
 * @param modifier 修饰符
 * @param availableQualityIds 可用画质 ID 列表
 * @param availableVideoCodec 可用编码列表
 * @param currentResolution 当前画质 ID
 * @param currentVideoCodec 当前编码
 * @param currentVideoAspectRatio 当前宽高比
 * @param onResolutionChange 画质变化回调
 * @param onCodecChange 编码变化回调
 * @param onAspectRatioChange 宽高比变化回调
 * @param onFocusStateChange 焦点状态变化回调
 */
@Composable
fun PictureMenuList(
    modifier: Modifier = Modifier,
    availableQualityIds: List<Int>,
    availableVideoCodec: List<VideoCodec>,
    currentResolution: Int?,
    currentVideoCodec: VideoCodec,
    currentVideoAspectRatio: VideoAspectRatio,
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    val qualityIdList =
        remember(availableQualityIds) {
            availableQualityIds.sortedByDescending { it }
        }

    PlayerThreeLevelMenu(
        modifier = modifier,
        categories = VideoPlayerPictureMenuItem.entries,
        categoryLabel = { it.displayName },
        onFocusStateChange = onFocusStateChange,
    ) { selectedItem, itemModifier, backToMenu ->
        when (selectedItem) {
            VideoPlayerPictureMenuItem.Resolution ->
                RadioMenuList(
                    modifier = itemModifier,
                    items =
                        qualityIdList.map { resolutionCode ->
                            Resolution.fromCode(resolutionCode).displayName
                        },
                    selected = qualityIdList.indexOf(currentResolution),
                    onSelectedChanged = { onResolutionChange(qualityIdList[it]) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerPictureMenuItem.Codec ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = availableVideoCodec.map { it.displayName },
                    selected = availableVideoCodec.indexOf(currentVideoCodec),
                    onSelectedChanged = { onCodecChange(availableVideoCodec[it]) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerPictureMenuItem.AspectRatio ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = VideoAspectRatio.entries.map { it.displayName },
                    selected = VideoAspectRatio.entries.indexOf(currentVideoAspectRatio),
                    onSelectedChanged = { onAspectRatioChange(VideoAspectRatio.entries[it]) },
                    onFocusBackToParent = backToMenu,
                )
        }
    }
}

/** 画质显示名称。 */
private val Resolution.displayName: String
    get() =
        when (this) {
            Resolution.R240P -> "240P"
            Resolution.R360P -> "360P"
            Resolution.R480P -> "480P"
            Resolution.R720P -> "720P"
            Resolution.R720P60 -> "720P60"
            Resolution.R1080P -> "1080P"
            Resolution.R1080PPlus -> "1080P+"
            Resolution.R1080P60 -> "1080P60"
            Resolution.R4K -> "4K"
            Resolution.RHdr -> "HDR"
            Resolution.RDolby -> "Dolby"
            Resolution.R8K -> "8K"
        }

/** 编码显示名称。 */
private val VideoCodec.displayName: String
    get() =
        when (this) {
            VideoCodec.AVC -> "AVC/H.264"
            VideoCodec.HEVC -> "HEVC/H.265"
            VideoCodec.AV1 -> "AV1"
            VideoCodec.DVH1 -> "DV H.1"
        }
