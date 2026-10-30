package dev.frost819.newbv.app.viewmodel.personal

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.biliapi.entity.FavoriteFolderData
import dev.frost819.newbv.biliapi.entity.FavoriteFolderMetadata
import dev.frost819.newbv.biliapi.entity.FavoriteItem
import dev.frost819.newbv.biliapi.entity.season.FollowingSeason
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonStatus
import dev.frost819.newbv.biliapi.entity.season.FollowingSeasonType
import dev.frost819.newbv.biliapi.entity.user.HistoryItem
import dev.frost819.newbv.biliapi.entity.user.HistoryItemType
import dev.frost819.newbv.biliapi.entity.user.ToViewItem
import dev.frost819.newbv.biliapi.repositories.FavoriteRepository
import dev.frost819.newbv.biliapi.repositories.HistoryRepository
import dev.frost819.newbv.biliapi.repositories.SeasonRepository
import dev.frost819.newbv.biliapi.repositories.ToViewRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/** 网络请求超时时间（毫秒）。 */
private const val LOAD_TIMEOUT_MS = 10_000L

/** PersonalViewModel 的 UI 效果（一次性事件）。 */
sealed interface PersonalUiEffect {
    data class ShowToast(
        val message: String,
    ) : PersonalUiEffect
}

/**
 * 个人页 UI 状态。
 *
 * 管理稍后再看、历史、收藏、追番四个 Tab 的数据。
 *
 * @property toViewLoading 稍后再看加载中。
 * @property toViewError 稍后再看加载失败。
 * @property historyItems 历史列表。
 * @property historyLoading 历史加载中。
 * @property historyError 历史加载失败。
 * @property historyHasMore 历史是否还有更多。
 * @property favoriteFolders 收藏夹列表。
 * @property favoriteItems 当前收藏夹视频列表。
 * @property favoriteLoading 收藏加载中。
 * @property favoriteError 收藏加载失败。
 * @property favoriteHasMore 收藏夹是否还有更多。
 * @property currentFolderId 当前选中的收藏夹 ID。
 * @property followingSeasons 追番列表。
 * @property followingLoading 追番加载中。
 * @property followingError 追番加载失败。
 * @property followingHasMore 追番是否还有更多。
 * @property followingType 追番类型筛选。
 * @property followingStatus 追番状态筛选。
 * @property isLogin 是否已登录。
 */
data class PersonalUiState(
    val toViewLoading: Boolean = false,
    val toViewError: Boolean = false,
    val historyItems: List<HistoryItem> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: Boolean = false,
    val historyHasMore: Boolean = true,
    val favoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    val favoriteItems: List<FavoriteItem> = emptyList(),
    val favoriteLoading: Boolean = false,
    val favoriteError: Boolean = false,
    val favoriteHasMore: Boolean = true,
    val currentFolderId: Long = -1L,
    val followingSeasons: List<FollowingSeason> = emptyList(),
    val followingLoading: Boolean = false,
    val followingError: Boolean = false,
    val followingHasMore: Boolean = true,
    val followingType: FollowingSeasonType = FollowingSeasonType.Bangumi,
    val followingStatus: FollowingSeasonStatus = FollowingSeasonStatus.All,
    val isLogin: Boolean = false,
)

/**
 * 个人页 ViewModel。
 *
 * 管理稍后再看、历史、收藏、追番四个 Tab 的数据加载、分页、刷新。
 * 使用 [StateFlow] 暴露状态，UI 通过 [uiState] 观察。
 *
 * @property toViewRepository 稍后再看仓库。
 * @property historyRepository 历史仓库。
 * @property favoriteRepository 收藏仓库。
 * @property seasonRepository 追番仓库。
 */
@HiltViewModel
class PersonalViewModel
    @Inject
    constructor(
        private val toViewRepository: ToViewRepository,
        private val historyRepository: HistoryRepository,
        private val favoriteRepository: FavoriteRepository,
        private val seasonRepository: SeasonRepository,
        private val videoInfoRepository: VideoInfoRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("PersonalViewModel")

        private val _effect = MutableSharedFlow<PersonalUiEffect>()
        val effect = _effect.asSharedFlow()

        private fun prefApiType(): BiliApiType =
            when (Prefs.apiType) {
                DataApiType.Web -> BiliApiType.Web
                DataApiType.App -> BiliApiType.App
            }

        private val _uiState = MutableStateFlow(PersonalUiState())
        val uiState: StateFlow<PersonalUiState> = _uiState.asStateFlow()

        val toViewItems = mutableStateListOf<ToViewItem>()

        private var historyCursor: Long = 0L
        private var favoritePageNumber: Int = 1
        private var followingPageNumber: Int = 1
        private var followingTotal: Int = 0

        init {
            _uiState.update { it.copy(isLogin = Prefs.isLogin) }
            if (Prefs.isLogin) {
                loadToView()
                loadHistory()
                loadFavoriteFolders()
                loadFollowingSeasons()
            }
        }

        // region ToView

        /**
         * 加载稍后再看列表。
         *
         * 稍后再看接口不支持分页，一次性加载全部。
         */
        fun loadToView() {
            viewModelScope.launch {
                if (_uiState.value.toViewLoading) return@launch

                _uiState.update { it.copy(toViewLoading = true, toViewError = false) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val data =
                            toViewRepository.getToView(
                                cursor = 0,
                                preferApiType = prefApiType(),
                            )
                        toViewItems.clear()
                        toViewItems.addAll(data.data)
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load toview" }
                    _uiState.update { it.copy(toViewError = true) }
                }

                _uiState.update { it.copy(toViewLoading = false) }
            }
        }

        /**
         * 删除稍后再看项（原地移除，保持焦点不丢失）。
         *
         * @param aid 视频 AV 号。
         */
        fun delToView(
            aid: Long,
            viewed: Boolean = false,
        ) {
            viewModelScope.launch {
                runCatching {
                    toViewRepository.delToView(
                        aid = aid,
                        viewed = viewed,
                        preferApiType = prefApiType(),
                    )
                    toViewItems.removeAll { it.oid == aid }
                    _effect.emit(PersonalUiEffect.ShowToast("已移除稍后再看"))
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to delete toview $aid" }
                    _effect.emit(PersonalUiEffect.ShowToast("移除失败: ${error.message ?: "未知错误"}"))
                }
            }
        }

        /**
         * 刷新稍后再看列表。
         */
        fun refreshToView() {
            toViewItems.clear()
            _uiState.update { it.copy(toViewError = false) }
            loadToView()
        }

        // endregion

        // region History

        /**
         * 加载更多历史记录。
         *
         * 使用 cursor 分页，首次加载 cursor=0。
         * 超过 [LOAD_TIMEOUT_MS] 未返回时标记为加载失败。
         */
        fun loadHistory() {
            viewModelScope.launch {
                val current = _uiState.value
                if (current.historyLoading || !current.historyHasMore) return@launch

                _uiState.update { it.copy(historyLoading = true, historyError = false) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val data =
                            historyRepository.getHistories(
                                cursor = historyCursor,
                                preferApiType = prefApiType(),
                            )
                        historyCursor = data.cursor
                        _uiState.update {
                            it.copy(
                                historyItems = it.historyItems + data.data,
                                historyHasMore = data.cursor != 0L,
                            )
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load history" }
                    _uiState.update { it.copy(historyError = true) }
                }

                _uiState.update { it.copy(historyLoading = false) }
            }
        }

        /**
         * 刷新历史记录。
         */
        fun refreshHistory() {
            historyCursor = 0L
            _uiState.update {
                it.copy(historyItems = emptyList(), historyHasMore = true, historyError = false)
            }
            loadHistory()
        }

        // endregion

        // region Favorite

        /**
         * 加载收藏夹列表。
         *
         * 加载完成后自动加载第一个收藏夹的视频列表。
         */
        fun loadFavoriteFolders() {
            viewModelScope.launch {
                if (_uiState.value.favoriteLoading) return@launch

                _uiState.update { it.copy(favoriteLoading = true, favoriteError = false) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val mid = Prefs.uid
                        val folders =
                            favoriteRepository.getAllFavoriteFolderMetadataList(
                                mid = mid,
                                preferApiType = prefApiType(),
                            )
                        _uiState.update { it.copy(favoriteFolders = folders) }
                        if (folders.isNotEmpty()) {
                            val firstFolder = folders.first()
                            _uiState.update { it.copy(currentFolderId = firstFolder.id) }
                            loadFavoriteItems(firstFolder.id, forceRefresh = true)
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load favorite folders" }
                    _uiState.update { it.copy(favoriteError = true) }
                }

                _uiState.update { it.copy(favoriteLoading = false) }
            }
        }

        /**
         * 加载收藏夹视频列表。
         *
         * @param folderId 收藏夹 ID。
         * @param forceRefresh 是否强制刷新（切换收藏夹时为 true）。
         */
        fun loadFavoriteItems(
            folderId: Long,
            forceRefresh: Boolean = false,
        ) {
            viewModelScope.launch {
                val current = _uiState.value
                if (current.favoriteLoading) return@launch

                if (forceRefresh || folderId != current.currentFolderId) {
                    favoritePageNumber = 1
                    _uiState.update {
                        it.copy(
                            favoriteItems = emptyList(),
                            favoriteHasMore = true,
                            favoriteError = false,
                            currentFolderId = folderId,
                        )
                    }
                }

                if (!current.favoriteHasMore && !forceRefresh) return@launch

                _uiState.update { it.copy(favoriteLoading = true, favoriteError = false) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val data: FavoriteFolderData =
                            favoriteRepository.getFavoriteFolderData(
                                mediaId = folderId,
                                pageNumber = favoritePageNumber,
                                preferApiType = prefApiType(),
                            )
                        favoritePageNumber++
                        val videoItems =
                            data.medias.filter {
                                it.type ==
                                    dev.frost819.newbv.biliapi.entity.FavoriteItemType.Video
                            }
                        _uiState.update {
                            it.copy(
                                favoriteItems = it.favoriteItems + videoItems,
                                favoriteHasMore = data.hasMore,
                            )
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load favorite items for folder $folderId" }
                    _uiState.update { it.copy(favoriteError = true) }
                }

                _uiState.update { it.copy(favoriteLoading = false) }
            }
        }

        /**
         * 刷新收藏夹列表。
         */
        fun refreshFavorite() {
            _uiState.update {
                it.copy(
                    favoriteFolders = emptyList(),
                    favoriteItems = emptyList(),
                    favoriteHasMore = true,
                    favoriteError = false,
                    currentFolderId = -1L,
                )
            }
            loadFavoriteFolders()
        }

        // endregion

        // region FollowingSeason

        /**
         * 加载更多追番列表。
         *
         * 使用页码分页，每页 30 条。
         */
        fun loadFollowingSeasons() {
            viewModelScope.launch {
                val current = _uiState.value
                if (current.followingLoading || !current.followingHasMore) return@launch

                _uiState.update { it.copy(followingLoading = true, followingError = false) }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val data =
                            seasonRepository.getFollowingSeasons(
                                type = current.followingType,
                                status = current.followingStatus,
                                pageNumber = followingPageNumber,
                                preferApiType = prefApiType(),
                            )
                        followingPageNumber++
                        followingTotal = data.total
                        _uiState.update {
                            it.copy(
                                followingSeasons = it.followingSeasons + data.list,
                                followingHasMore = it.followingSeasons.size + data.list.size < followingTotal,
                            )
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load following seasons" }
                    _uiState.update { it.copy(followingError = true) }
                }

                _uiState.update { it.copy(followingLoading = false) }
            }
        }

        /**
         * 设置追番筛选条件并重新加载。
         *
         * @param type 类型（Bangumi/Cinema）。
         * @param status 状态（All/Want/Watching/Watched）。
         */
        fun setFollowingFilter(
            type: FollowingSeasonType,
            status: FollowingSeasonStatus,
        ) {
            followingPageNumber = 1
            followingTotal = 0
            _uiState.update {
                it.copy(
                    followingType = type,
                    followingStatus = status,
                    followingSeasons = emptyList(),
                    followingHasMore = true,
                    followingError = false,
                )
            }
            loadFollowingSeasons()
        }

        /**
         * 刷新追番列表。
         */
        fun refreshFollowingSeasons() {
            followingPageNumber = 1
            followingTotal = 0
            _uiState.update {
                it.copy(
                    followingSeasons = emptyList(),
                    followingHasMore = true,
                    followingError = false,
                )
            }
            loadFollowingSeasons()
        }

        // endregion

        /**
         * 删除单条历史记录。
         *
         * 采用乐观更新策略，立即从 UI 移除；若接口失败则回滚列表并提示错误。
         *
         * @param item 待删除的历史记录条目
         */
        fun deleteHistoryItem(item: HistoryItem) {
            val previousList = _uiState.value.historyItems
            _uiState.update { state ->
                state.copy(
                    historyItems = state.historyItems.filterNot { it.oid == item.oid && it.type == item.type },
                )
            }
            viewModelScope.launch {
                runCatching {
                    val business =
                        when (item.type) {
                            HistoryItemType.Pgc -> "pgc"
                            else -> "archive"
                        }
                    val kid = "${business}_${item.oid}"
                    historyRepository.deleteHistory(kid)
                    _effect.emit(PersonalUiEffect.ShowToast("已删除历史记录"))
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to delete history item: oid=${item.oid}" }
                    _uiState.update { it.copy(historyItems = previousList) }
                    val msg = error.message ?: "未知错误"
                    _effect.emit(PersonalUiEffect.ShowToast("删除失败: $msg"))
                }
            }
        }

        /**
         * 从当前收藏夹中移除视频。
         *
         * 采用乐观更新策略，立即从 UI 移除；若接口失败则回滚列表并提示错误。
         *
         * @param aid 视频 AV 号
         * @param folderId 收藏夹 ID
         */
        fun delFavoriteItem(
            aid: Long,
            folderId: Long,
        ) {
            val previousList = _uiState.value.favoriteItems
            _uiState.update { state ->
                state.copy(
                    favoriteItems = state.favoriteItems.filterNot { it.id == aid },
                )
            }
            viewModelScope.launch {
                runCatching {
                    favoriteRepository.delVideoFromFavoriteFolder(
                        aid = aid,
                        delMediaIds = listOf(folderId),
                        preferApiType = prefApiType(),
                    )
                    _effect.emit(PersonalUiEffect.ShowToast("已取消收藏"))
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to remove favorite item: aid=$aid, folderId=$folderId" }
                    _uiState.update { it.copy(favoriteItems = previousList) }
                    val msg = error.message ?: "未知错误"
                    _effect.emit(PersonalUiEffect.ShowToast("取消收藏失败: $msg"))
                }
            }
        }

        /**
         * 取消追番或追剧。
         *
         * 采用乐观更新策略，立即从 UI 移除；若接口失败则回滚列表并提示错误。
         *
         * @param seasonId 剧集 Season ID
         */
        fun cancelFollowingSeason(seasonId: Int) {
            val previousList = _uiState.value.followingSeasons
            _uiState.update { state ->
                state.copy(
                    followingSeasons = state.followingSeasons.filterNot { it.seasonId == seasonId },
                )
            }
            viewModelScope.launch {
                runCatching {
                    seasonRepository.delSeasonFollow(
                        seasonId = seasonId,
                        preferApiType = prefApiType(),
                    )
                    _effect.emit(PersonalUiEffect.ShowToast("已取消追番"))
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to cancel following season: seasonId=$seasonId" }
                    _uiState.update { it.copy(followingSeasons = previousList) }
                    val msg = error.message ?: "未知错误"
                    _effect.emit(PersonalUiEffect.ShowToast("取消追番失败: $msg"))
                }
            }
        }

        /**
         * 稍后再看「全部播放」。
         *
         * 将整张稍后再看列表填充至 [VideoInfoRepository]，智能寻址首个未看完视频并触发起播回调。
         *
         * @param onPlay 起播回调，传入目标视频卡片数据
         */
        fun playAllToView(onPlay: (VideoCardData) -> Unit) {
            if (toViewItems.isEmpty()) {
                viewModelScope.launch {
                    _effect.emit(PersonalUiEffect.ShowToast("稍后再看列表为空"))
                }
                return
            }
            val videoList =
                toViewItems.map { item ->
                    VideoListItem(
                        aid = item.oid,
                        cid = item.cid,
                        epid = item.epid,
                        seasonId = item.seasonId,
                        title = item.title,
                    )
                }
            videoInfoRepository.updateVideoList(videoList)

            // progress == -1 表示无播放数据（未开始/哨兵值），in 0 until duration 已在区间上隐含排除了 -1
            val target =
                toViewItems.firstOrNull { it.progress in 0 until it.duration }
                    ?: toViewItems.first()
            onPlay(
                VideoCardData(
                    avid = target.oid,
                    bvid = target.bvid,
                    cid = target.cid,
                    epid = target.epid,
                    title = target.title,
                    cover = target.cover,
                    upName = target.author,
                ),
            )
        }

        /**
         * 收藏夹「全部播放」。
         *
         * 将当前收藏夹视频列表填充至 [VideoInfoRepository]，从首个视频触发起播回调。
         *
         * @param onPlay 起播回调，传入目标视频卡片数据
         */
        fun playAllFavorite(onPlay: (VideoCardData) -> Unit) {
            val items = _uiState.value.favoriteItems
            if (items.isEmpty()) {
                viewModelScope.launch {
                    _effect.emit(PersonalUiEffect.ShowToast("当前收藏夹为空"))
                }
                return
            }
            val videoList =
                items.map { item ->
                    VideoListItem(
                        aid = item.id,
                        // B 站收藏夹明细接口 (/x/v3/fav/resource/list) 仅返回稿件维度元数据，不包含分 P 的 cid。
                        // 此处传入 0L 作为有意设计的哨兵占位值，播放器起播时会自动调用视频详情接口补全真实 cid。
                        cid = 0L,
                        title = item.title,
                    )
                }
            videoInfoRepository.updateVideoList(videoList)

            val target = items.first()
            onPlay(
                VideoCardData(
                    avid = target.id,
                    bvid = target.bvid,
                    title = target.title,
                    cover = target.cover,
                    upName = target.upper.name,
                    upMid = target.upper.mid,
                ),
            )
        }

        /**
         * 刷新指定 Tab 的数据。
         *
         * @param tab 目标 Tab。
         */
        fun refresh(tab: dev.frost819.newbv.data.datastore.PersonalTopNavItem) {
            when (tab) {
                dev.frost819.newbv.data.datastore.PersonalTopNavItem.ToView -> refreshToView()
                dev.frost819.newbv.data.datastore.PersonalTopNavItem.History -> refreshHistory()
                dev.frost819.newbv.data.datastore.PersonalTopNavItem.Favorite -> refreshFavorite()
                dev.frost819.newbv.data.datastore.PersonalTopNavItem.FollowingSeason -> refreshFollowingSeasons()
            }
        }
    }
