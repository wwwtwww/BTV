package dev.frost819.newbv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.dao.SearchHistoryDao
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Date

/**
 * [SearchHistoryRepositoryImpl] 的单元测试。
 *
 * 使用 MockK 模拟 [SearchHistoryDao]，验证搜索历史的增删查逻辑，
 * 包括无痕模式（incognitoMode）下不记录历史的行为。
 */
class SearchHistoryRepositoryImplTest {
    private lateinit var repository: SearchHistoryRepositoryImpl
    private lateinit var searchHistoryDao: SearchHistoryDao
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir =
            kotlin.io.path
                .createTempDirectory(prefix = "repo_test")
                .toFile()
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { File(tempDir, "Test.preferences_pb") },
            )
        Prefs.resetForTesting()
        Prefs.init(dataStore)

        searchHistoryDao = mockk(relaxed = true)
        repository = SearchHistoryRepositoryImpl(searchHistoryDao)
    }

    @AfterEach
    fun tearDown() {
        runBlocking { delay(200) }
        Prefs.resetForTesting()
        tempDir.deleteRecursively()
    }

    // ===== getHistories =====

    @Test
    fun `getHistories delegates to dao with count`() =
        runTest {
            val histories =
                listOf(
                    SearchHistoryEntity(id = 1, keyword = "test1"),
                    SearchHistoryEntity(id = 2, keyword = "test2"),
                )
            coEvery { searchHistoryDao.getHistories(10) } returns histories

            val result = repository.getHistories(10)

            assertThat(result).hasSize(2)
            assertThat(result[0].keyword).isEqualTo("test1")
            assertThat(result[1].keyword).isEqualTo("test2")
        }

    @Test
    fun `getHistories returns empty list when dao returns empty`() =
        runTest {
            coEvery { searchHistoryDao.getHistories(5) } returns emptyList()

            val result = repository.getHistories(5)

            assertThat(result).isEmpty()
        }

    @Test
    fun `getHistories with zero count returns empty list`() =
        runTest {
            coEvery { searchHistoryDao.getHistories(0) } returns emptyList()

            val result = repository.getHistories(0)

            assertThat(result).isEmpty()
        }

    // ===== addHistory =====

    @Test
    fun `addHistory upserts keyword atomically without find`() =
        runTest {
            // 新实现不再 find→insert/update 双步，直接单条 upsert 消除 TOCTOU
            repository.addHistory("new keyword")

            coVerify(exactly = 1) { searchHistoryDao.upsert(any()) }
            coVerify(exactly = 0) { searchHistoryDao.findHistory(any()) }
            coVerify(exactly = 0) { searchHistoryDao.insert(any()) }
            coVerify(exactly = 0) { searchHistoryDao.update(any()) }
        }

    @Test
    fun `addHistory passes entity with matching keyword and current searchDate`() =
        runTest {
            val before = Date().time

            repository.addHistory("hello")

            coVerify {
                searchHistoryDao.upsert(
                    match { it.keyword == "hello" && it.searchDate.time >= before },
                )
            }
        }

    @Test
    fun `addHistory does nothing when incognito mode is on`() =
        runTest {
            Prefs.incognitoMode = true

            repository.addHistory("any keyword")

            coVerify(exactly = 0) { searchHistoryDao.upsert(any()) }
            coVerify(exactly = 0) { searchHistoryDao.findHistory(any()) }
            coVerify(exactly = 0) { searchHistoryDao.insert(any()) }
            coVerify(exactly = 0) { searchHistoryDao.update(any()) }
        }

    @Test
    fun `addHistory with empty keyword still upserts`() =
        runTest {
            repository.addHistory("")

            coVerify(exactly = 1) {
                searchHistoryDao.upsert(match { it.keyword == "" })
            }
        }

    // ===== deleteHistory =====

    @Test
    fun `deleteHistory deletes when keyword exists`() =
        runTest {
            val existing = SearchHistoryEntity(id = 1, keyword = "test")
            coEvery { searchHistoryDao.findHistory("test") } returns existing

            repository.deleteHistory("test")

            coVerify(exactly = 1) { searchHistoryDao.delete(existing) }
        }

    @Test
    fun `deleteHistory does nothing when keyword not found`() =
        runTest {
            coEvery { searchHistoryDao.findHistory("notfound") } returns null

            repository.deleteHistory("notfound")

            coVerify(exactly = 0) {
                searchHistoryDao.delete(any<SearchHistoryEntity>())
            }
        }

    // ===== clearAll =====

    @Test
    fun `clearAll delegates to dao`() =
        runTest {
            repository.clearAll()

            coVerify(exactly = 1) { searchHistoryDao.deleteAll() }
        }
}
