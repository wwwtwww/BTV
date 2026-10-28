package dev.frost819.newbv.app.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.state.search.SearchInputUiState
import dev.frost819.newbv.app.ui.state.search.SearchPaneStage
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.repository.SearchHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/**
 * 搜索输入页 ViewModel。
 *
 * 管理搜索关键词输入、左右双区协同阶段（[SearchPaneStage]）、热搜词加载、
 * 宽幅搜索建议与结果态接力建议列表（`relaySuggests`）以及搜索历史增删。
 *
 * @param searchRepository 搜索数据仓库
 * @param searchHistoryRepository 搜索历史持久化仓库
 */
@HiltViewModel
class SearchInputViewModel
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
        private val searchHistoryRepository: SearchHistoryRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("SearchInputViewModel")

        companion object {
            private const val LOAD_TIMEOUT_MS = 10_000L
        }

        private val _uiState = MutableStateFlow(SearchInputUiState())

        /** 搜索输入与阶段状态流。 */
        val uiState = _uiState.asStateFlow()

        private var suggestJob: Job? = null

        init {
            loadHotwords()
            loadHistories()
        }

        /**
         * 更新搜索关键词（键入或删改阶段）。
         *
         * - 当 [keyword] 为空白时：回到 [SearchPaneStage.Discovery] 初始态并清空建议；
         * - 当 [keyword] 非空时：切换至 [SearchPaneStage.Suggests] 宽幅建议态并拉取联想建议。
         *
         * @param keyword 最新输入的关键词文本
         */
        fun updateKeyword(keyword: String) {
            suggestJob?.cancel()
            if (keyword.isBlank()) {
                _uiState.update {
                    it.copy(
                        keyword = keyword,
                        stage = SearchPaneStage.Discovery,
                        suggests = emptyList(),
                        relaySuggests = emptyList(),
                        isLoadingSuggests = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        keyword = keyword,
                        stage = SearchPaneStage.Suggests,
                    )
                }
                loadSuggests(keyword)
            }
        }

        /**
         * 提交搜索：切换至 [SearchPaneStage.Results] 结果态，同步接力建议并持久化搜索历史。
         *
         * 若用户从长建议词提交导致新词自身联想过少，自动合并保留提交前的同批候选建议，
         * 确保结果浏览态下左栏下部始终驻留有价值的关联建议词。
         *
         * @param keyword 提交搜索的关键词
         * @param onCompleted 历史记录落盘后的回调
         */
        fun commitSearch(
            keyword: String,
            onCompleted: () -> Unit = {},
        ) {
            if (keyword.isBlank()) return
            suggestJob?.cancel()

            val fallbackSuggests =
                _uiState.value.suggests.ifEmpty {
                    _uiState.value.relaySuggests
                }

            _uiState.update {
                it.copy(
                    keyword = keyword,
                    stage = SearchPaneStage.Results,
                    relaySuggests = fallbackSuggests,
                    isLoadingSuggests = false,
                )
            }

            loadSuggestsForRelay(keyword = keyword, fallbackSuggests = fallbackSuggests)

            viewModelScope.launch {
                // 确保数据库写入不会因路由变更或重组而被中途取消
                withContext(NonCancellable) {
                    searchHistoryRepository.addHistory(keyword)
                }
                loadHistories()
                onCompleted()
            }
        }

        /**
         * 删除指定搜索历史。
         *
         * @param keyword 待删除的历史关键词
         */
        fun deleteHistory(keyword: String) {
            viewModelScope.launch {
                searchHistoryRepository.deleteHistory(keyword)
                loadHistories()
            }
        }

        /** 清空全部搜索历史。 */
        fun clearAllHistories() {
            viewModelScope.launch {
                searchHistoryRepository.clearAll()
                loadHistories()
            }
        }

        /** 刷新热搜词。 */
        fun refreshHotwords() {
            _uiState.update { it.copy(hotwordsError = false) }
            loadHotwords()
        }

        private fun loadHotwords() {
            if (_uiState.value.isLoadingHotwords) return
            _uiState.update { it.copy(isLoadingHotwords = true, hotwordsError = false) }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        searchRepository.getSearchHotwords(
                            limit = 50,
                            preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                        )
                    }
                }.onSuccess { hotwords ->
                    _uiState.update {
                        it.copy(hotwords = hotwords, isLoadingHotwords = false, hotwordsError = false)
                    }
                    logger.info { "Loaded hotwords: ${hotwords.size}" }
                }.onFailure { e ->
                    if (e is CancellationException && e !is TimeoutCancellationException) {
                        throw e
                    }
                    logger.warn { "Failed to load hotwords: $e" }
                    _uiState.update { it.copy(isLoadingHotwords = false, hotwordsError = true) }
                }
            }
        }

        private fun loadSuggests(keyword: String) {
            _uiState.update { it.copy(isLoadingSuggests = true) }

            suggestJob =
                viewModelScope.launch {
                    runCatching {
                        withTimeout(LOAD_TIMEOUT_MS) {
                            searchRepository.getSearchSuggest(
                                keyword = keyword,
                                preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                            )
                        }
                    }.onSuccess { suggests ->
                        _uiState.update { current ->
                            current.copy(
                                suggests = suggests,
                                relaySuggests = suggests.ifEmpty { current.relaySuggests },
                                isLoadingSuggests = false,
                            )
                        }
                    }.onFailure { e ->
                        if (e is CancellationException && e !is TimeoutCancellationException) {
                            throw e
                        }
                        logger.warn { "Failed to load suggests: $e" }
                        _uiState.update { it.copy(suggests = emptyList(), isLoadingSuggests = false) }
                    }
                }
        }

        private fun loadSuggestsForRelay(
            keyword: String,
            fallbackSuggests: List<String>,
        ) {
            suggestJob =
                viewModelScope.launch {
                    runCatching {
                        withTimeout(LOAD_TIMEOUT_MS) {
                            searchRepository.getSearchSuggest(
                                keyword = keyword,
                                preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                            )
                        }
                    }.onSuccess { freshSuggests ->
                        val mergedRelay = (freshSuggests + fallbackSuggests).distinct()
                        _uiState.update { current ->
                            current.copy(
                                suggests = freshSuggests,
                                relaySuggests = mergedRelay,
                                isLoadingSuggests = false,
                            )
                        }
                    }.onFailure { e ->
                        if (e is CancellationException && e !is TimeoutCancellationException) {
                            throw e
                        }
                        logger.warn { "Failed to load relay suggests: $e" }
                        _uiState.update { current ->
                            current.copy(
                                relaySuggests = fallbackSuggests,
                                isLoadingSuggests = false,
                            )
                        }
                    }
                }
        }

        private fun loadHistories() {
            viewModelScope.launch {
                runCatching {
                    searchHistoryRepository.getHistories(20)
                }.onSuccess { histories ->
                    _uiState.update { it.copy(histories = histories) }
                }.onFailure { e ->
                    logger.warn { "Failed to load histories: $e" }
                }
            }
        }
    }
