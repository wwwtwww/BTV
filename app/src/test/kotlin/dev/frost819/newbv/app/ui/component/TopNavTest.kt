package dev.frost819.newbv.app.ui.component

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.screen.home.HomeFilterTab
import dev.frost819.newbv.app.ui.screen.pgc.PgcTabItem
import dev.frost819.newbv.app.ui.screen.ugc.UgcTabItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import org.junit.jupiter.api.Test

/**
 * [TopNav] 与 [TopNavItem] 的单元测试。
 *
 * 验证各功能模块的 Tab 项实现契约，确保显示名称、索引映射及全站统一模型一致性。
 */
class TopNavTest {
    private data class TestTopNavItem(
        override val displayName: String,
    ) : TopNavItem

    @Test
    fun `TopNavItem returns correct displayName for custom items`() {
        val item = TestTopNavItem("自定义菜单")
        assertThat(item.displayName).isEqualTo("自定义菜单")
    }

    @Test
    fun `HomeFilterTab implements TopNavItem correctly`() {
        val homeTabs: List<TopNavItem> = HomeFilterTab.entries.toList()
        assertThat(homeTabs.map { it.displayName }).containsExactly(
            "精选",
            "推荐",
            "热门",
            "动态",
        ).inOrder()
    }

    @Test
    fun `PersonalTabItem implements TopNavItem with expected labels`() {
        assertThat(PersonalTabItem(PersonalTopNavItem.ToView).displayName).isEqualTo("稍后再看")
        assertThat(PersonalTabItem(PersonalTopNavItem.History).displayName).isEqualTo("历史")
        assertThat(PersonalTabItem(PersonalTopNavItem.Favorite).displayName).isEqualTo("收藏")
        assertThat(PersonalTabItem(PersonalTopNavItem.FollowingSeason).displayName).isEqualTo("追番")
    }

    @Test
    fun `UgcTabItem implements TopNavItem with all partition labels`() {
        val ugcItems: List<TopNavItem> = UgcTabItem.entries.toList()
        assertThat(ugcItems).isNotEmpty()
        assertThat(ugcItems.first().displayName).isEqualTo("动画")
    }

    @Test
    fun `PgcTabItem implements TopNavItem with all anime and film labels`() {
        val pgcItems: List<TopNavItem> = PgcTabItem.entries.toList()
        assertThat(pgcItems.map { it.displayName }).containsExactly(
            "番剧",
            "国创",
            "电影",
            "电视剧",
            "纪录片",
            "综艺",
        ).inOrder()
    }

    @Test
    fun `PersonalTabItem equality and indexOf works as expected`() {
        val allItems = PersonalTopNavItem.entries.map { PersonalTabItem(it) }
        val target = PersonalTabItem(PersonalTopNavItem.Favorite)
        val index = allItems.indexOf(target)
        assertThat(index).isEqualTo(2)
    }
}
