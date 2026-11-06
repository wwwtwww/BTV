package dev.frost819.newbv.app.ui.screen.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.component.TopNavItem
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.personal.PersonalViewModel
import dev.frost819.newbv.biliapi.entity.FavoriteFolderMetadata
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 收藏页面。
 *
 * 顶部收藏夹 Tab 栏 + 下方 4 列视频网格。
 * 切换收藏夹时自动加载该夹视频列表，无限滚动加载更多。
 *
 * @param viewModel 个人页 ViewModel。
 * @param navController 导航控制器。
 * @param focusSaver 焦点恢复器（由 MainScreen 共享传入）。
 */
@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonalViewModel,
    navController: NavController,
    focusSaver: FocusSaver,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    if (state.favoriteFolders.isEmpty() && !state.favoriteLoading && !state.favoriteError) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.tv.material3.Text(
                text = "没有收藏夹",
                color = androidx.tv.material3.MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.favoriteItems.size - 20
            }.collect {
                if (state.currentFolderId != -1L) {
                    viewModel.loadFavoriteItems(state.currentFolderId)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        val folderNavItems =
            remember(state.favoriteFolders) {
                state.favoriteFolders.map { FavoriteFolderNavItem(it) }
            }
        val selectedFolderIndex =
            remember(state.favoriteFolders, state.currentFolderId) {
                state.favoriteFolders.indexOfFirst { it.id == state.currentFolderId }.coerceAtLeast(0)
            }

        if (folderNavItems.isNotEmpty()) {
            TopNav(
                items = folderNavItems,
                selectedIndex = selectedFolderIndex,
                horizontalPadding = 24.dp,
                onSelectedChanged = { navItem ->
                    viewModel.loadFavoriteItems(navItem.folder.id)
                },
                onClick = { navItem ->
                    viewModel.loadFavoriteItems(navItem.folder.id, forceRefresh = true)
                },
            )
        }

        TvLazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = state.favoriteItems,
                key = { _, item -> item.id },
            ) { index, item ->
                val cardData =
                    remember(item) {
                        VideoCardData(
                            avid = item.id,
                            title = item.title,
                            cover = item.cover,
                            playString = "",
                            danmakuString = "",
                            timeString = (item.duration * 1000L).formatHourMinSec(),
                            upName = item.upper.name,
                            upMid = item.upper.mid,
                        )
                    }
                SmallVideoCard(
                    modifier = Modifier.focusSaverItem(focusSaver, "favorite_$index"),
                    data = cardData,
                    onClick = {
                        navController.navigateFromVideoCard(cardData)
                    },
                    onGoToDetailPage = {
                        navController.navigateFromVideoCard(cardData, forceDetail = true)
                    },
                    onGoToUpPage = {
                        navController.navigate(UserSpaceRoute(mid = item.upper.mid, name = item.upper.name))
                    },
                    onAddWatchLater = { watchLaterViewModel.addToView(aid = item.id) },
                    onRemoveFavorite = { viewModel.delFavoriteItem(item.id, state.currentFolderId) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ListFooterTip(
                    isLoading = state.favoriteLoading,
                    isError = state.favoriteError,
                    hasMore = state.favoriteHasMore,
                    itemsIsEmpty = state.favoriteItems.isEmpty(),
                )
            }
        }
    }
}

private data class FavoriteFolderNavItem(
    val folder: FavoriteFolderMetadata,
) : TopNavItem {
    override val displayName: String = "${folder.title} (${folder.mediaCount})"
}
