package dev.frost819.newbv.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.db.dao.UserDao
import dev.frost819.newbv.data.db.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [UserDao] 的插桩测试。
 *
 * 在真实 SQLite 上验证 SQL 正确性：CRUD 操作。
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_and_getAll() =
        runTest {
            val user =
                UserEntity(
                    uid = 1001L,
                    username = "testUser",
                    avatar = "https://example.com/avatar.jpg",
                    auth = "{\"sessData\":\"abc\"}",
                )
            dao.insert(user)

            val result = dao.getAll()

            assertThat(result).hasSize(1)
            assertThat(result[0].uid).isEqualTo(1001L)
            assertThat(result[0].username).isEqualTo("testUser")
            assertThat(result[0].avatar).isEqualTo("https://example.com/avatar.jpg")
            assertThat(result[0].auth).isEqualTo("{\"sessData\":\"abc\"}")
        }

    @Test
    fun insert_multiple_users() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "user1", avatar = "url1", auth = "auth1"),
                UserEntity(uid = 2L, username = "user2", avatar = "url2", auth = "auth2"),
                UserEntity(uid = 3L, username = "user3", avatar = "url3", auth = "auth3"),
            )

            val result = dao.getAll()

            assertThat(result).hasSize(3)
            assertThat(result.map { it.uid }).containsExactly(1L, 2L, 3L)
        }

    @Test
    fun findUserByUid_returns_match() =
        runTest {
            dao.insert(UserEntity(uid = 12345L, username = "found", avatar = "url", auth = "auth"))

            val result = dao.findUserByUid(12345L)

            assertThat(result).isNotNull()
            assertThat(result!!.username).isEqualTo("found")
        }

    @Test
    fun findUserByUid_returns_null_when_not_found() =
        runTest {
            val result = dao.findUserByUid(99999L)

            assertThat(result).isNull()
        }

    @Test
    fun delete_removes_user() =
        runTest {
            val user = UserEntity(uid = 100L, username = "delete", avatar = "url", auth = "auth")
            dao.insert(user)
            val inserted = dao.getAll().first()

            dao.delete(inserted)

            assertThat(dao.getAll()).isEmpty()
        }

    @Test
    fun delete_specific_user_keeps_others() =
        runTest {
            dao.insert(
                UserEntity(uid = 1L, username = "keep", avatar = "url1", auth = "auth1"),
                UserEntity(uid = 2L, username = "delete", avatar = "url2", auth = "auth2"),
            )
            val users = dao.getAll()
            val toDelete = users.find { it.uid == 2L }!!

            dao.delete(toDelete)

            val remaining = dao.getAll()
            assertThat(remaining).hasSize(1)
            assertThat(remaining[0].uid).isEqualTo(1L)
        }

    @Test
    fun update_changes_username_and_avatar() =
        runTest {
            val user = UserEntity(uid = 100L, username = "oldName", avatar = "oldUrl", auth = "auth")
            dao.insert(user)
            var inserted = dao.getAll().first()

            inserted = inserted.copy(username = "newName", avatar = "newUrl")
            dao.update(inserted)

            val result = dao.findUserByUid(100L)
            assertThat(result!!.username).isEqualTo("newName")
            assertThat(result.avatar).isEqualTo("newUrl")
        }

    @Test
    fun update_changes_auth_and_lock() =
        runTest {
            val user = UserEntity(uid = 100L, username = "user", avatar = "url", auth = "oldAuth")
            dao.insert(user)
            var inserted = dao.getAll().first()

            inserted = inserted.copy(auth = "newAuth", lock = "1234")
            dao.update(inserted)

            val result = dao.findUserByUid(100L)
            assertThat(result!!.auth).isEqualTo("newAuth")
            assertThat(result.lock).isEqualTo("1234")
        }

    @Test
    fun default_lock_is_empty_string() =
        runTest {
            dao.insert(UserEntity(uid = 1L, username = "user", avatar = "url", auth = "auth"))

            val result = dao.findUserByUid(1L)
            assertThat(result!!.lock).isEmpty()
        }

    // ── upsertRemoteFields（Issue 1：lock 不被 REPLACE 清空回归）──────────────

    @Test
    fun upsertRemoteFields_preserves_lock_on_conflict() =
        runTest {
            // 先插入一个已设置用户锁的账户
            dao.insert(
                UserEntity(
                    uid = 100L,
                    username = "old",
                    avatar = "old_url",
                    auth = "old_auth",
                    lock = "1234",
                ),
            )

            // 模拟（并发）重新登录：只传远端字段，ON CONFLICT 不得触碰 lock
            dao.upsertRemoteFields(
                uid = 100L,
                username = "new",
                avatar = "new_url",
                auth = "new_auth",
            )

            val result = dao.findUserByUid(100L)
            assertThat(result).isNotNull()
            assertThat(result!!.username).isEqualTo("new")
            assertThat(result.avatar).isEqualTo("new_url")
            assertThat(result.auth).isEqualTo("new_auth")
            // 关键断言：lock 必须保留，绝不被静默清空
            assertThat(result.lock).isEqualTo("1234")
        }

    @Test
    fun upsertRemoteFields_inserts_new_user_with_empty_lock() =
        runTest {
            dao.upsertRemoteFields(uid = 500L, username = "fresh", avatar = "url", auth = "auth")

            val result = dao.findUserByUid(500L)
            assertThat(result).isNotNull()
            assertThat(result!!.uid).isEqualTo(500L)
            assertThat(result.username).isEqualTo("fresh")
            assertThat(result.lock).isEmpty()
        }

    @Test
    fun upsertRemoteFields_does_not_duplicate_row_on_conflict() =
        runTest {
            dao.insert(UserEntity(uid = 100L, username = "old", avatar = "old", auth = "old"))

            dao.upsertRemoteFields(uid = 100L, username = "new", avatar = "new", auth = "new")

            // uid 唯一索引命中，更新而非新增，表内仍只有一行
            val all = dao.getAll()
            assertThat(all).hasSize(1)
            assertThat(all[0].username).isEqualTo("new")
        }

    @Test
    fun getAll_on_empty_table_returns_empty() =
        runTest {
            val result = dao.getAll()

            assertThat(result).isEmpty()
        }
}
