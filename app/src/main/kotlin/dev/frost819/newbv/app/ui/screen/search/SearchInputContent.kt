package dev.frost819.newbv.app.ui.screen.search

import dev.frost819.newbv.core.theme.BVShape

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCardData
import dev.frost819.newbv.app.ui.component.search.SearchKeyword
import dev.frost819.newbv.app.ui.component.search.SearchResultFilter
import dev.frost819.newbv.app.ui.component.search.SoftKeyboard
import dev.frost819.newbv.app.ui.component.search.UpCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCardData
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.ui.state.search.SearchResultItem
import dev.frost819.newbv.app.ui.state.search.TypedSearchResult
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.removeHtmlTags
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchInputViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchResultViewModel
import dev.frost819.newbv.biliapi.repositories.SearchType
import dev.frost819.newbv.core.focus.isKeyDown
import dev.frost819.newbv.core.focus.isMenuKey
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val searchTypeLabels =
    mapOf(
        SearchType.Video to "视频",
        SearchType.MediaBangumi to "番剧",
        SearchType.MediaFt to "影视",
        SearchType.BiliUser to "用户",
        SearchType.LiveRoom to "直播",
    )

private val searchTypeColumns =
    mapOf(
        SearchType.Video to 3,
        SearchType.MediaBangumi to 5,
        SearchType.MediaFt to 5,
        SearchType.BiliUser to 4,
        SearchType.LiveRoom to 3,
    )

/**
 * YouTube TV 风格同屏双栏搜索（Split-Screen Search）。
 *
 * 左栏：软键盘 + 搜索建议/历史/热搜。
 * 右栏：同屏呈现搜索结果网格，即点即播，无需跨页面跳转。
 *
 * @param modifier 修饰符。
 * @param inputViewModel 搜索输入 ViewModel。
 * @param resultViewModel 搜索结果 ViewModel。
 * @param focusRequester 初始键盘焦点请求器。
 * @param focusSaver 焦点记忆恢复器。
 * @param navController 导航控制器。
 * @param initialKeyword 初始搜索关键词（可选，用于外部跳转）。
 * @param onSearch 提交搜索额外回调（兼容旧接口）。
 * @param isStandaloneRoute 是否作为独立路由页使用（如 [dev.frost819.newbv.app.ui.navigation.SearchResultRoute]）。
 * 为 true 时返回键不做「清空关键字」拦截，保证首次返回即退出当前页。
 */
@Composable
fun SearchInputContent(
    modifier: Modifier = Modifier,
    inputViewModel: SearchInputViewModel = hiltViewModel(),
    resultViewModel: SearchResultViewModel = hiltViewModel(),
    focusRequester: FocusRequester,
    focusSaver: FocusSaver,
    navController: NavController,
    initialKeyword: String = "",
    onSearch: ((String) -> Unit)? = null,
    isStandaloneRoute: Boolean = false,
) {
    val inputState by inputViewModel.uiState.collectAsStateWithLifecycle()
    val resultState by resultViewModel.uiState.collectAsStateWithLifecycle()
    val watchLaterViewModel: WatchLaterViewModel = hiltViewModel()

    CollectWatchLaterEffects(watchLaterViewModel)

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var focusInRightPane by remember { mutableStateOf(false) }

    fun executeSearch(keyword: String) {
        if (keyword.isBlank()) return
        inputViewModel.commitSearch(keyword) {
            resultViewModel.search(keyword)
            scope.launch { gridState.scrollToItem(0) }
            onSearch?.invoke(keyword)
        }
    }

    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotBlank()) {
            inputViewModel.updateKeyword(initialKeyword)
            executeSearch(initialKeyword)
        }
    }

    LaunchedEffect(resultState.activeType) {
        gridState.scrollToItem(0)
    }

    // 分页加载监听
    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastIndex =
                gridState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val current = resultState
            val activeType = current.activeType
            val count = current.results[activeType]?.count ?: 0
            Triple(lastIndex, count, activeType)
        }.distinctUntilChanged()
            .filter { (lastIndex, count, _) ->
                count > 0 && lastIndex in 0 until count && lastIndex >= count - 10
            }.collect { (_, _, activeType) ->
                resultViewModel.loadMore(activeType)
            }
    }

    // 遥控器返回键分层处理：若在右侧结果区，返回左侧输入区；
    // 主界面搜索 Tab 内左侧输入有字时清空；独立路由页不做清空拦截，首次返回即退出
    BackHandler(
        enabled = focusInRightPane || (!isStandaloneRoute && inputState.keyword.isNotEmpty()),
    ) {
        if (focusInRightPane) {
            runCatching { focusRequester.requestFocus() }
        } else if (inputState.keyword.isNotEmpty()) {
            inputViewModel.updateKeyword("")
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ================= 左栏：软键盘与辅助词条 =================
        Column(
            modifier =
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 搜索输入框
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = inputState.keyword,
                onValueChange = { inputViewModel.updateKeyword(it) },
                maxLines = 1,
                shape = BVShape.medium,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.border,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                placeholder = {
                    Text(
                        text = "搜索视频、番剧、UP主...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            // 软键盘
            SoftKeyboard(
                firstButtonFocusRequester = focusRequester,
                searchButtonModifier = Modifier.focusSaverItem(focusSaver, "search_btn"),
                onClick = { inputViewModel.updateKeyword(inputState.keyword + it) },
                onClear = { inputViewModel.updateKeyword("") },
                onDelete = {
                    if (inputState.keyword.isNotEmpty()) {
                        inputViewModel.updateKeyword(inputState.keyword.dropLast(1))
                    }
                },
                onSearch = { executeSearch(inputState.keyword) },
            )

            // 下部：候选词流（建议或历史/热搜）
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                if (inputState.keyword.isNotEmpty()) {
                    SearchSuggestsSection(
                        suggests = inputState.suggests,
                        onSelect = { executeSearch(it) },
                        focusSaver = focusSaver,
                    )
                } else {
                    SearchHistoryAndHotwordsSection(
                        histories = inputState.histories,
                        hotwords = inputState.hotwords,
                        onSelect = { executeSearch(it) },
                        onDeleteHistory = { inputViewModel.deleteHistory(it) },
                        onClearAllHistories = { inputViewModel.clearAllHistories() },
                        focusSaver = focusSaver,
                    )
                }
            }
        }

        // ================= 右栏：同屏结果区 =================
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusProperties { left = focusRequester }
                    .onFocusChanged { focusInRightPane = it.hasFocus }
                    .onPreviewKeyEvent { event ->
                        if (resultState.activeType == SearchType.Video &&
                            Prefs.apiType == ApiType.Web &&
                            event.isMenuKey() &&
                            event.isKeyDown()
                        ) {
                            resultViewModel.toggleFilter(true)
                            true
                        } else {
                            false
                        }
                    },
        ) {
            val activeResult =
                resultState.results[resultState.activeType]
                    ?: TypedSearchResult(resultState.activeType)
            val columnCount = searchTypeColumns[resultState.activeType] ?: 3

            // 顶部：分类 Filter Chips 与 筛选按钮
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchType.entries.forEach { type ->
                        val isSelected = type == resultState.activeType
                        Surface(
                            onClick = {
                                // 滚动重置由 LaunchedEffect(activeType) 统一处理
                                resultViewModel.switchType(type)
                            },
                            modifier = Modifier.height(32.dp),
                            shape = ClickableSurfaceDefaults.shape(BVShape.pill),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                            colors =
                                ClickableSurfaceDefaults.colors(
                                    containerColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.inverseSurface
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        },
                                    contentColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.inverseOnSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    focusedContainerColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.inverseSurface
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    focusedContentColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.inverseOnSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                ),
                            border =
                                ClickableSurfaceDefaults.border(
                                    border = Border.None,
                                    focusedBorder =
                                        Border(
                                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                            shape = BVShape.pill,
                                        ),
                                ),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = searchTypeLabels[type] ?: type.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // 筛选按钮（仅视频 Web API 支持）
                if (resultState.activeType == SearchType.Video && Prefs.apiType == ApiType.Web) {
                    IconButton(
                        onClick = { resultViewModel.toggleFilter(true) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "筛选",
                        )
                    }
                }
            }

            // 结果网格区
            if (activeResult.items.isEmpty() && !activeResult.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (resultState.keyword.isBlank()) "在左侧键入关键词，按「搜索」键查看结果" else "暂无搜索结果",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            } else {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = activeResult.items,
                        key = { item ->
                            when (item) {
                                is SearchResultItem.VideoItem -> "video_${item.video.aid}"
                                is SearchResultItem.PgcItem -> "pgc_${item.pgc.seasonId}"
                                is SearchResultItem.UserItem -> "user_${item.user.mid}"
                                is SearchResultItem.LiveRoomItem -> "live_${item.room.roomId}"
                            }
                        },
                    ) { item ->
                        when (item) {
                            is SearchResultItem.VideoItem -> {
                                val v = item.video
                                val cardData =
                                    VideoCardData(
                                        avid = v.aid,
                                        title = v.title.removeHtmlTags(),
                                        cover = v.cover,
                                        playString = v.play.toWanString(),
                                        danmakuString = v.danmaku.toWanString(),
                                        timeString = v.duration.formatHourMinSec(),
                                        upName = v.author,
                                        upMid = v.mid,
                                        pubTime = v.pubTime,
                                    )
                                SmallVideoCard(
                                    modifier = Modifier.focusSaverItem(focusSaver, "video_${v.aid}"),
                                    data = cardData,
                                    onClick = { navController.navigateFromVideoCard(cardData) },
                                    onGoToDetailPage = {
                                        navController.navigateFromVideoCard(
                                            cardData,
                                            forceDetail = true,
                                        )
                                    },
                                    onGoToUpPage = {
                                        navController.navigate(
                                            UserSpaceRoute(mid = v.mid, name = v.author),
                                        )
                                    },
                                    onAddWatchLater = { watchLaterViewModel.addToView(aid = v.aid) },
                                )
                            }
                            is SearchResultItem.PgcItem -> {
                                val p = item.pgc
                                SeasonCard(
                                    modifier = Modifier.focusSaverItem(focusSaver, "pgc_${p.seasonId}"),
                                    data =
                                        SeasonCardData(
                                            seasonId = p.seasonId,
                                            title = p.title.removeHtmlTags(),
                                            cover = p.cover,
                                            rating = if (p.star > 0) String.format("%.1f", p.star) else null,
                                        ),
                                    onClick = {
                                        navController.navigate(PgcFeatureRoute(seasonId = p.seasonId.toLong()))
                                    },
                                )
                            }
                            is SearchResultItem.UserItem -> {
                                val u = item.user
                                UpCard(
                                    modifier = Modifier.focusSaverItem(focusSaver, "user_${u.mid}"),
                                    avatar = u.avatar,
                                    username = u.name,
                                    sign = u.sign,
                                    onClick = {
                                        navController.navigate(
                                            UserSpaceRoute(mid = u.mid, name = u.name, face = u.avatar),
                                        )
                                    },
                                )
                            }
                            is SearchResultItem.LiveRoomItem -> {
                                val room = item.room
                                LiveRoomCard(
                                    modifier = Modifier.focusSaverItem(focusSaver, "live_${room.roomId}"),
                                    data =
                                        LiveRoomCardData(
                                            roomId = room.roomId,
                                            title = room.title.removeHtmlTags(),
                                            uname = room.uname.removeHtmlTags(),
                                            uid = room.uid,
                                            cover = room.cover,
                                            face = room.face,
                                            areaV2Name = room.areaName.removeHtmlTags(),
                                            areaV2ParentName = "",
                                            onlineString = room.online.toWanString(),
                                            watchedString = "",
                                        ),
                                    onClick = {
                                        navController.navigate(LivePlayerRoute(roomId = room.roomId))
                                    },
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(columnCount) }) {
                        ListFooterTip(
                            isLoading = activeResult.isLoading,
                            isError = activeResult.error,
                            hasMore = activeResult.hasMore,
                            itemsIsEmpty = activeResult.items.isEmpty(),
                        )
                    }
                }
            }

            // 筛选弹窗
            if (resultState.showFilter) {
                SearchResultFilter(
                    selectedOrder = resultState.selectedOrder,
                    selectedDuration = resultState.selectedDuration,
                    onConfirm = { order, duration ->
                        resultViewModel.updateFilter(order, duration)
                    },
                    onDismiss = { resultViewModel.toggleFilter(false) },
                )
            }
        }
    }
}

/**
 * 实时关联搜索建议小部件。
 */
@Composable
private fun SearchSuggestsSection(
    suggests: List<String>,
    onSelect: (String) -> Unit,
    focusSaver: FocusSaver,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "搜索建议",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(suggests) { index, suggest ->
                SearchKeyword(
                    modifier = Modifier.focusSaverItem(focusSaver, "suggest_$index"),
                    keyword = suggest,
                    onClick = { onSelect(suggest) },
                )
            }
        }
    }
}

/**
 * 搜索历史与热门推荐小部件。
 */
@Composable
private fun SearchHistoryAndHotwordsSection(
    histories: List<dev.frost819.newbv.data.db.entity.SearchHistoryEntity>,
    hotwords: List<dev.frost819.newbv.biliapi.entity.search.Hotword>,
    onSelect: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearAllHistories: () -> Unit,
    focusSaver: FocusSaver,
) {
    var deleteMode by remember { mutableStateOf(false) }
    var showHotword by remember { mutableStateOf(Prefs.showHotword) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 历史记录
        if (histories.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "搜索历史",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        if (deleteMode) {
                            IconButton(
                                modifier = Modifier.size(28.dp),
                                onClick = onClearAllHistories,
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "清空全部",
                                )
                            }
                        }
                        IconButton(
                            modifier = Modifier.size(28.dp),
                            onClick = { deleteMode = !deleteMode },
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = if (deleteMode) Icons.Default.Close else Icons.Default.Delete,
                                contentDescription = if (deleteMode) "完成" else "管理历史",
                            )
                        }
                    }
                }
            }

            itemsIndexed(histories) { index, history ->
                SearchKeyword(
                    modifier = Modifier.focusSaverItem(focusSaver, "hist_$index"),
                    keyword = history.keyword,
                    onClick = {
                        if (deleteMode) {
                            onDeleteHistory(history.keyword)
                        } else {
                            onSelect(history.keyword)
                        }
                    },
                    trailingIcon =
                        if (deleteMode) {
                            {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }

        // 热门搜索（尊重「显示搜索热词」设置，支持展开/收起并持久化）
        if (hotwords.isNotEmpty()) {
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "热搜榜单",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        modifier = Modifier.size(28.dp),
                        onClick = {
                            val next = !showHotword
                            showHotword = next
                            Prefs.showHotword = next
                        },
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector =
                                if (showHotword) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                            contentDescription = if (showHotword) "收起热搜" else "展开热搜",
                        )
                    }
                }
            }

            if (showHotword) {
                itemsIndexed(hotwords) { index, hotword ->
                    SearchKeyword(
                        modifier = Modifier.focusSaverItem(focusSaver, "hot_$index"),
                        keyword = hotword.showName,
                        onClick = { onSelect(hotword.showName) },
                    )
                }
            }
        }
    }
}
