package dev.frost819.newbv.app.ui.component.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.video.Dimension
import dev.frost819.newbv.biliapi.entity.video.VideoPage
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [EpisodeTrackState] 与 [buildEpisodeTrackState] 的单元测试。
 *
 * 覆盖单视频多 P 展平、PGC 剧集文案格式化、超长集数 50 集区间分组、
 * 合集嵌套子分 P 二级轨构建，以及异常入参校验。
 */
class EpisodeTrackModelTest {
    private val defaultDimension = Dimension(width = 1920, height = 1080)

    private fun page(
        index: Int,
        cid: Long,
        title: String,
    ) = VideoPage(
        cid = cid,
        index = index,
        title = title,
        duration = 300,
        dimension = defaultDimension,
    )

    @Test
    fun `hasMultipleEpisodes returns false for empty list and single 1P item`() {
        assertThat(emptyList<VideoListItem>().hasMultipleEpisodes()).isFalse()

        val singleWithoutPages = listOf(VideoListItem(aid = 1L, cid = 10L, title = "单集视频"))
        assertThat(singleWithoutPages.hasMultipleEpisodes()).isFalse()

        val singleWithOnePage =
            listOf(
                VideoListItem(
                    aid = 1L,
                    cid = 10L,
                    title = "单集视频",
                    ugcPages = listOf(page(index = 1, cid = 10L, title = "P1")),
                ),
            )
        assertThat(singleWithOnePage.hasMultipleEpisodes()).isFalse()
    }

    @Test
    fun `hasMultipleEpisodes returns true for multi-episode list and single video multi-P`() {
        val multiEpisode =
            listOf(
                VideoListItem(aid = 1L, cid = 10L, title = "第一集"),
                VideoListItem(aid = 2L, cid = 20L, title = "第二集"),
            )
        assertThat(multiEpisode.hasMultipleEpisodes()).isTrue()

        val singleMultiPage =
            listOf(
                VideoListItem(
                    aid = 1L,
                    cid = 10L,
                    title = "教程合集",
                    ugcPages =
                        listOf(
                            page(index = 1, cid = 10L, title = "环境搭建"),
                            page(index = 2, cid = 11L, title = "基础语法"),
                        ),
                ),
            )
        assertThat(singleMultiPage.hasMultipleEpisodes()).isTrue()
    }

    @Test
    fun `buildEpisodeTrackState flattens single video multi-P into primary track and propagates page title`() {
        // Given
        val parent =
            VideoListItem(
                aid = 99L,
                cid = 1001L,
                title = "Kotlin 实战课",
                ugcPages =
                    listOf(
                        page(index = 1, cid = 1001L, title = "1"),
                        page(index = 2, cid = 1002L, title = "协程原理"),
                        page(index = 3, cid = 1003L, title = ""),
                    ),
            )

        // When
        val state = buildEpisodeTrackState(videoList = listOf(parent), currentCid = 1002L)

        // Then
        assertThat(state.headerTitle).isEqualTo("分P选集")
        assertThat(state.groups).isEmpty()
        assertThat(state.items).hasSize(3)
        assertThat(state.activeItemIndex).isEqualTo(1)
        assertThat(state.activeGroupIndex).isEqualTo(0)

        assertThat(state.items[0].badge).isEmpty()
        assertThat(state.items[0].title).isEqualTo("P1")
        assertThat(state.items[0].isCurrent).isFalse()

        assertThat(state.items[1].badge).isEqualTo("P2")
        assertThat(state.items[1].title).isEqualTo("协程原理")
        assertThat(state.items[1].isCurrent).isTrue()
        assertThat(state.items[1].targetItem.cid).isEqualTo(1002L)
        assertThat(state.items[1].targetItem.title).isEqualTo("协程原理")

        assertThat(state.items[2].title).isEqualTo("P3")
        assertThat(state.items[2].targetItem.title).isEqualTo("Kotlin 实战课")
    }

    @Test
    fun `buildEpisodeTrackState formats PGC numeric and special episode titles cleanly`() {
        // Given
        val pgcList =
            listOf(
                VideoListItem(aid = 1L, cid = 11L, epid = 501, seasonId = 88, title = "1"),
                VideoListItem(aid = 2L, cid = 12L, epid = 502, seasonId = 88, title = "2"),
                VideoListItem(aid = 3L, cid = 13L, epid = 503, seasonId = 88, title = "PV1 预告"),
            )

        // When
        val state = buildEpisodeTrackState(videoList = pgcList, currentCid = 12L)

        // Then
        assertThat(state.headerTitle).isEqualTo("选集")
        assertThat(state.items.map { it.title }).containsExactly("第 1 话", "第 2 话", "PV1 预告").inOrder()
        assertThat(state.items.map { it.badge }).containsExactly("", "", "")
        assertThat(state.activeItemIndex).isEqualTo(1)
    }

    @Test
    fun `buildEpisodeTrackState paginates 120 episodes into 50-item groups and locates active item`() {
        // Given
        val longList =
            (1..120).map { num ->
                VideoListItem(
                    aid = num.toLong(),
                    cid = (num * 10).toLong(),
                    epid = num,
                    seasonId = 1000,
                    title = num.toString(),
                )
            }

        // When: 当前播放第 73 话（索引 72，位于第 2 组 51-100）
        val state = buildEpisodeTrackState(videoList = longList, currentCid = 730L)

        // Then
        assertThat(state.groups.map { it.label }).containsExactly("1-50", "51-100", "101-120").inOrder()
        assertThat(state.activeItemIndex).isEqualTo(72)
        assertThat(state.activeGroupIndex).isEqualTo(1)
        assertThat(state.localIndexInGroup(state.activeItemIndex, groupIndex = 1)).isEqualTo(22)
        assertThat(state.localIndexInGroup(state.activeItemIndex, groupIndex = 0)).isEqualTo(-1)

        val secondGroupItems = state.itemsInGroup(1)
        assertThat(secondGroupItems).hasSize(50)
        assertThat(secondGroupItems.first().title).isEqualTo("第 51 话")
        assertThat(secondGroupItems.last().title).isEqualTo("第 100 话")
        assertThat(secondGroupItems[22].isCurrent).isTrue()

        val thirdGroupItems = state.itemsInGroup(2)
        assertThat(thirdGroupItems).hasSize(20)
        assertThat(thirdGroupItems.first().title).isEqualTo("第 101 话")
        assertThat(thirdGroupItems.last().title).isEqualTo("第 120 话")
    }

    @Test
    fun `buildEpisodeTrackState builds nested subPages only for UGC season items with multiple pages`() {
        // Given
        val seasonList =
            listOf(
                VideoListItem(
                    aid = 10L,
                    cid = 100L,
                    title = "合集导言",
                    ugcPages = listOf(page(index = 1, cid = 100L, title = "导言")),
                ),
                VideoListItem(
                    aid = 20L,
                    cid = 201L,
                    title = "核心章节",
                    ugcPages =
                        listOf(
                            page(index = 1, cid = 201L, title = "上篇"),
                            page(index = 2, cid = 202L, title = "下篇"),
                        ),
                ),
            )

        // When
        val state = buildEpisodeTrackState(videoList = seasonList, currentCid = 202L, currentAid = 20L)

        // Then: 单 P 合集项不产生冗余二级轨，多 P 合集项保留二级分 P 胶囊并命中当前子分 P
        assertThat(state.items[0].subPages).isEmpty()
        assertThat(state.items[0].isCurrent).isFalse()

        assertThat(state.items[1].isCurrent).isTrue()
        assertThat(state.items[1].subPages).hasSize(2)
        assertThat(state.items[1].subPages[0].isCurrent).isFalse()
        val secondSub = state.items[1].subPages[1]
        assertThat(secondSub.isCurrent).isTrue()
        assertThat(secondSub.badge).isEqualTo("P2")
        assertThat(secondSub.title).isEqualTo("下篇")
        assertThat(secondSub.targetItem.cid).isEqualTo(202L)
        assertThat(secondSub.targetItem.title).isEqualTo("下篇")
    }

    @Test
    fun `buildEpisodeTrackState disambiguates active item by currentAid`() {
        // Given
        val listWithSameCid =
            listOf(
                VideoListItem(aid = 1L, cid = 999L, title = "视频 A"),
                VideoListItem(aid = 2L, cid = 999L, title = "视频 B"),
            )

        // When
        val state = buildEpisodeTrackState(videoList = listWithSameCid, currentCid = 999L, currentAid = 2L)

        // Then
        assertThat(state.activeItemIndex).isEqualTo(1)
        assertThat(state.items[0].isCurrent).isFalse()
        assertThat(state.items[1].isCurrent).isTrue()
    }

    @Test
    fun `buildEpisodeTrackState throws IllegalArgumentException when groupSize is non-positive`() {
        assertFailsWith<IllegalArgumentException> {
            buildEpisodeTrackState(videoList = emptyList(), currentCid = 1L, groupSize = 0)
        }
    }
}
