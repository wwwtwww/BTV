package dev.frost819.newbv.app.ui.state.player

import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import dev.frost819.newbv.bilisubtitle.entity.SubtitleItem
import dev.frost819.newbv.danmaku.config.DanmakuState
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 播放器主 UI 状态（低频更新）。
 *
 * 仅在播放状态发生实质性变化时更新，不包含高频的进度信息。
 * 高频进度信息使用独立的 [SeekerState]。
 *
 * @see SeekerState
 * @see PlayerState
 */
data class PlayerUiState(
    // 视频信息
    val aid: Long = 0,
    val cid: Long = 0,
    val epid: Int? = null,
    val seasonId: Int = 0,
    val authorMid: Long = 0,
    val authorName: String = "",
    val title: String = "",
    val onlineWatching: String = "",
    val videoHeight: Int = 0,
    val videoWidth: Int = 0,
    val lastPlayed: Int = 0,
    val fromSeason: Boolean = false,
    val subType: Int = 0,
    // 播放状态
    val playerState: PlayerState = PlayerState.Ready,
    val isBuffering: Boolean = false,
    val videoShot: VideoShot? = null,
    val clock: Pair<Int, Int> = Pair(0, 0),
    // 提示与连播浮层
    val showSkipToNextEp: Boolean = false,
    val nextPlayInfo: NextPlayOverlayInfo? = null,
    val nextPlayCountdownSec: Int = 0,
    val showBackToStart: Boolean = false,
    val showPreviewTip: Boolean = false,
    val shortcutTipText: String? = null,
    // 可用资源
    val availableQuality: Map<Int, String> = emptyMap(),
    val availableVideoCodec: List<VideoCodec> = emptyList(),
    val availableAudio: List<Audio> = emptyList(),
    // 当前选中状态
    val mediaProfileState: MediaProfileState = MediaProfileState(),
    val playSpeed: Float = 1f,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.Default,
    val isLooping: Boolean = false,
    // 弹幕
    val danmakuState: DanmakuState = DanmakuState(),
    val danmakuMask: DanmakuMask? = null,
    // 字幕
    val subtitleState: SubtitleState = SubtitleState(),
    val subtitleId: Long = -1L,
    val subtitleData: List<SubtitleItem> = emptyList(),
    val subtitleList: List<Subtitle> = emptyList(),
    // 列表
    val videoList: List<VideoListItem> = emptyList(),
    val relatedVideos: List<VideoCardData> = emptyList(),
    // 当前网络与 CDN 调试信息
    val currentCdnUrl: String? = null,
) {
    /**
     * 是否为番剧（PGC）播放内容。
     *
     * 由 [epid] 推导：有有效 EP ID 即视为番剧。UI 层据此隐藏
     * “视频信息 / up主页 / 相关视频”等仅 UGC 适用的入口。
     * 与播放侧 [fromSeason]（走 PGC 播放接口/心跳）相互独立。
     */
    val isPgc: Boolean get() = (epid ?: 0) != 0
}

/**
 * 播毕下一集/推荐微浮层信息。
 *
 * @property title 下一个视频标题
 * @property cover 封面图片 URL
 * @property author UP 主名称
 * @property isEpisode 是否为分集/连续播放（true 为下一集，false 为相关推荐）
 */
data class NextPlayOverlayInfo(
    val title: String,
    val cover: String? = null,
    val author: String? = null,
    val isEpisode: Boolean = true,
)

/**
 * 播放器进度条状态（高频更新，100ms 间隔）。
 *
 * 与 [PlayerUiState] 分离，避免高频进度更新触发不必要的 UI 重组。
 *
 * @property totalDuration 视频总时长（毫秒）
 * @property currentTime 当前播放位置（毫秒）
 * @property bufferedPercentage 缓冲百分比（0-100）
 * @property debugInfo 调试信息字符串
 */
data class SeekerState(
    val totalDuration: Long = 0L,
    val currentTime: Long = 0L,
    val bufferedPercentage: Int = 0,
    val debugInfo: String = "",
)

/**
 * 媒体格式状态。
 *
 * @property qualityId 画质 ID（B 站 qn 参数）
 * @property videoCodec 视频编码
 * @property audio 音频配置
 */
data class MediaProfileState(
    val qualityId: Int = 80,
    val videoCodec: VideoCodec = VideoCodec.AVC,
    val audio: Audio = Audio.A192K,
)

/**
 * 字幕显示状态。
 *
 * @property fontSize 字体大小（sp）
 * @property opacity 背景透明度（0-1）
 * @property bottomPadding 底部间距（dp）
 */
data class SubtitleState(
    val fontSize: Int = 24,
    val opacity: Float = 0.4f,
    val bottomPadding: Int = 12,
)

/**
 * 播放错误类型分类。
 */
enum class PlayerErrorType {
    /** 网络连接断开（无网络）。 */
    NetworkDisconnected,

    /** 网络缓冲或加载超时。 */
    Timeout,

    /** 视频源解析失败。 */
    SourceResolutionFailed,

    /** 播放器解码或流媒体渲染异常。 */
    DecoderError,

    /** 通用或其他未知异常。 */
    Generic,
}

/**
 * 播放器状态枚举。
 */
sealed class PlayerState {
    /** 准备就绪。 */
    data object Ready : PlayerState()

    /** 正在播放。 */
    data object Playing : PlayerState()

    /** 已暂停。 */
    data object Paused : PlayerState()

    /** 播放结束。 */
    data object Ended : PlayerState()

    /** 发生错误。
     * @param message 错误信息
     * @param type 错误分类
     * @param canDowngradeQuality 是否支持尝试降级画质重试
     */
    data class Error(
        val message: String,
        val type: PlayerErrorType = PlayerErrorType.Generic,
        val canDowngradeQuality: Boolean = false,
    ) : PlayerState()
}
