package dev.frost819.newbv.app.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.state.search.SearchResultItem
import dev.frost819.newbv.app.ui.state.search.SearchResultUiState
import dev.frost819.newbv.app.ui.state.search.TypedSearchResult
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.repositories.SearchFilterDuration
import dev.frost819.newbv.biliapi.repositories.SearchFilterOrderType
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.biliapi.repositories.SearchType
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/**
 * 搜索结果页 ViewModel。
 *
 * 管理 5 类搜索结果（视频/番剧/影视/用户/直播间）的按需加载、缓存、分页、刷新与筛选。
 * 为降低 B 站 WBI 搜索接口风控概率，提交搜索时仅立即加载当前激活分类，其余分类在焦点切换至对应
 * `TopNav` 项时按需懒加载并缓存。
 *
 * @param searchRepository 搜索数据仓库
 */
@HiltViewModel
class SearchResultViewModel
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("SearchResultViewModel")

        companion object {
            private const val LOAD_TIMEOUT_MS = 10_000L

            /** 遥控器快速横扫 TopNav 时的轻量请求防抖窗口（毫秒）。 */
            internal const val SWITCH_TYPE_DEBOUNCE_MS = 150L
        }

        private val _uiState = MutableStateFlow(SearchResultUiState())

        /** 搜索结果状态流。 */
        val uiState = _uiState.asStateFlow()

        private val searchJobs = mutableMapOf<SearchType, Job>()
        private var switchDebounceJob: Job? = null

        /**
         * 设置搜索关键词并启动搜索。
         *
         * 重置所有分类分页和缓存，取消正在进行的请求，并优先立即加载当前激活分类 [SearchResultUiState.activeType]；
         * 其余分类保留未加载态，待切换至对应分类时按需懒加载。
         *
         * @param keyword 搜索关键词
         */
        fun search(keyword: String) {
            if (keyword.isBlank()) return
            switchDebounceJob?.cancel()
            searchJobs.values.forEach { it.cancel() }
            searchJobs.clear()

            val targetType = _uiState.value.activeType
            _uiState.update {
                it.copy(
                    keyword = keyword,
                    results =
                        SearchType.entries.associateWith { type ->
                            TypedSearchResult(type = type)
                        },
                )
            }
            loadMore(targetType)
        }

        /**
         * 切换当前激活的搜索类型 Tab。
         *
         * - 若目标分类已有缓存结果，直接秒切不重发请求；
         * - 若目标分类尚未加载，经轻量防抖（[SWITCH_TYPE_DEBOUNCE_MS]）后按需触发首页加载，
         *   避免遥控器快速横扫顶栏连续触发多次 WBI 搜索请求。
         *
         * @param type 目标搜索分类
         */
        fun switchType(type: SearchType) {
            switchDebounceJob?.cancel()
            _uiState.update { it.copy(activeType = type) }

            val state = _uiState.value
            if (state.keyword.isBlank()) return
            val result = state.results[type] ?: return
            if (result.items.isEmpty() && !result.isLoading && result.hasMore) {
                switchDebounceJob =
                    viewModelScope.launch {
                        delay(SWITCH_TYPE_DEBOUNCE_MS)
                        loadMore(type)
                    }
            }
        }

        /**
         * 强制刷新指定搜索分类的数据（用于点击已选中的 TopNav Tab 时刷新）。
         *
         * @param type 需要刷新的分类，默认为当前激活分类
         */
        fun refreshType(type: SearchType = _uiState.value.activeType) {
            if (_uiState.value.keyword.isBlank()) return
            switchDebounceJob?.cancel()
            searchJobs[type]?.cancel()
            updateResult(type) { TypedSearchResult(type = type) }
            loadMore(type)
        }

        /**
         * 加载指定分类的下一页数据。
         *
         * 若当前类型正在加载或已无更多数据，则跳过。
         *
         * @param type 目标搜索分类
         */
        fun loadMore(type: SearchType) {
            val state = _uiState.value
            val keyword = state.keyword
            if (keyword.isBlank()) return

            val result = state.results[type] ?: return
            if (result.isLoading || !result.hasMore) return

            updateResult(type) { it.copy(isLoading = true, error = false) }

            searchJobs[type]?.cancel()
            val job =
                viewModelScope.launch {
                    runCatching {
                        withTimeout(LOAD_TIMEOUT_MS) {
                            searchRepository.searchType(
                                keyword = keyword,
                                type = type,
                                tid = null,
                                order = state.selectedOrder,
                                duration = state.selectedDuration,
                                page = result.page,
                                preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                            )
                        }
                    }.onSuccess { searchResult ->
                        val newItems =
                            when (type) {
                                SearchType.Video -> searchResult.videos.map { SearchResultItem.VideoItem(it) }
                                SearchType.MediaBangumi, SearchType.MediaFt ->
                                    searchResult.pgcs.map {
                                        SearchResultItem.PgcItem(it)
                                    }
                                SearchType.BiliUser -> searchResult.users.map { SearchResultItem.UserItem(it) }
                                SearchType.LiveRoom -> searchResult.liveRooms.map { SearchResultItem.LiveRoomItem(it) }
                            }
                        updateResult(type) {
                            val existingIds =
                                it.items
                                    .map { item -> itemKey(item) }
                                    .toSet()
                            val dedupedNewItems =
                                newItems.filter { item ->
                                    itemKey(item) !in existingIds
                                }
                            val hasMore = searchResult.hasMore && dedupedNewItems.isNotEmpty()
                            it.copy(
                                items = it.items + dedupedNewItems,
                                page = searchResult.page,
                                isLoading = false,
                                hasMore = hasMore,
                                error = false,
                            )
                        }
                        logger.info {
                            "Loaded search result: type=$type, new=${newItems.size}, total=${result.items.size + newItems.size}"
                        }
                    }.onFailure { e ->
                        if (e is CancellationException && e !is TimeoutCancellationException) {
                            throw e
                        }
                        logger.warn { "Failed to load search result: type=$type, $e" }
                        updateResult(type) { it.copy(isLoading = false, error = true) }
                    }
                }
            searchJobs[type] = job
        }

        /**
         * 更新筛选条件并重新搜索当前激活分类（其余分类缓存一并重置）。
         *
         * @param order 排序方式
         * @param duration 时长筛选
         */
        fun updateFilter(
            order: SearchFilterOrderType,
            duration: SearchFilterDuration,
        ) {
            _uiState.update {
                it.copy(selectedOrder = order, selectedDuration = duration, showFilter = false)
            }
            search(_uiState.value.keyword)
        }

        /**
         * 显示/隐藏筛选弹窗。
         *
         * @param show 是否显示筛选弹窗
         */
        fun toggleFilter(show: Boolean) {
            _uiState.update { it.copy(showFilter = show) }
        }

        private fun itemKey(item: SearchResultItem): String =
            when (item) {
                is SearchResultItem.VideoItem -> "v_${item.video.aid}"
                is SearchResultItem.PgcItem -> "p_${item.pgc.seasonId}"
                is SearchResultItem.UserItem -> "u_${item.user.mid}"
                is SearchResultItem.LiveRoomItem -> "l_${item.room.roomId}"
            }

        private fun updateResult(
            type: SearchType,
            block: (TypedSearchResult) -> TypedSearchResult,
        ) {
            _uiState.update { state ->
                val current = state.results[type] ?: return@update state
                state.copy(results = state.results.toMutableMap().apply { put(type, block(current)) })
            }
        }
    }
