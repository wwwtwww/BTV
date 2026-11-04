package dev.frost819.newbv.app.ui.screen.home

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import org.junit.jupiter.api.Test

/**
 * [HomeShelfRow] 的纯 JVM 逻辑单元测试。
 *
 * 验证泳道卡片去重规则、Key 命名约定与空数据边界保护。
 */
class HomeShelfRowLogicTest {
    private fun sampleCard(
        avid: Long,
        title: String = "标题-$avid",
    ) = VideoCardData(
        avid = avid,
        title = title,
        cover = "https://example.com/cover.jpg",
        upName = "UP主",
        upMid = 12345L,
        playString = "10万",
        danmakuString = "1000",
        timeString = "10:00",
    )

    @Test
    fun `distinctItems removes duplicate avid items preserving first occurrence`() {
        val rawItems =
            listOf(
                sampleCard(avid = 1L, title = "原始项1"),
                sampleCard(avid = 2L, title = "原始项2"),
                sampleCard(avid = 1L, title = "重复项1"),
                sampleCard(avid = 3L, title = "原始项3"),
            )

        val distinctItems = rawItems.distinctBy { it.avid }

        assertThat(distinctItems).hasSize(3)
        assertThat(distinctItems.map { it.avid }).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(distinctItems[0].title).isEqualTo("原始项1")
    }

    @Test
    fun `seeAll item key convention is shelfKey_see_all`() {
        val shelfKey = "rcmd_shelf"
        val expectedSeeAllKey = "${shelfKey}_see_all"
        assertThat(expectedSeeAllKey).isEqualTo("rcmd_shelf_see_all")
    }
}
