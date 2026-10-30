package dev.frost819.newbv.app.ui.component.player
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.theme.BVTheme

/**
 * YouTube TV 风格下沉式相关推荐泳道浮层（Bottom Shelf）。
 *
 * 位于屏幕底部 1/3 区域，采用自底向上滑入滑出动效。
 * 顶部 2/3 区域继续保持视频画面播放，做到观影与选片两不误。
 *
 * @param modifier 修饰符。
 * @param show 是否显示。
 * @param relatedVideos 相关视频列表。
 * @param onVideoClicked 点击视频卡片回调。
 * @param onClose 关闭浮层回调（返回键与「向上」键的关闭统一由根播放器按键处理器分发）。
 */
@Composable
fun RelatedVideosController(
    modifier: Modifier = Modifier,
    show: Boolean,
    relatedVideos: List<VideoCardData>,
    onVideoClicked: (VideoCardData) -> Unit,
    onClose: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    val backgroundBrush =
        remember {
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.8f),
                    Color.Black.copy(alpha = 0.96f),
                ),
            )
        }

    LaunchedEffect(show, relatedVideos.isNotEmpty()) {
        if (show && relatedVideos.isNotEmpty()) {
            focusRequester.safeRequestFocus()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = show,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .background(backgroundBrush)
                        .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "为你推荐",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "按「返回」或「向上」返回播放",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth().focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(horizontal = 28.dp, vertical = 6.dp),
                ) {
                    itemsIndexed(
                        items = relatedVideos,
                        key = { _, video -> video.avid },
                    ) { index, video ->
                        SmallVideoCard(
                            modifier =
                                Modifier
                                    .width(220.dp)
                                    .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                            data = video,
                            onClick = {
                                onVideoClicked(video)
                                onClose()
                            },
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RelatedVideosControllerPreview() {
    val sampleVideos =
        listOf(
            VideoCardData(
                avid = 1,
                cid = 101,
                title = "相关视频标题 1",
                cover = "",
                upName = "UP主A",
                playString = "1.2万播放",
                danmakuString = "300弹幕",
                timeString = "10:30",
            ),
            VideoCardData(
                avid = 2,
                cid = 102,
                title = "相关视频标题 2",
                cover = "",
                upName = "UP主B",
                playString = "5000播放",
                danmakuString = "100弹幕",
                timeString = "5:20",
            ),
        )

    BVTheme {
        RelatedVideosController(
            show = true,
            relatedVideos = sampleVideos,
            onVideoClicked = {},
        )
    }
}

// endregion
