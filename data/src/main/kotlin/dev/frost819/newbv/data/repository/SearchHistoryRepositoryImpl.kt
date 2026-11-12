package dev.frost819.newbv.data.repository

import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.dao.SearchHistoryDao
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SearchHistoryRepository] 的默认实现。
 *
 * 使用 [SearchHistoryDao] 持久化搜索历史，受 [Prefs.incognitoMode] 控制。
 * 无痕模式下不记录搜索历史。
 *
 * @param searchHistoryDao Room DAO
 */
@Singleton
class SearchHistoryRepositoryImpl
    @Inject
    constructor(
        private val searchHistoryDao: SearchHistoryDao,
    ) : SearchHistoryRepository {
        override suspend fun getHistories(count: Int): List<SearchHistoryEntity> = searchHistoryDao.getHistories(count)

        /**
         * 记录一条搜索历史。
         *
         * 无痕模式（[Prefs.incognitoMode]）下直接返回，不落库。
         * 借 `keyword` 上的唯一索引走单条 [SearchHistoryDao.upsert]（`ON CONFLICT REPLACE`）完成
         * 「存在则更新、不存在则插入」，消除旧实现 find→insert 的 TOCTOU 竞态：
         * 并发重复搜索同一关键词时不再因唯一索引冲突抛 `SQLiteConstraintException`。
         * search_history 表仅含 keyword + search_date，REPLACE 全行替换语义合理
         * （新搜索自然覆盖旧时间戳）。
         */
        override suspend fun addHistory(keyword: String) {
            if (Prefs.incognitoMode) return
            searchHistoryDao.upsert(SearchHistoryEntity(keyword = keyword))
        }

        override suspend fun deleteHistory(keyword: String) {
            searchHistoryDao.findHistory(keyword)?.let { searchHistoryDao.delete(it) }
        }

        override suspend fun clearAll() {
            searchHistoryDao.deleteAll()
        }
    }
