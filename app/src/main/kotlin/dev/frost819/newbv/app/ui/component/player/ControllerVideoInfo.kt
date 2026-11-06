package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.ui.component.player.menu.PlaySpeedItem
import dev.frost819.newbv.app.ui.component.player.menu.detailedDisplayName
import dev.frost819.newbv.app.ui.component.player.menu.shortBadge
import dev.frost819.newbv.app.ui.component.settings.displayName
import dev.frost819.newbv.app.ui.state.player.SeekerState
import dev.frost819.newbv.app.util.VideoShotImageCache
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.BVTypography
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.Resolution
import kotlinx.coroutines.delay

/**
 * 播放器控制器信息层。
 *
 * 包含顶部标题+时钟和底部进度条+操作按钮两部分。
 * 通过 [AnimatedVisibility] 控制进出场动画。
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param isSeeking 是否正在 seek
 * @param goTime seek 预览位置（毫秒）
 * @param seekerState 进度条状态
 * @param title 视频标题
 * @param clock 时钟（hour, minute）
 * @param onlineWatching 同时观看人数文案（空串时不显示）
 * @param audio 当前音轨（用于显示杜比全景声 / Hi-Res 专属徽标，为 null 时不展示）
 * @param resolution 当前画质（用于显示 8K / 4K / 杜比视界 / HDR 徽标，为 null 时不展示）
 * @param aspectRatio 当前画面填充比例
 * @param videoShot 缩略图数据（为 null 时不显示预览）
 * @param videoShotCache 缩略图缓存
 * @param isPgc 是否来自番剧（为 true 时隐藏详情/UP/相关视频按钮）
 * @param episodesAvailable 是否存在可切换的多集/多分 P（为 true 时展示「选集」入口及向下快捷提示）
 * @param relatedAvailable 相关推荐是否可用（PGC 或列表为空时为 false，此时隐藏入口与提示）
 * @param danmakuEnabled 弹幕是否开启
 * @param isLooping 是否循环播放
 * @param onDirectionLeft seek 左移回调
 * @param onDirectionRight seek 右移回调
 * @param onSeekGoTime 确认 seek 回调
 * @param onSeekToPosition 触屏拖拽/点击进度条时 seek 到指定位置（毫秒）
 * @param onPlayPause 播放/暂停回调
 * @param onDanmakuSwitchChange 弹幕开关回调
 * @param onShowSettings 打开设置回调
 * @param onShowRelatedVideos 打开相关视频回调
 * @param onGoToVideoInfo 跳转视频详情回调
 * @param onToggleLoop 切换循环回调
 * @param onGoToUpPage 跳转 UP 主页面回调
 * @param onShowInteraction 打开视频交互弹窗回调
 * @param onShowComments 打开评论弹窗回调
 * @param onShowEpisodes 打开底部选集胶囊轨回调
 * @param onCycleAspectRatio 循环切换画面比例回调
 * @param availableQuality 可用清晰度键值映射 (qn to label)
 * @param currentQualityId 当前生效的画质代码 qn
 * @param onResolutionChange 画质切换回调
 * @param availableAudio 可用音频流列表
 * @param currentAudio 当前生效的音频流
 * @param onAudioChange 音轨切换回调
 * @param playSpeed 当前播放倍速
 * @param onPlaySpeedChange 播放倍速切换回调
 * @param availableSubtitles 可用字幕列表
 * @param currentSubtitleId 当前生效的字幕 ID
 * @param onSubtitleChange 字幕切换回调
 * @param onResetAutoHide 重置 OSD 自动隐藏计时器回调
 */
@Composable
fun ControllerVideoInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    isSeeking: Boolean,
    goTime: Long,
    seekerState: SeekerState,
    title: String,
    clock: Pair<Int, Int>,
    onlineWatching: String,
    audio: Audio? = null,
    resolution: Resolution? = null,
    aspectRatio: VideoAspectRatio = VideoAspectRatio.Default,
    videoShot: VideoShot?,
    videoShotCache: VideoShotImageCache,
    isPgc: Boolean,
    episodesAvailable: Boolean = false,
    relatedAvailable: Boolean = true,
    danmakuEnabled: Boolean,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onSeekToPosition: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,
    onShowInteraction: () -> Unit,
    onShowComments: () -> Unit,
    onShowEpisodes: () -> Unit = {},
    onCycleAspectRatio: () -> Unit = {},
    availableQuality: Map<Int, String> = emptyMap(),
    currentQualityId: Int? = null,
    onResolutionChange: (Int) -> Unit = {},
    availableAudio: List<Audio> = emptyList(),
    currentAudio: Audio = Audio.A192K,
    onAudioChange: (Audio) -> Unit = {},
    playSpeed: Float = 1f,
    onPlaySpeedChange: (Float) -> Unit = {},
    availableSubtitles: List<Subtitle> = emptyList(),
    currentSubtitleId: Long = -1L,
    onSubtitleChange: (Subtitle) -> Unit = {},
    onResetAutoHide: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerTopVideoInfo",
        ) {
            ControllerVideoInfoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = title,
                clock = clock,
                onlineWatching = onlineWatching,
                audio = audio,
                resolution = resolution,
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerBottomVideoInfo",
        ) {
            ControllerVideoInfoBottom(
                modifier = Modifier.align(Alignment.BottomCenter),
                show = show,
                isSeeking = isSeeking,
                aspectRatio = aspectRatio,
                onCycleAspectRatio = onCycleAspectRatio,
                goTime = goTime,
                seekerState = seekerState,
                videoShot = videoShot,
                videoShotCache = videoShotCache,
                isPgc = isPgc,
                episodesAvailable = episodesAvailable,
                relatedAvailable = relatedAvailable,
                danmakuEnabled = danmakuEnabled,
                isLooping = isLooping,
                onDirectionLeft = onDirectionLeft,
                onDirectionRight = onDirectionRight,
                onSeekGoTime = onSeekGoTime,
                onSeekToPosition = onSeekToPosition,
                onPlayPause = onPlayPause,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onShowSettings = onShowSettings,
                onShowEpisodes = onShowEpisodes,
                onShowRelatedVideos = onShowRelatedVideos,
                onGoToVideoInfo = onGoToVideoInfo,
                onToggleLoop = onToggleLoop,
                onGoToUpPage = onGoToUpPage,
                onShowInteraction = onShowInteraction,
                onShowComments = onShowComments,
                resolution = resolution,
                availableQuality = availableQuality,
                currentQualityId = currentQualityId,
                onResolutionChange = onResolutionChange,
                availableAudio = availableAudio,
                currentAudio = currentAudio,
                onAudioChange = onAudioChange,
                playSpeed = playSpeed,
                onPlaySpeedChange = onPlaySpeedChange,
                availableSubtitles = availableSubtitles,
                currentSubtitleId = currentSubtitleId,
                onSubtitleChange = onSubtitleChange,
                onResetAutoHide = onResetAutoHide,
            )
        }
    }
}

/**
 * 控制器顶部信息（标题 + 同时观看人数 + 时钟 + 视听专属徽标）。
 *
 * @param modifier 修饰符
 * @param title 视频标题
 * @param clock 时钟（hour, minute）
 * @param onlineWatching 同时观看人数文案（服务端预格式化，如 "9.4万+"），空串时不显示
 * @param audio 当前音轨（用于显示杜比全景声 / Hi-Res 音质徽标，为 null 时不展示）
 * @param resolution 当前画质（用于显示 8K / 4K / 杜比视界 / HDR 徽标，为 null 时不展示）
 */
@Composable
fun ControllerVideoInfoTop(
    modifier: Modifier = Modifier,
    title: String,
    clock: Pair<Int, Int>,
    onlineWatching: String,
    audio: Audio? = null,
    resolution: Resolution? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp),
                    ),
                ).background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.82f),
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Black.copy(alpha = 0f),
                                ),
                        ),
                ).padding(horizontal = 32.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                text = title,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 4f),
                    ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Clock(hour = clock.first, minute = clock.second)
        }

        // Meta 行（同时观看人数 + 杜比/Hi-Res/4K专属视听高光徽标）
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onlineWatching.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_player_watching),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${onlineWatching}人正在看",
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                shadow = Shadow(color = Color.Black, blurRadius = 1f),
                            ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }
            }

            // 画质与音质专属视听徽标
            VideoQualityBadges(audio = audio, resolution = resolution)
        }
    }
}

@Composable
private fun VideoQualityBadges(
    audio: Audio?,
    resolution: Resolution?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (resolution) {
            Resolution.R8K ->
                QualityBadge(
                    text = "8K ULTRA HD",
                    backgroundColor = BVColors.Badge8kBackground,
                    contentColor = BVColors.Badge8kContent,
                )
            Resolution.R4K ->
                QualityBadge(
                    text = "4K ULTRA HD",
                    backgroundColor = BVColors.Badge4kBackground,
                    contentColor = BVColors.Badge4kContent,
                )
            Resolution.RDolby ->
                QualityBadge(
                    text = "DOLBY VISION",
                    backgroundColor = BVColors.BadgeDolbyVisionBackground,
                    contentColor = BVColors.BadgeDolbyVisionContent,
                )
            Resolution.RHdr ->
                QualityBadge(
                    text = "HDR",
                    backgroundColor = BVColors.BadgeHdrBackground,
                    contentColor = BVColors.BadgeHdrContent,
                )
            else -> {}
        }

        when (audio) {
            Audio.ADolbyAtoms ->
                QualityBadge(
                    text = "DOLBY ATMOS",
                    backgroundColor = BVColors.BadgeDolbyAtmosBackground,
                    contentColor = BVColors.BadgeDolbyAtmosContent,
                )
            Audio.AHiRes ->
                QualityBadge(
                    text = "Hi-Res AUDIO",
                    backgroundColor = BVColors.BadgeHiResBackground,
                    contentColor = BVColors.BadgeHiResContent,
                )
            else -> {}
        }
    }
}

@Composable
private fun QualityBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .border(1.dp, contentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

/**
 * 控制器底部信息（缩略图预览 + 时间 + 进度条 + 操作按钮）。
 *
 * 「选集」与「相关推荐」相互独立展示：
 * - [episodesAvailable] 为 `true` 时显示「选集」按钮（含番剧多集、UGC 合集、单视频多 P）；
 * - [relatedAvailable] 为 `true` 时显示「相关推荐」按钮；
 * - 在操作按钮行继续按遥控器「向下」键时，优先呼出底部选集胶囊轨，无选集时呼出相关推荐泳道。
 *
 * @param modifier 修饰符
 * @param show 控制器是否可见
 * @param isSeeking 是否正在 seek
 * @param goTime seek 目标时间点（毫秒）
 * @param seekerState 进度条状态模型
 * @param videoShot 视频帧缩略图
 * @param videoShotCache 缩略图缓存池
 * @param isPgc 是否为番剧/影视
 * @param episodesAvailable 选集是否可用
 * @param relatedAvailable 相关推荐是否可用
 * @param danmakuEnabled 弹幕是否开启
 * @param isLooping 是否循环播放
 * @param onDirectionLeft 向左 seek 回调
 * @param onDirectionRight 向右 seek 回调
 * @param onSeekGoTime 确认 seek 回调
 * @param onSeekToPosition 拖拽 seek 回调
 * @param onPlayPause 播放/暂停切换回调
 * @param onDanmakuSwitchChange 弹幕开关切换回调
 * @param onShowSettings 打开高级设置抽屉回调
 * @param onShowRelatedVideos 打开相关推荐泳道回调
 * @param onGoToVideoInfo 跳转视频详情页回调
 * @param onToggleLoop 切换循环播放模式回调
 * @param onGoToUpPage 跳转 UP 主空间回调
 * @param onShowInteraction 打开点赞投币三连弹窗回调
 * @param onShowComments 打开评论弹窗回调
 * @param onShowEpisodes 打开选集胶囊轨回调
 * @param aspectRatio 画面填充比例
 * @param onCycleAspectRatio 画面比例切换回调
 * @param resolution 当前视频画质
 * @param availableQuality 可用清晰度键值映射 (qn to label)
 * @param currentQualityId 当前生效的画质代码 qn
 * @param onResolutionChange 画质切换回调
 * @param availableAudio 可用音频流列表
 * @param currentAudio 当前生效的音频流
 * @param onAudioChange 音轨切换回调
 * @param playSpeed 当前播放倍速
 * @param onPlaySpeedChange 播放倍速切换回调
 * @param availableSubtitles 可用字幕列表
 * @param currentSubtitleId 当前生效的字幕 ID
 * @param onSubtitleChange 字幕切换回调
 * @param onResetAutoHide 重置 OSD 自动隐藏计时器回调
 */
@Composable
fun ControllerVideoInfoBottom(
    modifier: Modifier = Modifier,
    show: Boolean = true,
    isSeeking: Boolean,
    goTime: Long,
    seekerState: SeekerState,
    videoShot: VideoShot?,
    videoShotCache: VideoShotImageCache,
    isPgc: Boolean,
    episodesAvailable: Boolean = false,
    relatedAvailable: Boolean = true,
    danmakuEnabled: Boolean,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onSeekToPosition: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,
    onShowInteraction: () -> Unit,
    onShowComments: () -> Unit,
    onShowEpisodes: () -> Unit = {},
    aspectRatio: VideoAspectRatio = VideoAspectRatio.Default,
    onCycleAspectRatio: () -> Unit = {},
    resolution: Resolution? = null,
    availableQuality: Map<Int, String> = emptyMap(),
    currentQualityId: Int? = null,
    onResolutionChange: (Int) -> Unit = {},
    availableAudio: List<Audio> = emptyList(),
    currentAudio: Audio = Audio.A192K,
    onAudioChange: (Audio) -> Unit = {},
    playSpeed: Float = 1f,
    onPlaySpeedChange: (Float) -> Unit = {},
    availableSubtitles: List<Subtitle> = emptyList(),
    currentSubtitleId: Long = -1L,
    onSubtitleChange: (Subtitle) -> Unit = {},
    onResetAutoHide: () -> Unit = {},
) {
    val seekFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val resolutionFocusRequester = remember { FocusRequester() }
    val speedFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val subtitleFocusRequester = remember { FocusRequester() }

    var activeQuickMenu by remember { mutableStateOf<VodQuickMenuType?>(null) }
    var popoverContainerRootX by remember { mutableFloatStateOf(0f) }
    val quickMenuAnchorRootX = remember { mutableStateMapOf<VodQuickMenuType, Float>() }
    var isSeekFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // TV 遥控器输入焦点时序：延迟 50ms（约 2-3 帧垂直同步），
        // 等待 OSD 展开动画及操作栏节点完全组合并挂载至 Compose Focus 树节点，
        // 避免未完成挂载时直接 safeRequestFocus 导致请求静默失效（详见 AGENTS.md §8.2.1）。
        delay(50)
        // 初始呼出焦点直达按钮行第1项，消灭进入设置必须先往下按一次的痛点
        runCatching { playPauseFocusRequester.safeRequestFocus() }
    }

    Column(
        modifier =
            modifier
                .clip(
                    MaterialTheme.shapes.large.copy(
                        bottomStart = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp),
                    ),
                ).background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.88f),
                                ),
                        ),
                ).padding(top = 28.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Seek 缩略图预览
        if (isSeeking && videoShot != null) {
            VideoShot(
                modifier = Modifier.padding(horizontal = 48.dp),
                videoShot = videoShot,
                imageCache = videoShotCache,
                position = goTime,
                duration = seekerState.totalDuration,
                coercedOffset = (-24).dp,
            )
        }

        // 时间显示
        Row(modifier = Modifier.fillMaxWidth()) {
            val timeText = if (isSeeking) goTime.formatHourMinSec() else seekerState.currentTime.formatHourMinSec()
            Text(
                modifier = Modifier.padding(bottom = 2.dp, start = 24.dp),
                text = "$timeText / ${seekerState.totalDuration.formatHourMinSec()}",
                color = Color.White,
                style =
                    BVTypography.timecode.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 3f),
                    ),
            )
        }

        // Seek bar（可聚焦，处理方向键 + 触屏拖拽）
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .border(
                        width = 1.dp,
                        color =
                            MaterialTheme.colorScheme.border.copy(
                                alpha = if (isSeekFocused) 1f else 0f,
                            ),
                        shape = BVShape.small,
                    ).focusable()
                    .focusRequester(seekFocusRequester)
                    .pointerInput(seekerState.totalDuration) {
                        awaitEachGesture {
                            val firstDown =
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                            firstDown.consume()

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                val w = this.size.width.toFloat()

                                if (!change.pressed) {
                                    // 手指抬起：seek 到最终位置
                                    if (w > 0 && seekerState.totalDuration > 0) {
                                        val ratio = (change.position.x / w).coerceIn(0f, 1f)
                                        val targetTime = (ratio * seekerState.totalDuration).toLong()
                                        onSeekToPosition(targetTime)
                                    }
                                    change.consume()
                                    break
                                }

                                if (change.positionChanged()) {
                                    // 拖拽中：持续 seek 到触摸位置
                                    if (w > 0 && seekerState.totalDuration > 0) {
                                        val ratio = (change.position.x / w).coerceIn(0f, 1f)
                                        val targetTime = (ratio * seekerState.totalDuration).toLong()
                                        onSeekToPosition(targetTime)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }.onKeyEvent {
                        when (it.key) {
                            Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                                if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                                if (isSeeking) onSeekGoTime() else onPlayPause()
                                true
                            }

                            Key.DirectionLeft, Key.MediaRewind -> {
                                if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                                onDirectionLeft()
                                true
                            }

                            Key.DirectionRight, Key.MediaFastForward -> {
                                if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                                onDirectionRight()
                                true
                            }

                            Key.DirectionDown -> {
                                if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                                playPauseFocusRequester.safeRequestFocus()
                                true
                            }

                            else -> false
                        }
                    }.onFocusChanged { isSeekFocused = it.isFocused },
        ) {
            VideoProgressSeek(
                modifier = Modifier.focusable().fillMaxWidth(),
                duration = seekerState.totalDuration,
                position = if (isSeeking) goTime else seekerState.currentTime,
                bufferedPercentage = seekerState.bufferedPercentage,
                isPersistentSeek = false,
            )
        }

        // 快捷设置垂直微卡片浮层（位于进度条与操作按钮行之间，水平对齐触发按钮实际坐标）
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .onGloballyPositioned { coordinates ->
                        // 记录微卡片水平对齐基准容器的全局 X 坐标。
                        // 在 OSD 底栏初次展示完成时即刻捕获稳态值，微卡片在后续用户触发挂载时首帧直接读取该值，保证 0 帧延迟精准对齐。
                        popoverContainerRootX = coordinates.positionInRoot().x
                    },
        ) {
            var lastMenu by remember { mutableStateOf<VodQuickMenuType?>(null) }
            if (activeQuickMenu != null) {
                lastMenu = activeQuickMenu
            }
            val currentMenu = activeQuickMenu ?: lastMenu
            val isMenuVisible = activeQuickMenu != null

            androidx.compose.animation.AnimatedVisibility(
                visible = isMenuVisible && currentMenu != null,
                enter = EnterTransition.None,
                exit = fadeOut(tween(durationMillis = 250)),
            ) {
                if (currentMenu != null) {
                    AnimatedContent(
                        targetState = currentMenu,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 150)) togetherWith
                                fadeOut(tween(durationMillis = 150))
                        },
                        label = "VodQuickMenuTransition",
                    ) { menu ->
                        Box(
                            modifier =
                                Modifier
                                    .padding(bottom = 6.dp)
                                    .alignToQuickMenuAnchor(
                                        anchorRootXProvider = { quickMenuAnchorRootX[menu] },
                                        containerRootXProvider = { popoverContainerRootX },
                                    ),
                        ) {
                            val navigateLeftAction: (() -> Unit)? =
                                menu.previous()?.let { prev ->
                                    {
                                        activeQuickMenu = prev
                                        onResetAutoHide()
                                    }
                                }
                            val navigateRightAction: (() -> Unit)? =
                                menu.next(hasSubtitles = availableSubtitles.isNotEmpty())?.let { next ->
                                    {
                                        activeQuickMenu = next
                                        onResetAutoHide()
                                    }
                                }
                            when (menu) {
                                VodQuickMenuType.Resolution -> {
                                    val qualityList =
                                        remember(availableQuality) {
                                            availableQuality.keys.sortedDescending()
                                        }
                                    val items =
                                        qualityList.map { code ->
                                            val res = Resolution.fromCode(code)
                                            val label = availableQuality[code] ?: res.displayName
                                            val badge = if (code >= 120) "高画质" else null
                                            QuickMenuItem(
                                                id = code,
                                                label = label,
                                                badge = badge,
                                                isSelected = code == currentQualityId,
                                            )
                                        }
                                    PlayerQuickMenuPopover(
                                        visible = isMenuVisible,
                                        title = "清晰度",
                                        items = items,
                                        onSelect = {
                                            onResolutionChange(it)
                                            activeQuickMenu = null
                                            resolutionFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onDismiss = {
                                            activeQuickMenu = null
                                            resolutionFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onNavigateLeft = navigateLeftAction,
                                        onNavigateRight = navigateRightAction,
                                    )
                                }

                                VodQuickMenuType.PlaySpeed -> {
                                    val items =
                                        PlaySpeedItem.entries.map {
                                            QuickMenuItem(
                                                id = it.speed,
                                                label = it.displayName,
                                                isSelected = it.speed == playSpeed,
                                            )
                                        }
                                    PlayerQuickMenuPopover(
                                        visible = isMenuVisible,
                                        title = "播放倍速",
                                        items = items,
                                        onSelect = {
                                            onPlaySpeedChange(it)
                                            activeQuickMenu = null
                                            speedFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onDismiss = {
                                            activeQuickMenu = null
                                            speedFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onNavigateLeft = navigateLeftAction,
                                        onNavigateRight = navigateRightAction,
                                    )
                                }

                                VodQuickMenuType.Audio -> {
                                    val audioList =
                                        remember(availableAudio) {
                                            val list =
                                                if (availableAudio.isNotEmpty()) {
                                                    availableAudio
                                                } else {
                                                    Audio.entries
                                                }
                                            list.sortedByDescending { it.ordinal }
                                        }
                                    val items =
                                        audioList.map {
                                            QuickMenuItem(
                                                id = it,
                                                label = it.detailedDisplayName,
                                                badge = it.shortBadge,
                                                isSelected = it == currentAudio,
                                            )
                                        }
                                    PlayerQuickMenuPopover(
                                        visible = isMenuVisible,
                                        title = "音频音轨",
                                        items = items,
                                        onSelect = {
                                            onAudioChange(it)
                                            activeQuickMenu = null
                                            audioFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onDismiss = {
                                            activeQuickMenu = null
                                            audioFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onNavigateLeft = navigateLeftAction,
                                        onNavigateRight = navigateRightAction,
                                    )
                                }

                                VodQuickMenuType.Subtitle -> {
                                    val items =
                                        buildList {
                                            add(
                                                QuickMenuItem(
                                                    id = Subtitle.Empty,
                                                    label = "关闭",
                                                    isSelected = currentSubtitleId == -1L,
                                                ),
                                            )
                                            availableSubtitles.forEach { sub ->
                                                add(
                                                    QuickMenuItem(
                                                        id = sub,
                                                        label = sub.langDoc.ifBlank { "未知字幕" },
                                                        isSelected = sub.id == currentSubtitleId,
                                                    ),
                                                )
                                            }
                                        }
                                    PlayerQuickMenuPopover(
                                        visible = isMenuVisible,
                                        title = "字幕语言",
                                        items = items,
                                        onSelect = {
                                            onSubtitleChange(it)
                                            activeQuickMenu = null
                                            subtitleFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onDismiss = {
                                            activeQuickMenu = null
                                            subtitleFocusRequester.safeRequestFocus()
                                            onResetAutoHide()
                                        },
                                        onNavigateLeft = navigateLeftAction,
                                        onNavigateRight = navigateRightAction,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 操作按钮行（单行分段 + 动态状态徽标）
        var focusedDescription by remember { mutableStateOf("") }
        val currentRes = resolution ?: currentQualityId?.let { Resolution.fromCode(it) } ?: Resolution.R1080P
        val resBadge = currentRes.displayName
        val speedBadge = PlaySpeedItem.fromSpeed(playSpeed).displayName
        val audioBadge = currentAudio.shortBadge
        val subtitleBadge = if (currentSubtitleId == -1L) "字幕:关" else "字幕:开"

        val buttons =
            buildList {
                add(
                    ControllerButton(
                        id = "play_pause",
                        icon = R.drawable.play_pause_24px,
                        description = "播放 / 暂停",
                        focusRequester = playPauseFocusRequester,
                        action = onPlayPause,
                    ),
                )
                add(
                    ControllerButton(
                        id = "resolution",
                        badge = resBadge,
                        description = "清晰度：${availableQuality[currentQualityId] ?: resBadge} (按 OK 切换)",
                        focusRequester = resolutionFocusRequester,
                        quickMenuType = VodQuickMenuType.Resolution,
                        action = {
                            activeQuickMenu =
                                if (activeQuickMenu ==
                                    VodQuickMenuType.Resolution
                                ) {
                                    null
                                } else {
                                    VodQuickMenuType.Resolution
                                }
                            onResetAutoHide()
                        },
                    ),
                )
                add(
                    ControllerButton(
                        id = "speed",
                        badge = speedBadge,
                        description = "播放倍速：$speedBadge (按 OK 切换)",
                        focusRequester = speedFocusRequester,
                        quickMenuType = VodQuickMenuType.PlaySpeed,
                        action = {
                            activeQuickMenu =
                                if (activeQuickMenu == VodQuickMenuType.PlaySpeed) null else VodQuickMenuType.PlaySpeed
                            onResetAutoHide()
                        },
                    ),
                )
                add(
                    ControllerButton(
                        id = "audio",
                        badge = audioBadge,
                        description = "音频音轨：${currentAudio.detailedDisplayName} (按 OK 切换)",
                        focusRequester = audioFocusRequester,
                        quickMenuType = VodQuickMenuType.Audio,
                        action = {
                            activeQuickMenu =
                                if (activeQuickMenu == VodQuickMenuType.Audio) null else VodQuickMenuType.Audio
                            onResetAutoHide()
                        },
                    ),
                )
                if (availableSubtitles.isNotEmpty()) {
                    add(
                        ControllerButton(
                            id = "subtitle",
                            badge = subtitleBadge,
                            description =
                                "字幕语言：${availableSubtitles
                                    .find {
                                        it.id == currentSubtitleId
                                    }?.langDoc ?: "关闭"} (按 OK 切换)",
                            focusRequester = subtitleFocusRequester,
                            quickMenuType = VodQuickMenuType.Subtitle,
                            action = {
                                activeQuickMenu =
                                    if (activeQuickMenu ==
                                        VodQuickMenuType.Subtitle
                                    ) {
                                        null
                                    } else {
                                        VodQuickMenuType.Subtitle
                                    }
                                onResetAutoHide()
                            },
                        ),
                    )
                }
                add(
                    ControllerButton(
                        id = "danmaku",
                        icon = if (danmakuEnabled) R.drawable.danmaku_on_24px else R.drawable.danmaku_off_24px,
                        description = if (danmakuEnabled) "弹幕：开启" else "弹幕：关闭",
                        action = onDanmakuSwitchChange,
                    ),
                )
                if (episodesAvailable) {
                    add(
                        ControllerButton(
                            id = "episodes",
                            icon = R.drawable.playlist_play_24px,
                            description = "选集",
                            action = onShowEpisodes,
                        ),
                    )
                }
                if (relatedAvailable) {
                    add(
                        ControllerButton(
                            id = "related",
                            icon = R.drawable.related_videos_24px,
                            description = "相关推荐",
                            action = onShowRelatedVideos,
                        ),
                    )
                }
                if (!isPgc) {
                    add(
                        ControllerButton(
                            id = "video_info",
                            icon = R.drawable.info_24px,
                            description = "视频详情",
                            action = onGoToVideoInfo,
                        ),
                    )
                    add(
                        ControllerButton(
                            id = "up_space",
                            icon = R.drawable.contact_page_24px,
                            description = "UP 主空间",
                            action = onGoToUpPage,
                        ),
                    )
                }
                add(
                    ControllerButton(
                        id = "aspect_ratio",
                        icon = R.drawable.aspect_ratio_24px,
                        description = "画面比例：${aspectRatio.displayName}",
                        action = onCycleAspectRatio,
                    ),
                )
                add(
                    ControllerButton(
                        id = "loop",
                        icon = if (isLooping) R.drawable.repeat_one_on_24px else R.drawable.repeat_one_24px,
                        description = if (isLooping) "循环播放：开启" else "循环播放：关闭",
                        action = onToggleLoop,
                    ),
                )
                add(
                    ControllerButton(
                        id = "interaction",
                        icon = R.drawable.interaction_24px,
                        description = "点赞与互动",
                        action = onShowInteraction,
                    ),
                )
                add(
                    ControllerButton(
                        id = "comment",
                        icon = R.drawable.comment,
                        description = "评论区",
                        action = onShowComments,
                    ),
                )
                add(
                    ControllerButton(
                        id = "settings",
                        icon = R.drawable.settings_24px,
                        description = "更多高级设置",
                        action = {
                            activeQuickMenu = null
                            onShowSettings()
                        },
                    ),
                )
            }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onKeyEvent {
                        if (it.key == Key.DirectionUp) {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            seekFocusRequester.safeRequestFocus()
                            return@onKeyEvent true
                        }
                        if (it.key == Key.DirectionDown) {
                            if (episodesAvailable) {
                                if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                                onShowEpisodes()
                                return@onKeyEvent true
                            }
                            if (relatedAvailable) {
                                if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                                onShowRelatedVideos()
                                return@onKeyEvent true
                            }
                        }
                        false
                    }.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons.forEach { item ->
                key(item.id) {
                    val anchorMod =
                        if (item.quickMenuType != null) {
                            Modifier.onGloballyPositioned { coordinates ->
                                quickMenuAnchorRootX[item.quickMenuType] = coordinates.positionInRoot().x
                            }
                        } else {
                            Modifier
                        }
                    val focusMod =
                        if (item.focusRequester != null) {
                            Modifier.focusRequester(item.focusRequester)
                        } else {
                            Modifier
                        }
                    Surface(
                        modifier =
                            anchorMod
                                .height(46.dp)
                                .defaultMinSize(minWidth = 46.dp)
                                .then(focusMod)
                                .onFocusChanged {
                                    if (it.hasFocus) focusedDescription = item.description
                                }.touchClickable(
                                    onClick = item.action,
                                ),
                        onClick = item.action,
                        shape =
                            ClickableSurfaceDefaults.shape(
                                shape = if (item.badge != null) RoundedCornerShape(23.dp) else CircleShape,
                            ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
                        colors =
                            ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF0F0F0F),
                            ),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = if (item.badge != null) 12.dp else 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.badge != null) {
                                Text(
                                    text = item.badge,
                                    style =
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp,
                                        ),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            } else if (item.icon != null) {
                                Icon(
                                    painter = painterResource(id = item.icon),
                                    contentDescription = item.description,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        val defaultDownHint =
            when {
                episodesAvailable -> "按遥控器「向下」展开底部选集"
                relatedAvailable -> "按遥控器「向下」浏览相关推荐"
                else -> ""
            }
        val hintText = focusedDescription.ifBlank { defaultDownHint }
        if (hintText.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 28.dp, bottom = 14.dp),
                text = hintText,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 3f),
                    ),
                color = Color(0xFFE0E0E0),
            )
        }
    }
}

/**
 * 操作按钮数据模型（支持图标或文本角标）。
 */
private data class ControllerButton(
    val id: String,
    val icon: Int? = null,
    val badge: String? = null,
    val description: String,
    val focusRequester: FocusRequester? = null,
    val quickMenuType: VodQuickMenuType? = null,
    val action: () -> Unit,
)

/**
 * 时钟组件。
 */
@Composable
private fun Clock(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
) {
    Text(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        style =
            TextStyle(
                shadow = Shadow(color = Color.Black, blurRadius = 1f),
                fontFeatureSettings = "tnum",
                letterSpacing = 0.5.sp,
            ),
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 32.sp)) {
                    append("$hour".padStart(2, '0'))
                    append(":")
                    append("$minute".padStart(2, '0'))
                }
            },
    )
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ControllerVideoInfoPreview() {
    BVTheme {
        ControllerVideoInfo(
            show = true,
            isSeeking = false,
            goTime = 0L,
            seekerState =
                SeekerState(
                    totalDuration = 600_000L,
                    currentTime = 120_000L,
                    bufferedPercentage = 50,
                ),
            title = "示例视频标题",
            clock = Pair(14, 30),
            onlineWatching = "9.4万+",
            videoShot = null,
            videoShotCache = VideoShotImageCache(),
            isPgc = false,
            episodesAvailable = true,
            relatedAvailable = true,
            danmakuEnabled = true,
            isLooping = false,
            onDirectionLeft = {},
            onDirectionRight = {},
            onSeekGoTime = {},
            onSeekToPosition = {},
            onPlayPause = {},
            onDanmakuSwitchChange = {},
            onShowSettings = {},
            onShowEpisodes = {},
            onShowRelatedVideos = {},
            onGoToVideoInfo = {},
            onToggleLoop = {},
            onGoToUpPage = {},
            onShowInteraction = {},
            onShowComments = {},
        )
    }
}

// endregion
