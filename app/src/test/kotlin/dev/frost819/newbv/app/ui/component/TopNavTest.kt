package dev.frost819.newbv.app.ui.component

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.ui.screen.home.HomeFilterTab
import dev.frost819.newbv.app.ui.screen.pgc.PgcTabItem
import dev.frost819.newbv.app.ui.screen.ugc.UgcTabItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import org.junit.jupiter.api.Test

/**
 * [TopNav] 与各功能模块 Tab 项的模型契约测试。
 *
 * 验证全站各功能模块的 Tab 项实现契约，确保所有 Tab 数据源均提供合法的非空标题，
 * 避免维护脆弱的字面量断言与脱离组件的冗余标准库测试。
 */
class TopNavTest {
    @Test
    fun `all top navigation tab models define non-empty display names`() {
        val allTabs: List<TopNavItem> =
            buildList {
                addAll(HomeFilterTab.entries)
                addAll(PersonalTopNavItem.entries.map { PersonalTabItem(it) })
                addAll(UgcTabItem.entries)
                addAll(PgcTabItem.entries)
            }

        assertThat(allTabs).isNotEmpty()
        allTabs.forEach { tab ->
            assertThat(tab.displayName).isNotNull()
            assertThat(tab.displayName.trim()).isNotEmpty()
        }
    }
}
