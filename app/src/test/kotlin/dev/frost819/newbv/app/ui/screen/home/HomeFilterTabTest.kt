package dev.frost819.newbv.app.ui.screen.home

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import org.junit.jupiter.api.Test

/**
 * [HomeFilterTab] 的单元测试。
 *
 * 验证从持久化配置 [HomeTopNavItem] 到 [HomeFilterTab] 的映射及 Tab 属性定义。
 */
class HomeFilterTabTest {
    @Test
    fun `fromHomeTopNavItem maps Recommend to Featured according to 10-foot design`() {
        val mapped = HomeFilterTab.fromHomeTopNavItem(HomeTopNavItem.Recommend)
        assertThat(mapped).isEqualTo(HomeFilterTab.Featured)
        assertThat(mapped.title).isEqualTo("精选")
    }

    @Test
    fun `fromHomeTopNavItem maps Popular to Popular`() {
        val mapped = HomeFilterTab.fromHomeTopNavItem(HomeTopNavItem.Popular)
        assertThat(mapped).isEqualTo(HomeFilterTab.Popular)
        assertThat(mapped.title).isEqualTo("热门")
    }

    @Test
    fun `fromHomeTopNavItem maps Dynamics to Dynamics`() {
        val mapped = HomeFilterTab.fromHomeTopNavItem(HomeTopNavItem.Dynamics)
        assertThat(mapped).isEqualTo(HomeFilterTab.Dynamics)
        assertThat(mapped.title).isEqualTo("动态")
    }

    @Test
    fun `all tabs define valid display titles`() {
        assertThat(HomeFilterTab.Featured.title).isEqualTo("精选")
        assertThat(HomeFilterTab.Recommend.title).isEqualTo("推荐")
        assertThat(HomeFilterTab.Popular.title).isEqualTo("热门")
        assertThat(HomeFilterTab.Dynamics.title).isEqualTo("动态")
    }
}
