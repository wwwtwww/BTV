package dev.frost819.newbv.app.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.biliapi.entity.home.RecommendData
import dev.frost819.newbv.biliapi.entity.home.RecommendPage
import dev.frost819.newbv.biliapi.entity.rank.PopularVideoPage
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.biliapi.entity.user.DynamicVideo
import dev.frost819.newbv.biliapi.repositories.RecommendVideoRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/** 网络请求超时时间（毫秒）。 */
private const val LOAD_TIMEOUT_MS = 10_000L

/**
 * 首页 Tab 状态。
 */
data class HomeUiState(
    val recommendItems: List<UgcItem> = emptyList(),
    val recommendLoading: Boolean = false,
    val recommendHasMore: Boolean = true,
    val recommendError: Boolean = false,
    val popularItems: List<UgcItem> = emptyList(),
    val popularLoading: Boolean = false,
    val popularHasMore: Boolean = true,
    val popularError: Boolean = false,
    val dynamicItems: List<DynamicVideo> = emptyList(),
    val dynamicLoading: Boolean = false,
    val dynamicHasMore: Boolean = true,
    val dynamicError: Boolean = false,
    val isLogin: Boolean = false,
    val currentUid: Long = 0L,
)

/**
 * 首页 ViewModel。
 *
 * 管理推荐、热门、动态三个 Tab 的数据加载、分页、刷新。
 * 使用 [StateFlow] 暴露状态，UI 通过 [uiState] 观察。
 *
 * @property recommendVideoRepository 推荐/热门数据仓库。
 * @property userRepository 动态数据仓库。
 * @property accountRepository 账户仓库（监听登录状态变化）。
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val recommendVideoRepository: RecommendVideoRepository,
        private val userRepository: UserRepository,
        private val accountRepository: AccountRepositoryImpl,
    ) : ViewModel() {
        private val logger = Loggers.get("HomeViewModel")

        /** 将 data 层 ApiType 映射为 bili-api 层 ApiType。 */
        private fun prefApiType(): BiliApiType =
            when (Prefs.apiType) {
                DataApiType.Web -> BiliApiType.Web
                DataApiType.App -> BiliApiType.App
            }

        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        private var recommendNextPage = RecommendPage()
        private var popularNextPage = PopularVideoPage()
        private var dynamicCurrentPage = 0
        private var dynamicHistoryOffset: String? = null
        private var dynamicUpdateBaseline: String? = null

        private var recommendLoadJob: Job? = null
        private var popularLoadJob: Job? = null
        private var dynamicLoadJob: Job? = null

        init {
            _uiState.update { it.copy(isLogin = Prefs.isLogin) }
            loadRecommend()
            loadPopular()
            if (Prefs.isLogin) loadDynamic()

            viewModelScope.launch {
                accountRepository.uiState
                    .map { it.uid to it.isLogin }
                    .distinctUntilChanged()
                    .collect { (uid, isLogin) ->
                        if (isLogin != _uiState.value.isLogin) {
                            updateLoginState(isLogin)
                        } else if (isLogin && uid != 0L && uid != _uiState.value.currentUid) {
                            onUserSwitched(uid)
                        }
                    }
            }
        }

        /**
         * 加载推荐视频。
         *
         * 列表为空时视为首次加载，连续请求直到 >= 24 条或达到 3 次上限；
         * 列表非空时视为加载更多，只请求一页。
         *
         * 每页请求使用独立的 [LOAD_TIMEOUT_MS] 超时：首次补齐首屏时某一页超时
         * 不应导致已加载的数据被标记为失败（避免"部分列表 + 报错"）。仅当列表
         * 最终为空时才置 [HomeUiState.recommendError]。
         */
        fun loadRecommend() {
            val current = _uiState.value
            if (current.recommendLoading || !current.recommendHasMore) {
                logger.debug {
                    "loadRecommend skipped: loading=${current.recommendLoading}, hasMore=${current.recommendHasMore}"
                }
                return
            }

            _uiState.update { it.copy(recommendLoading = true, recommendError = false) }

            recommendLoadJob =
                viewModelScope.launch {
                    // 首次加载（列表为空）需要连续请求补齐首屏；已有数据时每次只追加一页，
                    val isFirstLoad = _uiState.value.recommendItems.isEmpty()
                    val maxLoadCount = if (isFirstLoad) 3 else 1
                    var loadCount = 0
                    var failed = false
                    try {
                        while (loadCount < maxLoadCount && isActive) {
                            var fetchedData: RecommendData? = null
                            runCatching {
                                withTimeout(LOAD_TIMEOUT_MS) {
                                    recommendVideoRepository.getRecommendVideos(
                                        page = recommendNextPage,
                                        preferApiType = prefApiType(),
                                    )
                                }
                            }.onSuccess { data ->
                                fetchedData = data
                            }.onFailure { error ->
                                if (error is CancellationException && error !is TimeoutCancellationException) {
                                    throw error
                                }
                                logger.error(error) { "Failed to load recommend videos" }
                                failed = true
                            }

                            val data = fetchedData ?: break

                            if (data.items.isEmpty()) {
                                logger.debug {
                                    "loadRecommend received transient empty items on page $recommendNextPage, cursor preserved for retry"
                                }
                                if (!data.hasMore) {
                                    _uiState.update { it.copy(recommendHasMore = false) }
                                }
                                // 遇到偶发空页时不推进游标，保持游标以便下次重试同一页（避免永久跳页丢失数据）
                                break
                            }

                            // 仅在成功获取非空数据后推进游标
                            recommendNextPage = data.nextPage
                            _uiState.update {
                                it.copy(
                                    recommendItems = it.recommendItems + data.items,
                                    recommendHasMore = data.hasMore,
                                )
                            }
                            loadCount++
                            if (!data.hasMore || !isFirstLoad || _uiState.value.recommendItems.size >= 24) break
                        }

                        // 首屏补齐失败时，只要已拿到部分数据就静默停止（避免"部分列表 + 报错"）；
                        // 加载更多失败则照常报错，让底部提示可重试
                        val hasItems = _uiState.value.recommendItems.isNotEmpty()
                        val showError = failed && (!hasItems || !isFirstLoad)
                        if (showError) {
                            _uiState.update { it.copy(recommendError = true) }
                        }
                    } finally {
                        _uiState.update { it.copy(recommendLoading = false) }
                    }
                }
        }

        /**
         * 清空推荐数据并重新加载。
         */
        fun refreshRecommend() {
            recommendLoadJob?.cancel()
            recommendNextPage = RecommendPage()
            _uiState.update {
                it.copy(
                    recommendItems = emptyList(),
                    recommendLoading = false,
                    recommendHasMore = true,
                    recommendError = false,
                )
            }
            loadRecommend()
        }

        /**
         * 加载更多热门视频。
         *
         * 超过 [LOAD_TIMEOUT_MS] 未返回时标记为加载失败。
         */
        fun loadPopular() {
            val current = _uiState.value
            if (current.popularLoading || !current.popularHasMore) {
                logger.debug {
                    "loadPopular skipped: loading=${current.popularLoading}, hasMore=${current.popularHasMore}"
                }
                return
            }

            _uiState.update { it.copy(popularLoading = true, popularError = false) }

            popularLoadJob =
                viewModelScope.launch {
                    try {
                        runCatching {
                            withTimeout(LOAD_TIMEOUT_MS) {
                                val data =
                                    recommendVideoRepository.getPopularVideos(
                                        page = popularNextPage,
                                        preferApiType = prefApiType(),
                                    )
                                popularNextPage = data.nextPage
                                _uiState.update {
                                    it.copy(
                                        popularItems = it.popularItems + data.list,
                                        popularHasMore = !data.noMore,
                                    )
                                }
                            }
                        }.onFailure { error ->
                            if (error is CancellationException && error !is TimeoutCancellationException) {
                                throw error
                            }
                            logger.error(error) { "Failed to load popular videos" }
                            _uiState.update { it.copy(popularError = true) }
                        }
                    } finally {
                        _uiState.update { it.copy(popularLoading = false) }
                    }
                }
        }

        /**
         * 清空热门数据并重新加载。
         */
        fun refreshPopular() {
            popularLoadJob?.cancel()
            popularNextPage = PopularVideoPage()
            _uiState.update {
                it.copy(
                    popularItems = emptyList(),
                    popularLoading = false,
                    popularHasMore = true,
                    popularError = false,
                )
            }
            loadPopular()
        }

        /**
         * 加载更多动态视频。
         *
         * 需要登录，未登录时不执行。
         * 超过 [LOAD_TIMEOUT_MS] 未返回时标记为加载失败。
         */
        fun loadDynamic() {
            if (!_uiState.value.isLogin) return
            val current = _uiState.value
            if (current.dynamicLoading || !current.dynamicHasMore) return

            _uiState.update { it.copy(dynamicLoading = true, dynamicError = false) }

            dynamicLoadJob =
                viewModelScope.launch {
                    val nextPage = dynamicCurrentPage + 1
                    try {
                        runCatching {
                            withTimeout(LOAD_TIMEOUT_MS) {
                                val data =
                                    userRepository.getDynamicVideos(
                                        page = nextPage,
                                        offset = dynamicHistoryOffset.orEmpty(),
                                        updateBaseline = dynamicUpdateBaseline.orEmpty(),
                                        preferApiType = prefApiType(),
                                    )
                                dynamicCurrentPage = nextPage
                                dynamicHistoryOffset = data.historyOffset
                                dynamicUpdateBaseline = data.updateBaseline
                                _uiState.update {
                                    it.copy(
                                        dynamicItems = it.dynamicItems + data.videos,
                                        dynamicHasMore = data.hasMore,
                                    )
                                }
                            }
                        }.onFailure { error ->
                            if (error is CancellationException && error !is TimeoutCancellationException) {
                                throw error
                            }
                            logger.error(error) { "Failed to load dynamic videos" }
                            _uiState.update { it.copy(dynamicError = true) }
                        }
                    } finally {
                        _uiState.update { it.copy(dynamicLoading = false) }
                    }
                }
        }

        /**
         * 清空动态数据并重新加载。
         */
        fun refreshDynamic() {
            dynamicLoadJob?.cancel()
            dynamicCurrentPage = 0
            dynamicHistoryOffset = null
            dynamicUpdateBaseline = null
            _uiState.update {
                it.copy(
                    dynamicItems = emptyList(),
                    dynamicLoading = false,
                    dynamicHasMore = true,
                    dynamicError = false,
                )
            }
            loadDynamic()
        }

        /**
         * 刷新指定 Tab 的数据。
         *
         * @param tab 目标 Tab。
         */
        fun refresh(tab: dev.frost819.newbv.data.datastore.HomeTopNavItem) {
            when (tab) {
                dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend -> refreshRecommend()
                dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular -> refreshPopular()
                dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics -> refreshDynamic()
            }
        }

        /**
         * 加载指定 Tab 的更多数据。
         *
         * @param tab 目标 Tab。
         */
        fun loadMore(tab: dev.frost819.newbv.data.datastore.HomeTopNavItem) {
            when (tab) {
                dev.frost819.newbv.data.datastore.HomeTopNavItem.Recommend -> loadRecommend()
                dev.frost819.newbv.data.datastore.HomeTopNavItem.Popular -> loadPopular()
                dev.frost819.newbv.data.datastore.HomeTopNavItem.Dynamics -> loadDynamic()
            }
        }

        /**
         * 更新登录状态（登录/登出时调用）。
         */
        fun updateLoginState(isLogin: Boolean) {
            _uiState.update { it.copy(isLogin = isLogin) }
            if (isLogin) {
                if (_uiState.value.dynamicItems.isEmpty()) loadDynamic()
            } else {
                dynamicCurrentPage = 0
                dynamicHistoryOffset = null
                dynamicUpdateBaseline = null
                _uiState.update {
                    it.copy(dynamicItems = emptyList(), dynamicHasMore = true, dynamicError = false, currentUid = 0L)
                }
            }
        }

        /**
         * 切换用户后刷新所有数据。
         *
         * 清空推荐/热门/动态列表并重新加载，确保展示新用户的个性化内容。
         *
         * @param uid 新用户 UID。
         */
        private fun onUserSwitched(uid: Long) {
            _uiState.update { it.copy(currentUid = uid) }
            refreshRecommend()
            refreshPopular()
            refreshDynamic()
        }
    }
