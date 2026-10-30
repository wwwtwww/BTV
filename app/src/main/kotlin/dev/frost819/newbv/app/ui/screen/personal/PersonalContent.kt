package dev.frost819.newbv.app.ui.screen.personal

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.PersonalTabItem
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.navigation.VideoPlayerRoute
import dev.frost819.newbv.app.viewmodel.personal.PersonalUiEffect
import dev.frost819.newbv.app.viewmodel.personal.PersonalViewModel
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.Prefs
import androidx.compose.material3.Scaffold as Material3Scaffold

/**
 * 个人页内容（TopNav + 4 个子 Tab）。
 *
 * 顶部 Tab 按首选项排序，Tab 切换使用 [AnimatedContent] 横向滑动。
 * 稍后再看与收藏夹 Tab 顶栏右侧提供「全部播放」连播入口，菜单键刷新当前 Tab 数据。
 *
 * @param navFocusRequester 顶部 Tab 的焦点请求器（由 MainScreen 传入）。
 * @param navController 导航控制器（跳转详情页等）。
 * @param focusSaver 焦点恢复器（由 MainScreen 共享传入）。
 * @param viewModel 个人页 ViewModel。
 */
@Composable
fun PersonalContent(
    navFocusRequester: FocusRequester,
    navController: NavController,
    focusSaver: FocusSaver,
    viewModel: PersonalViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val firstTab = remember { Prefs.firstPersonalTopNavItem }
    var selectedTab by rememberSaveable { mutableStateOf(firstTab) }
    var focusOnContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PersonalUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val reorderedItems =
        remember {
            val allItems = PersonalTopNavItem.entries
            val startIndex = allItems.indexOf(firstTab)
            if (startIndex == -1) {
                allItems.map { PersonalTabItem(it) }
            } else {
                (allItems.drop(startIndex) + allItems.take(startIndex)).map { PersonalTabItem(it) }
            }
        }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Material3Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.focusRequester(navFocusRequester),
                items = reorderedItems,
                selectedIndex = reorderedItems.indexOf(PersonalTabItem(selectedTab)),
                isLargePadding = !focusOnContent,
                trailingContent = {
                    val showPlayAll =
                        when (selectedTab) {
                            PersonalTopNavItem.ToView -> viewModel.toViewItems.isNotEmpty()
                            PersonalTopNavItem.Favorite -> uiState.favoriteItems.isNotEmpty()
                            else -> false
                        }
                    if (showPlayAll) {
                        PersonalPlayAllButton(
                            onClick = {
                                when (selectedTab) {
                                    PersonalTopNavItem.ToView -> {
                                        viewModel.playAllToView { target ->
                                            navController.navigate(
                                                VideoPlayerRoute(
                                                    aid = target.avid,
                                                    cid = target.cid ?: 0L,
                                                    bvid = target.bvid,
                                                    title = target.title,
                                                    cover = target.cover,
                                                ),
                                            ) {
                                                popUpTo<VideoPlayerRoute> { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }

                                    PersonalTopNavItem.Favorite -> {
                                        viewModel.playAllFavorite { target ->
                                            navController.navigate(
                                                VideoPlayerRoute(
                                                    aid = target.avid,
                                                    cid = target.cid ?: 0L,
                                                    bvid = target.bvid,
                                                    title = target.title,
                                                    cover = target.cover,
                                                ),
                                            ) {
                                                popUpTo<VideoPlayerRoute> { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                            },
                        )
                    }
                },
                onSelectedChanged = { nav ->
                    selectedTab = (nav as PersonalTabItem).item
                },
                onClick = { nav ->
                    val tab = (nav as PersonalTabItem).item
                    viewModel.refresh(tab)
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .onFocusChanged { focusOnContent = it.hasFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
                            viewModel.refresh(selectedTab)
                            navFocusRequester.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                        false
                    },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "personal-animated-content",
                transitionSpec = {
                    val coefficient = 10
                    if (reorderedItems.indexOf(PersonalTabItem(targetState)) <
                        reorderedItems.indexOf(PersonalTabItem(initialState))
                    ) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                            fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                            fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                },
            ) { screen ->
                when (screen) {
                    PersonalTopNavItem.ToView ->
                        ToViewScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )
                    PersonalTopNavItem.History ->
                        HistoryScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )
                    PersonalTopNavItem.Favorite ->
                        FavoriteScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )
                    PersonalTopNavItem.FollowingSeason ->
                        FollowingSeasonScreen(
                            viewModel = viewModel,
                            navController = navController,
                            focusSaver = focusSaver,
                        )
                }
            }
        }
    }
}

/**
 * 个人页 TopNav 右侧「全部播放」胶囊按钮。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PersonalPlayAllButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = BVShape.pill),
        scale = TvFocusDefaults.surfaceScale(),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.playlist_play_24px),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "全部播放",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
