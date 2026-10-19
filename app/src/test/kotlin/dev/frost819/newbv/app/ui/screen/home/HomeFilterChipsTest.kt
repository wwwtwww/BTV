package dev.frost819.newbv.app.ui.screen.home

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import org.junit.jupiter.api.Test

/**
 * [HomeFilterTab] 的单元测试。
 *
 * 验证从 [HomeTopNavItem] 到 [HomeFilterTab] 的映射逻辑及 Tab 属性。
 */
class HomeFilterChipsTest {
    @Test
    fun `fromHomeTopNavItem correctly maps each enum value`() {
        assertThat(HomeFilterTab.fromHomeTopNavItem(HomeTopNavItem.Recommend))
            .isEqualTo(HomeFilterTab.Featured)
        assertThat(HomeFilterTab.fromHomeTopNavItem(HomeTopNavItem.Popular))
            .isEqualTo(HomeFilterTab.Popular)
        assertThat(HomeFilterTab.fromHomeTopNavItem(HomeTopNavItem.Dynamics))
            .isEqualTo(HomeFilterTab.Dynamics)
    }

    @Test
    fun `fromHomeTopNavItem covers all HomeTopNavItem entries`() {
        // 保证当未来新增 HomeTopNavItem 时能够被测试捕获
        val mappedTabs = HomeTopNavItem.entries.map { HomeFilterTab.fromHomeTopNavItem(it) }
        assertThat(mappedTabs).hasSize(HomeTopNavItem.entries.size)
        assertThat(mappedTabs).containsExactly(
            HomeFilterTab.Featured,
            HomeFilterTab.Popular,
            HomeFilterTab.Dynamics,
        )
    }

    @Test
    fun `HomeFilterTab titles match design specifications`() {
        assertThat(HomeFilterTab.Featured.title).isEqualTo("精选")
        assertThat(HomeFilterTab.Recommend.title).isEqualTo("推荐")
        assertThat(HomeFilterTab.Popular.title).isEqualTo("热门")
        assertThat(HomeFilterTab.Dynamics.title).isEqualTo("动态")
    }
}
