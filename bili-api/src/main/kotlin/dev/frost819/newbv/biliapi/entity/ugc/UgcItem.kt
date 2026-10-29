package dev.frost819.newbv.biliapi.entity.ugc

import dev.frost819.newbv.biliapi.http.entity.home.RcmdIndexData
import dev.frost819.newbv.biliapi.http.entity.home.RcmdTopData
import dev.frost819.newbv.biliapi.http.util.smartDate
import dev.frost819.newbv.biliapi.http.util.toSmartDate
import dev.frost819.newbv.biliapi.util.convertStringTimeToSeconds

/**
 * UGC 列表通用视频条目。
 *
 * @property aid 视频 AV 号。
 * @property bvid 视频 BV 号。
 * @property title 视频标题。
 * @property cover 封面图 URL。
 * @property author UP 主名称。
 * @property authorMid UP 主 MID。
 * @property play 播放量（未知时为 -1）。
 * @property danmaku 弹幕数（未知时为 -1）。
 * @property duration 时长（秒）。
 * @property idx 游标索引。
 * @property pubTime 格式化后的发布时间文本。
 * @property tagName 子分区或分类标签（可选）。
 * @property rcmdReason 推荐理由角标文案（可选）。
 * @property description 视频简介正文（可选）。
 */
data class UgcItem(
    val aid: Long,
    val bvid: String = "",
    val title: String,
    val cover: String,
    val author: String,
    val authorMid: Long?,
    val play: Int,
    val danmaku: Int,
    val duration: Int,
    val idx: Int = -1,
    val pubTime: String? = null,
    val tagName: String? = null,
    val rcmdReason: String? = null,
    val description: String? = null,
) {
    companion object {
        fun fromRcmdItem(rcmdItem: RcmdIndexData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.args.aid ?: 0,
                title = rcmdItem.title.orEmpty(),
                cover = rcmdItem.cover.orEmpty(),
                author = rcmdItem.args.upName ?: "",
                authorMid = rcmdItem.args.upId,
                play =
                    with(rcmdItem.coverLeftText1) {
                        runCatching {
                            val raw = this ?: return@with -1
                            if (raw.endsWith("万")) {
                                (raw.substring(0, raw.length - 1).toDouble() * 10000).toInt()
                            } else {
                                raw.toInt()
                            }
                        }.getOrDefault(-1)
                    },
                danmaku =
                    with(rcmdItem.coverLeftText2) {
                        if (this == null) return@with -1
                        runCatching {
                            if (this.endsWith("万")) {
                                (this.substring(0, this.length - 1).toDouble() * 10000).toInt()
                            } else {
                                this.toInt()
                            }
                        }.getOrDefault(-1)
                    },
                duration =
                    rcmdItem.playerArgs?.duration
                        ?: rcmdItem.coverRightText?.convertStringTimeToSeconds()
                        ?: 0,
                idx = rcmdItem.idx,
                tagName =
                    rcmdItem.args.tname?.takeIf { it.isNotBlank() }
                        ?: rcmdItem.args.rname?.takeIf { it.isNotBlank() },
                rcmdReason =
                    (rcmdItem.rcmdReason ?: rcmdItem.rcmdReasonStyle?.text)
                        ?.takeIf { it.isNotBlank() },
                description = rcmdItem.desc?.takeIf { it.isNotBlank() && it != "-" },
            )

        fun fromRcmdItem(rcmdItem: RcmdTopData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.id,
                bvid = rcmdItem.bvid,
                title = rcmdItem.title,
                cover = rcmdItem.pic,
                author = rcmdItem.owner?.name ?: "",
                authorMid = rcmdItem.owner?.mid,
                play = rcmdItem.stat?.view ?: -1,
                danmaku = rcmdItem.stat?.danmaku ?: -1,
                duration = rcmdItem.duration,
                pubTime = rcmdItem.pubdate.smartDate,
                rcmdReason =
                    rcmdItem.rcmdReason?.content?.takeIf { it.isNotBlank() }
                        ?: if (rcmdItem.isFollowed == 1) "已关注" else null,
            )

        fun fromVideoInfo(videoInfo: dev.frost819.newbv.biliapi.http.entity.video.VideoInfo) =
            UgcItem(
                aid = videoInfo.aid,
                bvid = videoInfo.bvid,
                title = videoInfo.title,
                duration = videoInfo.duration,
                author = videoInfo.owner.name,
                authorMid = videoInfo.owner.mid,
                cover = videoInfo.pic,
                play = videoInfo.stat.view,
                danmaku = videoInfo.stat.danmaku,
                pubTime = videoInfo.pubdate.smartDate,
                tagName = videoInfo.tname.takeIf { it.isNotBlank() },
                rcmdReason = videoInfo.rcmdReason?.content?.takeIf { it.isNotBlank() },
                description = videoInfo.desc.takeIf { it.isNotBlank() && it != "-" },
            )

        fun fromSmallCoverV5(card: bilibili.app.card.v1.SmallCoverV5): UgcItem {
            // 格式："n.n万观看 · n天前"
            val playAndPubTime = card.rightDesc2.split(" · ")
            val play = playAndPubTime.getOrNull(0)?.let { convertPlayStringToInt(it) } ?: -1
            val pubTime = playAndPubTime.getOrNull(1)
            val reason =
                if (card.hasRcmdReasonStyle()) {
                    card.rcmdReasonStyle.text.takeIf { it.isNotBlank() }
                } else {
                    null
                }

            return UgcItem(
                aid = card.base.param.toLong(),
                title = card.base.title,
                duration = convertStringTimeToSeconds(card.coverRightText1),
                author = card.rightDesc1,
                authorMid = card.up.id,
                cover = card.base.cover,
                play = play,
                pubTime = pubTime,
                danmaku = -1,
                idx = card.base.idx.toInt(),
                rcmdReason = reason,
            )
        }

        @Deprecated("User region v2 instead")
        fun fromRegionDynamicListItem(item: dev.frost819.newbv.biliapi.http.entity.region.RegionDynamicList.Item) =
            UgcItem(
                aid = item.param.toLong(),
                title = item.title,
                duration = item.duration,
                author = item.name,
                authorMid = null,
                cover = item.cover,
                play = item.play ?: -1,
                danmaku = item.danmaku ?: -1,
                pubTime = item.pubDate.smartDate,
            )

        fun fromRegionRcmdArchive(archive: dev.frost819.newbv.biliapi.http.entity.region.RegionFeedRcmd.Archive) =
            UgcItem(
                aid = archive.aid,
                title = archive.title,
                duration = archive.duration,
                author = archive.author.name,
                authorMid = archive.author.mid,
                cover = archive.cover,
                play = archive.stat.view,
                danmaku = archive.stat.danmaku,
                pubTime = archive.pubdate.toSmartDate(),
            )
    }
}

private fun convertStringTimeToSeconds(time: String): Int {
    if (time.isBlank()) return 0
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}

private fun convertPlayStringToInt(text: String): Int {
    if (text.isBlank()) return -1

    val value = text.replace("观看", "").trim()

    return try {
        when {
            value.endsWith("万") -> {
                val num = value.removeSuffix("万").toDouble()
                (num * 10_000).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }

            value.endsWith("亿") -> {
                val num = value.removeSuffix("亿").toDouble()
                (num * 100_000_000).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }

            else -> {
                value.toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        }
    } catch (e: Exception) {
        -1
    }
}
