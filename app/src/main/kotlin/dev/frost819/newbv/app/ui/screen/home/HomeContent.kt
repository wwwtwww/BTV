package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.home.HomeViewModel
import dev.frost819.newbv.core.focus.isMenuKey
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.Prefs

/**
 * 主页内容。
 *
 * 采用全站统一的极简文字与黑白滑动指示条顶部导航栏 [HomeFilterChips]，
 * 内容区默认呈现顶部沉浸式 Hero Showcase 舞台与多横向泳道视图 [HomeShelvesContent]，
 * 或无缝切换至单分类沉浸瀑布流。
 *
 * 支持通过遥控器「菜单键 (Menu)」（焦点位于顶部导航行时）或重复点击分类 Tab 刷新当前数据；
 * 焦点位于内容区卡片上时菜单键放行给卡片，用于唤起快捷操作弹窗。
 *
 * @param navFocusRequester 顶部导航焦点请求器。
 * @param navController 导航控制器。
 * @param focusSaver 焦点恢复器。
 * @param viewModel 首页 ViewModel。
 */
@Composable
fun HomeContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    focusSaver: FocusSaver,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val initialTab =
        remember {
            HomeFilterTab.fromHomeTopNavItem(Prefs.firstHomeTopNavItem)
        }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    // 菜单键刷新仅在焦点位于导航行时生效，避免与内容区卡片的菜单键弹窗冲突
    var chipsHasFocus by remember { mutableStateOf(false) }

    fun refreshCurrentTab(tab: HomeFilterTab) {
        when (tab) {
            HomeFilterTab.Featured -> {
                viewModel.refresh(HomeTopNavItem.Recommend)
                viewModel.refresh(HomeTopNavItem.Popular)
                if (uiState.isLogin) viewModel.refresh(HomeTopNavItem.Dynamics)
            }
            HomeFilterTab.Recommend -> viewModel.refresh(HomeTopNavItem.Recommend)
            HomeFilterTab.Popular -> viewModel.refresh(HomeTopNavItem.Popular)
            HomeFilterTab.Dynamics -> viewModel.refresh(HomeTopNavItem.Dynamics)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { event ->
                    if (event.isMenuKey() && event.type == KeyEventType.KeyUp && chipsHasFocus) {
                        refreshCurrentTab(selectedTab)
                        navFocusRequester.safeRequestFocus()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
    ) {
        // 全站统一极简文字 + 黑白滑动指示条顶部导航栏
        Box(modifier = Modifier.onFocusChanged { chipsHasFocus = it.hasFocus }) {
            HomeFilterChips(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == HomeFilterTab.Dynamics && uiState.dynamicItems.isEmpty()) {
                        viewModel.loadDynamic()
                    }
                },
                onTabReselected = { tab ->
                    refreshCurrentTab(tab)
                },
                focusRequester = navFocusRequester,
            )
        }

        // 内容区：综合横向泳道 / 单分类沉浸网格
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedTab,
                label = "home-shelves-content",
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
            ) { tab ->
                when (tab) {
                    HomeFilterTab.Featured ->
                        HomeShelvesContent(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                            onSelectTab = { selectedTab = it },
                        )

                    HomeFilterTab.Recommend ->
                        RecommendScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )

                    HomeFilterTab.Popular ->
                        PopularScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )

                    HomeFilterTab.Dynamics ->
                        DynamicsScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )
                }
            }
        }
    }
}
