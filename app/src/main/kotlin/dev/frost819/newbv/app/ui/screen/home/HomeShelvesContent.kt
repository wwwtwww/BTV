package dev.frost819.newbv.app.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.TvLazyColumn
import dev.frost819.newbv.app.ui.component.home.EffectiveHeroMode
import dev.frost819.newbv.app.ui.component.home.HomeHeroBanner
import dev.frost819.newbv.app.ui.component.home.isDeviceLowSpecForHero
import dev.frost819.newbv.app.ui.component.home.resolveEffectiveHeroMode
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.LoginRoute
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.home.HomeViewModel
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.data.datastore.Prefs

/**
 * YouTube TV 风格主页综合横向泳道视图（Home Shelves Content）。
 *
 * 顶部常驻沉浸式 Hero Showcase 舞台 [HomeHeroBanner]（不抢占遥控器焦点），
 * 下方为垂直滚动的多行横向泳道组合（为你推荐、关注动态、今日热门），任一泳道卡片获焦停留满
 * 400ms 后驱动顶部舞台平滑切换。
 *
 * @param modifier 修饰符。
 * @param viewModel 首页 ViewModel。
 * @param navController 导航控制器。
 * @param focusSaver 焦点记忆器。
 * @param onSelectTab 切换至特定单分类 Tab 回调。
 */
@Composable
fun HomeShelvesContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    navController: NavController,
    focusSaver: FocusSaver,
    onSelectTab: (HomeFilterTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val heroModePref by Prefs.homeHeroModeFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isLowSpecDevice = remember(context) { isDeviceLowSpecForHero(context) }
    val effectiveHeroMode =
        remember(heroModePref, isLowSpecDevice) {
            resolveEffectiveHeroMode(mode = heroModePref, isLowSpecDevice = isLowSpecDevice)
        }

    val listState = rememberLazyListState()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    val recommendCards =
        remember(state.recommendItems) {
            state.recommendItems.map { VideoCardData.fromUgcItem(it) }
        }

    val dynamicCards =
        remember(state.dynamicItems) {
            state.dynamicItems.map { VideoCardData.fromDynamicVideo(it) }
        }

    val popularCards =
        remember(state.popularItems) {
            state.popularItems.map { VideoCardData.fromUgcItem(it) }
        }

    val onShelfCardFocused: (VideoCardData, String) -> Unit = { card, shelfTitle ->
        if (effectiveHeroMode != EffectiveHeroMode.Off) {
            viewModel.onHeroCandidateFocused(
                card = card,
                shelfTitle = shelfTitle,
                allowNetworkEnrich = effectiveHeroMode == EffectiveHeroMode.FullDynamic,
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HomeHeroBanner(
            showcase = state.heroShowcase,
            effectiveMode = effectiveHeroMode,
        )

        TvLazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding =
                PaddingValues(
                    top = if (effectiveHeroMode == EffectiveHeroMode.Off) 8.dp else 4.dp,
                    bottom = 48.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 泳道 1：为你推荐
            if (recommendCards.isNotEmpty()) {
                item(key = "shelf_recommend") {
                    HomeShelfRow(
                        shelfKey = "rcmd_shelf",
                        title = "为你推荐",
                        items = recommendCards,
                        onVideoClick = { card -> navController.navigateFromVideoCard(card) },
                        onGoToDetailPage = { card -> navController.navigateFromVideoCard(card, forceDetail = true) },
                        onGoToUpPage = { mid, name -> navController.navigate(UserSpaceRoute(mid = mid, name = name)) },
                        onAddWatchLater = { aid -> watchLaterViewModel.addToView(aid = aid) },
                        onCardFocused = { card -> onShelfCardFocused(card, "为你推荐") },
                        onSeeAllClick = { onSelectTab(HomeFilterTab.Recommend) },
                        onLoadMore = { viewModel.loadRecommend() },
                        hasMore = state.recommendHasMore,
                        focusSaver = focusSaver,
                    )
                }
            }

            // 泳道 2：关注动态
            if (state.isLogin) {
                if (dynamicCards.isNotEmpty()) {
                    item(key = "shelf_dynamics") {
                        HomeShelfRow(
                            shelfKey = "dyn_shelf",
                            title = "关注动态",
                            items = dynamicCards,
                            onVideoClick = { card -> navController.navigateFromVideoCard(card) },
                            onGoToDetailPage = { card ->
                                navController.navigateFromVideoCard(card, forceDetail = true)
                            },
                            onGoToUpPage = { mid, name ->
                                navController.navigate(UserSpaceRoute(mid = mid, name = name))
                            },
                            onAddWatchLater = { aid -> watchLaterViewModel.addToView(aid = aid) },
                            onCardFocused = { card -> onShelfCardFocused(card, "关注动态") },
                            onSeeAllClick = { onSelectTab(HomeFilterTab.Dynamics) },
                            onLoadMore = { viewModel.loadDynamic() },
                            hasMore = state.dynamicHasMore,
                            focusSaver = focusSaver,
                        )
                    }
                }
            } else {
                // 未登录引导卡片泳道
                item(key = "shelf_login_banner") {
                    DynamicsLoginBanner(
                        onLoginClick = { navController.navigate(LoginRoute) },
                    )
                }
            }

            // 泳道 3：今日热门
            if (popularCards.isNotEmpty()) {
                item(key = "shelf_popular") {
                    HomeShelfRow(
                        shelfKey = "pop_shelf",
                        title = "今日热门",
                        items = popularCards,
                        onVideoClick = { card -> navController.navigateFromVideoCard(card) },
                        onGoToDetailPage = { card -> navController.navigateFromVideoCard(card, forceDetail = true) },
                        onGoToUpPage = { mid, name -> navController.navigate(UserSpaceRoute(mid = mid, name = name)) },
                        onAddWatchLater = { aid -> watchLaterViewModel.addToView(aid = aid) },
                        onCardFocused = { card -> onShelfCardFocused(card, "今日热门") },
                        onSeeAllClick = { onSelectTab(HomeFilterTab.Popular) },
                        onLoadMore = { viewModel.loadPopular() },
                        hasMore = state.popularHasMore,
                        focusSaver = focusSaver,
                    )
                }
            }
        }
    }
}

/**
 * 关注动态未登录时的引导横幅。
 *
 * @param modifier 修饰符。
 * @param onLoginClick 点击登录按钮回调。
 */
@Composable
private fun DynamicsLoginBanner(
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit,
) {
    Surface(
        onClick = onLoginClick,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(88.dp),
        shape = ClickableSurfaceDefaults.shape(BVShape.large),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = Border.None,
                focusedBorder =
                    Border(
                        border =
                            BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        shape = BVShape.large,
                    ),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = "登录哔哩哔哩账户",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "解锁关注 UP 主动态更新与大屏专属推荐",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .padding(end = 8.dp),
            ) {
                Text(
                    text = "立即登录 >",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
