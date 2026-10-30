package dev.frost819.newbv.app.ui.screen.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import dev.frost819.newbv.app.ui.component.ListFooterTip
import dev.frost819.newbv.app.ui.component.TopNav
import dev.frost819.newbv.app.ui.component.TopNavItem
import dev.frost819.newbv.app.ui.component.TvLazyVerticalGrid
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCard
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCardData
import dev.frost819.newbv.app.ui.component.search.SearchHistoryChip
import dev.frost819.newbv.app.ui.component.search.SearchHotwordItem
import dev.frost819.newbv.app.ui.component.search.SearchKeyword
import dev.frost819.newbv.app.ui.component.search.SearchResultFilter
import dev.frost819.newbv.app.ui.component.search.SoftKeyboard
import dev.frost819.newbv.app.ui.component.search.UpCard
import dev.frost819.newbv.app.ui.component.search.WideSearchSuggestItem
import dev.frost819.newbv.app.ui.component.search.displayLabel
import dev.frost819.newbv.app.ui.component.videocard.SeasonCard
import dev.frost819.newbv.app.ui.component.videocard.SeasonCardData
import dev.frost819.newbv.app.ui.component.videocard.SmallVideoCard
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.navigation.LivePlayerRoute
import dev.frost819.newbv.app.ui.navigation.PgcFeatureRoute
import dev.frost819.newbv.app.ui.navigation.UserSpaceRoute
import dev.frost819.newbv.app.ui.navigation.navigateFromVideoCard
import dev.frost819.newbv.app.ui.state.search.SearchPaneStage
import dev.frost819.newbv.app.ui.state.search.SearchResultItem
import dev.frost819.newbv.app.ui.state.search.SearchResultUiState
import dev.frost819.newbv.app.ui.state.search.TypedSearchResult
import dev.frost819.newbv.app.util.formatHourMinSec
import dev.frost819.newbv.app.util.removeHtmlTags
import dev.frost819.newbv.app.util.toWanString
import dev.frost819.newbv.app.viewmodel.common.CollectWatchLaterEffects
import dev.frost819.newbv.app.viewmodel.common.WatchLaterViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchInputViewModel
import dev.frost819.newbv.app.viewmodel.search.SearchResultViewModel
import dev.frost819.newbv.biliapi.entity.search.Hotword
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.biliapi.repositories.SearchType
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.isKeyUp
import dev.frost819.newbv.core.focus.isMenuKey
import dev.frost819.newbv.core.focus.safeRequestFocus
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.LocalIsDarkTheme
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private data class SearchTypeNavItem(
    val type: SearchType,
    override val displayName: String,
) : TopNavItem

private val searchNavItems =
    listOf(
        SearchTypeNavItem(SearchType.Video, "视频"),
        SearchTypeNavItem(SearchType.MediaBangumi, "番剧"),
        SearchTypeNavItem(SearchType.MediaFt, "影视"),
        SearchTypeNavItem(SearchType.BiliUser, "用户"),
        SearchTypeNavItem(SearchType.LiveRoom, "直播"),
    )

private val searchTypeColumns =
    mapOf(
        SearchType.Video to 3,
        SearchType.MediaBangumi to 4,
        SearchType.MediaFt to 4,
        SearchType.BiliUser to 3,
        SearchType.LiveRoom to 3,
    )

/**
 * YouTube TV 风格同屏双栏搜索（双区接力协同架构）。
 *
 * 采用「窄左栏输入（228dp）+ 宽右栏画布（自适应约 620dp）」黄金比例与三阶段接力状态机：
 * 1. [SearchPaneStage.Discovery]（未输入）：左栏下部展示流式搜索历史胶囊，右栏展示双列宽幅热搜榜；
 * 2. [SearchPaneStage.Suggests]（输入中）：左栏下部保留搜索历史，右栏切换为双列宽敞搜索建议面板；
 * 3. [SearchPaneStage.Results]（已提交）：右栏呈现全局统一 [TopNav] 分类栏与 3/4 列宽幅结果网格，
 *    同时关联搜索建议自动退守接力至左栏下部，便于浏览结果时随时切换长尾兄弟词条。
 *
 * @param modifier 修饰符。
 * @param inputViewModel 搜索输入 ViewModel。
 * @param resultViewModel 搜索结果 ViewModel。
 * @param focusRequester 初始软键盘首键焦点请求器。
 * @param focusSaver 跨页面焦点记忆恢复器。
 * @param navController 导航控制器。
 * @param initialKeyword 初始搜索关键词（可选，用于外部路由跳转）。
 * @param onSearch 提交搜索额外回调。
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
    var lastLeftFocusRequester by remember { mutableStateOf(focusRequester) }

    fun executeSearch(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        inputViewModel.commitSearch(trimmed) {
            resultViewModel.search(trimmed)
            scope.launch { gridState.scrollToItem(0) }
            onSearch?.invoke(trimmed)
        }
    }

    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotBlank() && resultState.keyword != initialKeyword) {
            executeSearch(initialKeyword)
        }
    }

    var lastScrolledType by rememberSaveable { mutableStateOf(resultState.activeType) }
    LaunchedEffect(resultState.activeType) {
        if (lastScrolledType != resultState.activeType) {
            lastScrolledType = resultState.activeType
            gridState.scrollToItem(0)
        }
    }

    // 滚动触底分页加载监听
    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastIndex =
                gridState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val activeType = resultState.activeType
            val count = resultState.results[activeType]?.count ?: 0
            Triple(lastIndex, count, activeType)
        }.distinctUntilChanged()
            .filter { (lastIndex, count, _) ->
                count > 0 && lastIndex in 0 until count && lastIndex >= count - 9
            }.collect { (_, _, activeType) ->
                resultViewModel.loadMore(activeType)
            }
    }

    // 遥控器返回键分层处理：在右栏时精确退回左栏上次聚焦按键；在左栏主界面且有输入时清空回探索态
    BackHandler(
        enabled = focusInRightPane || (!isStandaloneRoute && inputState.keyword.isNotEmpty()),
    ) {
        if (focusInRightPane) {
            if (!lastLeftFocusRequester.safeRequestFocus()) {
                focusRequester.safeRequestFocus()
            }
        } else if (inputState.keyword.isNotEmpty()) {
            inputViewModel.updateKeyword("")
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ================= 左栏（228dp）：紧凑输入框 + 扁平键盘 + 历史/关联建议接力区 =================
        Column(
            modifier =
                Modifier
                    .width(228.dp)
                    .fillMaxHeight()
                    .focusRestorer(focusRequester)
                    .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CompactSearchInputBox(
                keyword = inputState.keyword,
                onKeywordChange = { inputViewModel.updateKeyword(it) },
                onSearch = { executeSearch(inputState.keyword) },
                onFocusedRequester = { lastLeftFocusRequester = it },
            )

            SoftKeyboard(
                firstButtonFocusRequester = focusRequester,
                searchButtonModifier = Modifier.focusSaverItem(focusSaver, "search_btn"),
                onFocusedRequesterChanged = { lastLeftFocusRequester = it },
                onClick = { inputViewModel.updateKeyword(inputState.keyword + it) },
                onClear = { inputViewModel.updateKeyword("") },
                onDelete = {
                    if (inputState.keyword.isNotEmpty()) {
                        inputViewModel.updateKeyword(inputState.keyword.dropLast(1))
                    }
                },
                onSearch = { executeSearch(inputState.keyword) },
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                if (inputState.stage == SearchPaneStage.Results && inputState.relaySuggests.isNotEmpty()) {
                    RelaySuggestsSection(
                        suggests = inputState.relaySuggests,
                        activeKeyword = resultState.keyword,
                        onSelect = { executeSearch(it) },
                        onFocusedRequester = { lastLeftFocusRequester = it },
                        focusSaver = focusSaver,
                    )
                } else {
                    SearchHistoryChipsSection(
                        histories = inputState.histories,
                        onSelect = { executeSearch(it) },
                        onDeleteHistory = { inputViewModel.deleteHistory(it) },
                        onClearAllHistories = { inputViewModel.clearAllHistories() },
                        onFocusedRequester = { lastLeftFocusRequester = it },
                        focusSaver = focusSaver,
                    )
                }
            }
        }

        // ================= 右栏（自适应宽幅画布）：热搜榜 / 宽幅建议 / TopNav + 3列结果网格 =================
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { focusInRightPane = it.hasFocus },
        ) {
            when (inputState.stage) {
                SearchPaneStage.Discovery -> {
                    HotwordsCanvasSection(
                        hotwords = inputState.hotwords,
                        isLoading = inputState.isLoadingHotwords,
                        hasError = inputState.hotwordsError,
                        onRetry = { inputViewModel.refreshHotwords() },
                        onSelect = { executeSearch(it) },
                        focusSaver = focusSaver,
                    )
                }
                SearchPaneStage.Suggests -> {
                    WideSuggestsCanvasSection(
                        keyword = inputState.keyword,
                        suggests = inputState.suggests,
                        onSelect = { executeSearch(it) },
                        focusSaver = focusSaver,
                    )
                }
                SearchPaneStage.Results -> {
                    ResultsCanvasSection(
                        resultState = resultState,
                        gridState = gridState,
                        focusSaver = focusSaver,
                        navController = navController,
                        resultViewModel = resultViewModel,
                        watchLaterViewModel = watchLaterViewModel,
                    )
                }
            }
        }
    }
}

/**
 * 紧凑型单行搜索输入框（38dp 高度，支持软键盘与外接实体键盘回车搜索）。
 */
@Composable
private fun CompactSearchInputBox(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusedRequester: (FocusRequester) -> Unit,
) {
    val requester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    val containerColor =
        if (isFocused) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.5f else 0.75f)
        }
    val borderColor =
        if (isFocused) {
            MaterialTheme.colorScheme.border
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.1f else 0.08f)
        }

    BasicTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .focusRequester(requester)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) {
                        onFocusedRequester(requester)
                    }
                }.onPreviewKeyEvent { event ->
                    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (isEnter && event.isKeyUp()) {
                        onSearch()
                        true
                    } else {
                        false
                    }
                },
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(BVShape.medium)
                        .background(containerColor)
                        .border(
                            width = if (isFocused) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = BVShape.medium,
                        ).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (keyword.isEmpty()) {
                        Text(
                            text = "搜索视频、番剧、UP主...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

/**
 * 左栏底部流式搜索历史胶囊词云（探索态与输入建议态共用）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistoryChipsSection(
    histories: List<SearchHistoryEntity>,
    onSelect: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearAllHistories: () -> Unit,
    onFocusedRequester: (FocusRequester) -> Unit,
    focusSaver: FocusSaver,
) {
    var deleteMode by remember { mutableStateOf(false) }
    val clearBtnRequester = remember { FocusRequester() }
    val manageBtnRequester = remember { FocusRequester() }

    LaunchedEffect(histories.isEmpty()) {
        if (histories.isEmpty() && deleteMode) {
            deleteMode = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (deleteMode) "点击词条删除" else "搜索历史",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (histories.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (deleteMode) {
                        SmallHeaderIconAction(
                            modifier =
                                Modifier
                                    .focusRequester(clearBtnRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) onFocusedRequester(clearBtnRequester)
                                    },
                            icon = Icons.Default.DeleteSweep,
                            contentDescription = "清空全部",
                            onClick = onClearAllHistories,
                        )
                    }
                    SmallHeaderIconAction(
                        modifier =
                            Modifier
                                .focusRequester(manageBtnRequester)
                                .onFocusChanged {
                                    if (it.isFocused) onFocusedRequester(manageBtnRequester)
                                },
                        icon = if (deleteMode) Icons.Default.Close else Icons.Default.Delete,
                        contentDescription = if (deleteMode) "完成" else "管理历史",
                        onClick = { deleteMode = !deleteMode },
                    )
                }
            }
        }

        if (histories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无搜索历史",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    histories.forEachIndexed { index, history ->
                        val chipKey = "hist_${history.keyword}_$index"
                        val requester = focusSaver.focusRequesterFor(chipKey)
                        SearchHistoryChip(
                            modifier =
                                Modifier
                                    .focusSaverItem(focusSaver, chipKey)
                                    .onFocusChanged {
                                        if (it.isFocused) onFocusedRequester(requester)
                                    },
                            keyword = history.keyword,
                            deleteMode = deleteMode,
                            onClick = {
                                if (deleteMode) {
                                    onDeleteHistory(history.keyword)
                                } else {
                                    onSelect(history.keyword)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 左栏底部关联搜索建议接力列表（结果浏览态下承接关联词条，消除左栏留白并支持快速切换兄弟词条）。
 */
@Composable
private fun RelaySuggestsSection(
    suggests: List<String>,
    activeKeyword: String,
    onSelect: (String) -> Unit,
    onFocusedRequester: (FocusRequester) -> Unit,
    focusSaver: FocusSaver,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "关联建议",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(
                items = suggests,
                key = { index, item -> "relay_${index}_$item" },
            ) { index, suggest ->
                val saverKey = "relay_$index"
                val requester = focusSaver.focusRequesterFor(saverKey)
                SearchKeyword(
                    modifier =
                        Modifier
                            .focusSaverItem(focusSaver, saverKey)
                            .onFocusChanged {
                                if (it.isFocused) onFocusedRequester(requester)
                            },
                    keyword = suggest,
                    selected = suggest == activeKeyword,
                    onClick = { onSelect(suggest) },
                )
            }
        }
    }
}

/**
 * 右栏阶段一：双列宽幅全站热搜榜单。
 */
@Composable
private fun HotwordsCanvasSection(
    hotwords: List<Hotword>,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    focusSaver: FocusSaver,
) {
    var showHotword by remember { mutableStateOf(Prefs.showHotword) }
    val primaryRequester = remember { FocusRequester() }
    val toggleRequester = remember { FocusRequester() }
    val canFocusList = showHotword && hotwords.isNotEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRestorer(if (canFocusList) primaryRequester else toggleRequester)
                .focusGroup(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "全站热搜",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HeaderActionPill(
                modifier = Modifier.focusRequester(toggleRequester),
                label = if (showHotword) "收起热搜" else "展开热搜",
                icon = if (showHotword) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                onClick = {
                    val next = !showHotword
                    showHotword = next
                    Prefs.showHotword = next
                },
            )
        }

        when {
            !showHotword -> {
                CanvasMessageBox(text = "热搜榜单已收起，可在左侧键入关键词开始搜索")
            }
            isLoading && hotwords.isEmpty() -> {
                CanvasMessageBox(text = "正在加载全站热搜...")
            }
            hasError && hotwords.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "热搜加载失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HeaderActionPill(
                        label = "重试",
                        onClick = onRetry,
                    )
                }
            }
            hotwords.isEmpty() -> {
                CanvasMessageBox(text = "在左侧键入关键词，探索视频、番剧与 UP 主")
            }
            else -> {
                TvLazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(2),
                    contentPadding =
                        PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = 6.dp,
                            bottom = 20.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = hotwords,
                        key = { index, item -> "hot_${index}_${item.keyword}" },
                    ) { index, hotword ->
                        val entryMod =
                            if (index == 0) Modifier.focusRequester(primaryRequester) else Modifier
                        SearchHotwordItem(
                            modifier =
                                entryMod.focusSaverItem(focusSaver, "hot_$index"),
                            rank = index + 1,
                            showName = hotword.showName,
                            iconUrl = hotword.icon,
                            onClick = {
                                onSelect(hotword.keyword.ifBlank { hotword.showName })
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 右栏阶段二：双列宽敞实时搜索建议面板（彻底解决窄栏截断与拥挤痛点）。
 */
@Composable
private fun WideSuggestsCanvasSection(
    keyword: String,
    suggests: List<String>,
    onSelect: (String) -> Unit,
    focusSaver: FocusSaver,
) {
    val primaryRequester = remember { FocusRequester() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRestorer(primaryRequester)
                .focusGroup(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "搜索建议",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "按右方向键快速选择",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TvLazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(2),
            contentPadding =
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 20.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(
                key = "direct_search_action",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                val directMod =
                    if (suggests.isEmpty()) Modifier.focusRequester(primaryRequester) else Modifier
                WideSearchSuggestItem(
                    modifier = directMod,
                    keyword = "直接搜索「$keyword」",
                    isDirectAction = true,
                    onClick = { onSelect(keyword) },
                )
            }

            itemsIndexed(
                items = suggests,
                key = { index, item -> "wide_suggest_${index}_$item" },
            ) { index, suggest ->
                val firstSuggestMod =
                    if (index == 0) Modifier.focusRequester(primaryRequester) else Modifier
                WideSearchSuggestItem(
                    modifier = firstSuggestMod.focusSaverItem(focusSaver, "wide_suggest_$index"),
                    keyword = suggest,
                    onClick = { onSelect(suggest) },
                )
            }
        }
    }
}

/**
 * 右栏阶段三：全局统一 [TopNav] 分类栏 + 宽幅结果网格。
 */
@Composable
private fun ResultsCanvasSection(
    resultState: SearchResultUiState,
    gridState: LazyGridState,
    focusSaver: FocusSaver,
    navController: NavController,
    resultViewModel: SearchResultViewModel,
    watchLaterViewModel: WatchLaterViewModel,
) {
    val activeResult =
        resultState.results[resultState.activeType]
            ?: TypedSearchResult(resultState.activeType)
    val columnCount = searchTypeColumns[resultState.activeType] ?: 3
    val selectedTabIndex =
        searchNavItems.indexOfFirst { it.type == resultState.activeType }.coerceAtLeast(0)
    val canShowVideoFilter =
        resultState.activeType == SearchType.Video && Prefs.apiType == ApiType.Web
    var topNavHasFocus by remember { mutableStateOf(false) }

    val firstResultCardRequester = remember { FocusRequester() }
    val topNavAreaRequester = remember { FocusRequester() }
    val primaryEntryRequester =
        if (activeResult.items.isNotEmpty()) firstResultCardRequester else topNavAreaRequester

    key(resultState.keyword) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRestorer(primaryEntryRequester)
                    .focusGroup(),
        ) {
            // 顶部极简分类栏（与首页/个人页/分区页完全统一的 TopNav 样式）
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(topNavAreaRequester)
                        .onFocusChanged { topNavHasFocus = it.hasFocus }
                        .onPreviewKeyEvent { event ->
                            handleTopNavMenuKey(
                                topNavHasFocus = topNavHasFocus,
                                canShowVideoFilter = canShowVideoFilter,
                                event = event,
                                onOpenFilter = { resultViewModel.toggleFilter(true) },
                            )
                        },
            ) {
                TopNav(
                    items = searchNavItems,
                    selectedIndex = selectedTabIndex,
                    isLargePadding = false,
                    horizontalPadding = 8.dp,
                    onSelectedChanged = { item ->
                        (item as? SearchTypeNavItem)?.let { resultViewModel.switchType(it.type) }
                    },
                    onClick = { item ->
                        (item as? SearchTypeNavItem)?.let { resultViewModel.refreshType(it.type) }
                    },
                    trailingContent =
                        if (canShowVideoFilter) {
                            {
                                SearchFilterTriggerButton(
                                    selectedOrder = resultState.selectedOrder,
                                    selectedDuration = resultState.selectedDuration,
                                    onClick = { resultViewModel.toggleFilter(true) },
                                )
                            }
                        } else {
                            null
                        },
                )
            }

            // 结果网格区
            if (activeResult.items.isEmpty() && !activeResult.isLoading) {
                val emptyText =
                    if (activeResult.error) {
                        "加载失败，按确认键点击顶部分类可重试"
                    } else {
                        "暂无相关搜索结果"
                    }
                CanvasMessageBox(text = emptyText)
            } else {
                val firstVisibleIndex = gridState.firstVisibleItemIndex
                TvLazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(columnCount),
                    contentPadding =
                        PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = 10.dp,
                            bottom = 24.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(
                        items = activeResult.items,
                        key = { index, item -> searchResultItemKey(index, item) },
                    ) { index, item ->
                        val entryModifier =
                            if (index == firstVisibleIndex) {
                                Modifier.focusRequester(firstResultCardRequester)
                            } else {
                                Modifier
                            }
                        SearchResultCardCell(
                            modifier = entryModifier,
                            item = item,
                            focusSaver = focusSaver,
                            navController = navController,
                            watchLaterViewModel = watchLaterViewModel,
                        )
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
        }
    }

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

@Composable
private fun SearchResultCardCell(
    modifier: Modifier,
    item: SearchResultItem,
    focusSaver: FocusSaver,
    navController: NavController,
    watchLaterViewModel: WatchLaterViewModel,
) {
    when (item) {
        is SearchResultItem.VideoItem -> {
            val v = item.video
            val cardData =
                remember(v) {
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
                }
            SmallVideoCard(
                modifier = modifier.focusSaverItem(focusSaver, "video_${v.aid}"),
                data = cardData,
                onClick = { navController.navigateFromVideoCard(cardData) },
                onGoToDetailPage = {
                    navController.navigateFromVideoCard(cardData, forceDetail = true)
                },
                onGoToUpPage = {
                    navController.navigate(UserSpaceRoute(mid = v.mid, name = v.author))
                },
                onAddWatchLater = { watchLaterViewModel.addToView(aid = v.aid) },
            )
        }
        is SearchResultItem.PgcItem -> {
            val p = item.pgc
            val seasonData =
                remember(p) {
                    SeasonCardData(
                        seasonId = p.seasonId,
                        title = p.title.removeHtmlTags(),
                        cover = p.cover,
                        rating = if (p.star > 0) String.format("%.1f", p.star) else null,
                    )
                }
            SeasonCard(
                modifier = modifier.focusSaverItem(focusSaver, "pgc_${p.seasonId}"),
                data = seasonData,
                onClick = {
                    navController.navigate(PgcFeatureRoute(seasonId = p.seasonId.toLong()))
                },
            )
        }
        is SearchResultItem.UserItem -> {
            val u = item.user
            val cleanName = remember(u.name) { u.name.removeHtmlTags() }
            val cleanSign = remember(u.sign) { u.sign.removeHtmlTags() }
            UpCard(
                modifier = modifier.focusSaverItem(focusSaver, "user_${u.mid}"),
                avatar = u.avatar,
                username = cleanName,
                sign = cleanSign,
                onClick = {
                    navController.navigate(
                        UserSpaceRoute(mid = u.mid, name = cleanName, face = u.avatar),
                    )
                },
            )
        }
        is SearchResultItem.LiveRoomItem -> {
            val room = item.room
            val roomData =
                remember(room) {
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
                    )
                }
            LiveRoomCard(
                modifier = modifier.focusSaverItem(focusSaver, "live_${room.roomId}"),
                data = roomData,
                onClick = {
                    navController.navigate(LivePlayerRoute(roomId = room.roomId))
                },
            )
        }
    }
}

/**
 * 顶栏右侧轻量级筛选触发胶囊（已激活非默认筛选条件时高亮提示）。
 */
@Composable
private fun SearchFilterTriggerButton(
    selectedOrder: SearchFilterOrderType,
    selectedDuration: SearchFilterDuration,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val hasCustomOrder = selectedOrder != SearchFilterOrderType.ComprehensiveSort
    val hasCustomDuration = selectedDuration != SearchFilterDuration.All
    val isFiltered = hasCustomOrder || hasCustomDuration

    val summaryLabel =
        when {
            hasCustomOrder && hasCustomDuration ->
                "${selectedOrder.displayLabel()} · ${selectedDuration.displayLabel()}"
            hasCustomOrder -> selectedOrder.displayLabel()
            hasCustomDuration -> selectedDuration.displayLabel()
            else -> "筛选"
        }

    val containerColor =
        if (isFiltered) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.28f else 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.72f)
        }
    val contentColor =
        if (isFiltered) {
            if (isDark) MaterialTheme.colorScheme.onSurface else BVColors.PrimaryStrong
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val border =
        if (isFiltered) {
            Border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = BVShape.pill,
            )
        } else {
            Border.None
        }

    Surface(
        modifier =
            Modifier
                .height(30.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = border,
                focusedBorder = Border.None,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "筛选",
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = summaryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isFiltered) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SmallHeaderIconAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier =
            modifier
                .size(26.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.circle),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors =
            focusInvertedColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun HeaderActionPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier =
            modifier
                .height(28.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors =
            focusInvertedColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun CanvasMessageBox(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

private fun handleTopNavMenuKey(
    topNavHasFocus: Boolean,
    canShowVideoFilter: Boolean,
    event: KeyEvent,
    onOpenFilter: () -> Unit,
): Boolean {
    if (!topNavHasFocus || !canShowVideoFilter || !event.isMenuKey()) {
        return false
    }
    if (event.isKeyUp()) {
        onOpenFilter()
    }
    return true
}

private fun searchResultItemKey(
    index: Int,
    item: SearchResultItem,
): String =
    when (item) {
        is SearchResultItem.VideoItem -> "video_${item.video.aid}_$index"
        is SearchResultItem.PgcItem -> "pgc_${item.pgc.seasonId}_$index"
        is SearchResultItem.UserItem -> "user_${item.user.mid}_$index"
        is SearchResultItem.LiveRoomItem -> "live_${item.room.roomId}_$index"
    }
