package dev.frost819.newbv.app.ui.screen.search

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.frost819.newbv.app.ui.component.rememberFocusSaver
import dev.frost819.newbv.app.ui.navigation.SearchResultRoute

/**
 * 搜索结果页导航注册。
 *
 * [SearchResultRoute] 对应同屏双栏即时搜索 [SearchInputContent]，keyword 通过路由参数传递。
 */
fun NavGraphBuilder.searchResultScreen(navController: NavController) {
    composable<SearchResultRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<SearchResultRoute>()
        val focusRequester = remember { FocusRequester() }
        val focusSaver = rememberFocusSaver()
        focusSaver.RestoreFocus()
        SearchInputContent(
            focusRequester = focusRequester,
            focusSaver = focusSaver,
            navController = navController,
            initialKeyword = route.keyword,
            isStandaloneRoute = true,
        )
    }
}
