package dev.frost819.newbv.app.ui.component.player

import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.VideoPage

/**
 * 超长集数默认分组容量（每 50 集/分 P 一组，如 1-50、51-100）。
 */
const val EPISODE_GROUP_SIZE = 50

/**
 * 判断播放列表是否存在可切换的多个分集或分 P。
 *
 * 满足以下任一条件返回 `true`：
 * 1. 列表包含多个视频项（番剧多集、UGC 合集等）；
 * 2. 单视频项内包含超过 1 个 `ugcPages` 子分 P。
 */
fun List<VideoListItem>.hasMultipleEpisodes(): Boolean {
    if (isEmpty()) return false
    if (size > 1) return true
    return (first().ugcPages?.size ?: 0) > 1
}

/**
 * 超长集数横向分组区间模型。
 *
 * @property groupIndex 分组索引（0 基）。
 * @property startIndex 主轨列表起始下标（包含）。
 * @property endIndexExclusive 主轨列表结束下标（不包含）。
 * @property label 分组胶囊展示文案（如 "1-50"、"51-100"）。
 */
data class EpisodeGroupRange(
    val groupIndex: Int,
    val startIndex: Int,
    val endIndexExclusive: Int,
    val label: String,
)

/**
 * 二级子分 P 胶囊数据项（仅在「多集 UGC 合集 + 单集内部含多 P」场景下生成）。
 *
 * @property key 唯一稳定键。
 * @property badge 前缀角标（如 "P1"，若标题本身已是 P1 则为空串）。
 * @property title 胶囊主标题。
 * @property targetItem 点击播放时回传的目标 [VideoListItem]。
 * @property isCurrent 是否为当前正在播放的子分 P。
 */
data class SubPageCapsuleItem(
    val key: String,
    val badge: String,
    val title: String,
    val targetItem: VideoListItem,
    val isCurrent: Boolean,
)

/**
 * 主选集轨胶囊数据项。
 *
 * @property key 唯一稳定键。
 * @property badge 前缀序号角标（如 "P1"、"1"，无需前缀时为空串）。
 * @property title 胶囊主标题。
 * @property targetItem 点击该胶囊播放的目标 [VideoListItem]。
 * @property isCurrent 当前是否正在播放该集（或其下属子分 P）。
 * @property subPages 二级子分 P 列表（单视频多 P 已展平至主轨，此处为空；仅合集内多 P 时非空）。
 */
data class EpisodeCapsuleItem(
    val key: String,
    val badge: String,
    val title: String,
    val targetItem: VideoListItem,
    val isCurrent: Boolean,
    val subPages: List<SubPageCapsuleItem> = emptyList(),
)

/**
 * 播放器底部横向胶囊选集轨完整派生状态。
 *
 * @property headerTitle 轨头标题（"分P选集" 或 "选集"）。
 * @property items 主轨全部胶囊列表。
 * @property groups 超长集数分组区间列表（总集数不超过阈值时为空列表，不展示分组栏）。
 * @property activeItemIndex 当前播放项在 [items] 中的索引（未匹配时为 -1）。
 * @property activeGroupIndex 当前播放项所属分组在 [groups] 中的索引（无分组时为 0）。
 */
data class EpisodeTrackState(
    val headerTitle: String,
    val items: List<EpisodeCapsuleItem>,
    val groups: List<EpisodeGroupRange>,
    val activeItemIndex: Int,
    val activeGroupIndex: Int,
) {
    /**
     * 获取指定分组区间内的主轨胶囊子列表。
     *
     * @param groupIndex 分组索引。
     * @return 对应分组内的 [EpisodeCapsuleItem] 列表；未启用分组时返回全量 [items]。
     */
    fun itemsInGroup(groupIndex: Int): List<EpisodeCapsuleItem> {
        if (groups.isEmpty()) return items
        val safeIndex = groupIndex.coerceIn(0, groups.lastIndex)
        val range = groups[safeIndex]
        return items.subList(range.startIndex, range.endIndexExclusive)
    }

    /**
     * 计算全局主轨索引在指定分组内的局部偏移索引。
     *
     * @param globalIndex 在 [items] 中的全局下标。
     * @param groupIndex 目标分组索引。
     * @return 若 [globalIndex] 属于该分组则返回局部下标（0 基），否则返回 -1。
     */
    fun localIndexInGroup(
        globalIndex: Int,
        groupIndex: Int,
    ): Int {
        if (globalIndex !in items.indices) return -1
        if (groups.isEmpty()) return globalIndex
        val safeGroup = groupIndex.coerceIn(0, groups.lastIndex)
        val range = groups[safeGroup]
        return if (globalIndex in range.startIndex until range.endIndexExclusive) {
            globalIndex - range.startIndex
        } else {
            -1
        }
    }
}

/**
 * 根据播放列表与当前播放标识构建 [EpisodeTrackState]。
 *
 * 规则：
 * 1. 当 [videoList] 仅有 1 项且其 `ugcPages` 数量 > 1 时，自动将分 P 展平至主选集轨（支持超长多 P 50 集分组）；
 * 2. 当 [videoList] 有多项时，主轨展示剧集/合集列表，若某集内部仍有多个 `ugcPages` 则挂载到 [EpisodeCapsuleItem.subPages]；
 * 3. 当主轨总项数超过 [groupSize] 时，按 [groupSize] 自动切分为 "1-50"、"51-100" 等区间胶囊。
 *
 * @param videoList 原始播放列表。
 * @param currentCid 当前播放 CID。
 * @param currentAid 当前播放 AID（可选，用于合集内精准消歧）。
 * @param groupSize 分组分页大小，默认 [EPISODE_GROUP_SIZE]。
 * @return 构建完成的 [EpisodeTrackState]。
 */
fun buildEpisodeTrackState(
    videoList: List<VideoListItem>,
    currentCid: Long,
    currentAid: Long = 0L,
    groupSize: Int = EPISODE_GROUP_SIZE,
): EpisodeTrackState {
    require(groupSize > 0) { "groupSize must be positive, was $groupSize" }

    val singleItemPages = videoList.singleOrNull()?.ugcPages
    val isFlattenedMultiPage = singleItemPages != null && singleItemPages.size > 1

    val headerTitle: String
    val primaryItems: List<EpisodeCapsuleItem>

    if (isFlattenedMultiPage) {
        val parent = videoList.first()
        headerTitle = "分P选集"
        primaryItems =
            singleItemPages.map { page ->
                val (badge, title) = formatPageBadgeAndTitle(page)
                EpisodeCapsuleItem(
                    key = "page_${parent.aid}_${page.cid}_${page.index}",
                    badge = badge,
                    title = title,
                    targetItem = parent.copy(cid = page.cid, title = page.title.trim().ifEmpty { parent.title }),
                    isCurrent = page.cid == currentCid,
                )
            }
    } else {
        headerTitle = "选集"
        val matchedIndex = findActiveEpisodeIndex(videoList, currentCid, currentAid)
        primaryItems =
            videoList.mapIndexed { index, video ->
                val isItemCurrent = index == matchedIndex
                val (badge, title) = formatEpisodeBadgeAndTitle(index, video)
                val pages = video.ugcPages
                val subPages =
                    if (pages != null && pages.size > 1) {
                        pages.map { page ->
                            val (subBadge, subTitle) = formatPageBadgeAndTitle(page)
                            SubPageCapsuleItem(
                                key = "sub_${video.aid}_${page.cid}_${page.index}",
                                badge = subBadge,
                                title = subTitle,
                                targetItem =
                                    video.copy(
                                        cid = page.cid,
                                        title = page.title.trim().ifEmpty { video.title },
                                    ),
                                isCurrent = isItemCurrent && page.cid == currentCid,
                            )
                        }
                    } else {
                        emptyList()
                    }
                EpisodeCapsuleItem(
                    key = "ep_${video.aid}_${video.cid}_${video.epid ?: 0L}_$index",
                    badge = badge,
                    title = title,
                    targetItem = video,
                    isCurrent = isItemCurrent,
                    subPages = subPages,
                )
            }
    }

    val activeItemIndex = primaryItems.indexOfFirst { it.isCurrent }
    val groups = buildGroupRanges(totalSize = primaryItems.size, groupSize = groupSize)
    val activeGroupIndex =
        if (groups.isEmpty() || activeItemIndex < 0) {
            0
        } else {
            (activeItemIndex / groupSize).coerceIn(0, groups.lastIndex)
        }

    return EpisodeTrackState(
        headerTitle = headerTitle,
        items = primaryItems,
        groups = groups,
        activeItemIndex = activeItemIndex,
        activeGroupIndex = activeGroupIndex,
    )
}

private fun findActiveEpisodeIndex(
    videoList: List<VideoListItem>,
    currentCid: Long,
    currentAid: Long,
): Int {
    if (currentAid > 0L) {
        val exactMatch =
            videoList.indexOfFirst { video ->
                video.aid == currentAid &&
                    (video.cid == currentCid || video.ugcPages?.any { it.cid == currentCid } == true)
            }
        if (exactMatch != -1) return exactMatch
    }
    return videoList.indexOfFirst { video ->
        video.cid == currentCid || video.ugcPages?.any { it.cid == currentCid } == true
    }
}

private fun formatPageBadgeAndTitle(page: VideoPage): Pair<String, String> {
    val pageBadge = "P${page.index}"
    val rawTitle = page.title.trim()
    val isRedundantLabel =
        rawTitle.isEmpty() ||
            rawTitle == page.index.toString() ||
            rawTitle.equals(pageBadge, ignoreCase = true)
    return if (isRedundantLabel) {
        "" to pageBadge
    } else {
        pageBadge to rawTitle
    }
}

private fun formatEpisodeBadgeAndTitle(
    index: Int,
    video: VideoListItem,
): Pair<String, String> {
    val rawTitle = video.title.trim()
    val oneBasedIndex = index + 1
    if (rawTitle.isNotEmpty() && rawTitle.all { it.isDigit() }) {
        val unit = if (video.epid != null) "话" else "集"
        return "" to "第 $rawTitle $unit"
    }
    if (video.epid != null) {
        return "" to rawTitle.ifEmpty { "第 $oneBasedIndex 话" }
    }
    val hasExplicitPrefix =
        rawTitle.startsWith("第") ||
            rawTitle.startsWith("$oneBasedIndex.") ||
            rawTitle.startsWith("$oneBasedIndex ") ||
            rawTitle.startsWith("EP", ignoreCase = true)
    return if (hasExplicitPrefix) {
        "" to rawTitle
    } else {
        "$oneBasedIndex" to rawTitle.ifEmpty { "第 $oneBasedIndex 集" }
    }
}

private fun buildGroupRanges(
    totalSize: Int,
    groupSize: Int,
): List<EpisodeGroupRange> {
    if (totalSize <= groupSize) return emptyList()
    val groupCount = (totalSize + groupSize - 1) / groupSize
    return List(groupCount) { groupIndex ->
        val start = groupIndex * groupSize
        val endExclusive = ((groupIndex + 1) * groupSize).coerceAtMost(totalSize)
        EpisodeGroupRange(
            groupIndex = groupIndex,
            startIndex = start,
            endIndexExclusive = endExclusive,
            label = "${start + 1}-$endExclusive",
        )
    }
}
