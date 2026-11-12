package dev.frost819.newbv.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * [MIGRATION_4_5] 去重 + 唯一索引迁移的 SQL 语义回归测试。
 *
 * 背景 bug：迁移语句曾写成
 * `DELETE FROM search_history WHERE keyword IS NOT NULL AND keyword != '' AND id NOT IN (...)`，
 * 空串 keyword 被排除在去重之外。而 SQLite 唯一索引只按值相等判重、不区分空串，
 * 老用户库里多行空串会直接让 `CREATE UNIQUE INDEX` 抛 SQLException；Room 的
 * `fallbackToDestructiveMigration` 不捕获 migrate 内部异常，升级即崩溃。
 *
 * 测试策略：Room 迁移的端到端验证需要设备/模拟器（`RoomMigrationTest` 依赖
 * Instrumentation），此处改为 JVM 层执行真实 SQL —— mock [SupportSQLiteDatabase]，
 * 把 `execSQL` 转发到内存 SQLite（`org.xerial:sqlite-jdbc`），从而用迁移语句原文
 * 在真实 SQL 引擎上完整跑一遍，验证去重结果与索引创建。
 *
 * TODO(补 Robolectric/真机端到端)：在具备设备的环境用 `RoomMigrationTest` 跑
 *  v4.json → v5.json 的完整迁移路径（含 Room 身份校验与 schema 比对）。
 *
 * 建表语句取自 `data/schemas/.../4.json` 的 v4 schema（无唯一索引）。
 */
class Migration4To5Test {
    private lateinit var connection: Connection

    /** 迁移过程中实际执行的 SQL 语句，按顺序记录，供结构性断言使用。 */
    private val executedSql = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        Class.forName("org.sqlite.JDBC")
        // 每个连接独占一个内存库，测试之间零共享。
        connection = DriverManager.getConnection("jdbc:sqlite:")
        connection.setAutoCommit(true)
        execute(
            """
            CREATE TABLE user (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `uid` INTEGER NOT NULL,
                `username` TEXT NOT NULL,
                `avatar` TEXT NOT NULL,
                `auth` TEXT NOT NULL,
                `lock` TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent(),
        )
        execute(
            """
            CREATE TABLE search_history (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `keyword` TEXT NOT NULL,
                `search_date` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        executedSql.clear()
    }

    @AfterEach
    fun tearDown() {
        connection.close()
    }

    // ===== 场景 1：uid 重复 =====

    @Test
    fun `duplicate uid rows are deduped keeping max id and unique index is created`() {
        insertUser(1, uid = 100L)
        insertUser(2, uid = 100L)
        insertUser(3, uid = 100L)
        insertUser(4, uid = 200L)

        runMigration()

        assertThat(queryIds("user")).containsExactly(3L, 4L).inOrder()
        assertUniqueIndexExists("user", "index_user_uid", "uid")
    }

    @Test
    fun `unique user index rejects duplicate uid after migration`() {
        insertUser(1, uid = 100L)
        insertUser(2, uid = 100L)
        runMigration()

        val result = runCatching { insertUser(9, uid = 100L) }
        assertWithMessage("迁移后重复 uid 应被唯一索引拒绝")
            .that(result.isFailure)
            .isTrue()

        // 不同 uid 仍可写入。
        runCatching { insertUser(10, uid = 777L) }.getOrThrow()
        assertThat(queryIds("user")).containsExactly(2L, 10L).inOrder()
    }

    // ===== 场景 2：keyword 重复（含空串） =====

    @Test
    fun `duplicate keyword rows including empty string are deduped and unique index is created`() {
        // 空串重复是最关键的回归点：若去重漏掉空串，CREATE UNIQUE INDEX 会抛异常。
        insertHistory(1, "tv")
        insertHistory(2, "tv")
        insertHistory(3, "")
        insertHistory(4, "")
        insertHistory(5, "anime")

        runMigration()

        assertThat(queryIds("search_history")).containsExactly(2L, 4L, 5L).inOrder()
        assertThat(keywords()).containsExactly("tv", "", "anime").inOrder()
        assertUniqueIndexExists("search_history", "index_search_history_keyword", "keyword")
    }

    @Test
    fun `empty keyword duplicates alone do not crash the migration`() {
        // 只留空串重复：这是历史上导致升级崩溃的最小场景。
        insertHistory(1, "")
        insertHistory(2, "")
        insertHistory(3, "")

        runMigration()

        assertThat(queryIds("search_history")).containsExactly(3L)
        assertUniqueIndexExists("search_history", "index_search_history_keyword", "keyword")
    }

    @Test
    fun `unique search history index rejects duplicate empty keyword after migration`() {
        insertHistory(1, "")
        runMigration()

        val result = runCatching { insertHistory(9, "") }
        assertWithMessage("迁移后重复空串 keyword 应被唯一索引拒绝")
            .that(result.isFailure)
            .isTrue()
    }

    // ===== 场景 3：正常不重复数据保持不变 =====

    @Test
    fun `rows without duplicates are preserved unchanged`() {
        insertUser(1, uid = 1L)
        insertUser(2, uid = 2L)
        insertUser(3, uid = 3L)
        insertHistory(1, "a")
        insertHistory(2, "b")
        insertHistory(3, "c")

        runMigration()

        assertThat(queryIds("user")).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(userUids()).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(queryIds("search_history")).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(keywords()).containsExactly("a", "b", "c").inOrder()
        assertUniqueIndexExists("user", "index_user_uid", "uid")
        assertUniqueIndexExists("search_history", "index_search_history_keyword", "keyword")
    }

    @Test
    fun `empty tables migrate successfully`() {
        runMigration()

        assertThat(queryIds("user")).isEmpty()
        assertThat(queryIds("search_history")).isEmpty()
        assertUniqueIndexExists("user", "index_user_uid", "uid")
        assertUniqueIndexExists("search_history", "index_search_history_keyword", "keyword")
    }

    @Test
    fun `migration can run again on already indexed database`() {
        insertUser(1, uid = 100L)
        insertUser(2, uid = 100L)
        insertHistory(1, "")
        insertHistory(2, "")

        runMigration()
        // 第二次执行（IF NOT EXISTS）不应因索引已存在而失败。
        runMigration()

        assertThat(queryIds("user")).containsExactly(2L)
        assertThat(queryIds("search_history")).containsExactly(2L)
    }

    // ===== 语句结构防护（直接锁定被修复的 bug 形态） =====

    @Test
    fun `dedup statements do not filter out null or empty values`() {
        insertUser(1, uid = 100L)
        insertHistory(1, "")
        runMigration()

        val dedupStatements = executedSql.filter { it.trimStart().startsWith("DELETE FROM", ignoreCase = true) }
        assertWithMessage("应包含 user 与 search_history 两条去重语句")
            .that(dedupStatements)
            .hasSize(2)

        for (sql in dedupStatements) {
            val normalized = sql.replace('\n', ' ').lowercase()
            assertWithMessage("去重语句不应带 NULL 过滤，否则漏删重复行会撞唯一索引：$sql")
                .that(normalized.contains("is not null"))
                .isFalse()
            assertWithMessage("去重语句不应排除空串，否则空串重复会让 CREATE UNIQUE INDEX 崩溃：$sql")
                .that(normalized.contains("!= ''") || normalized.contains("<> ''"))
                .isFalse()
            assertWithMessage("去重应保留每组 MAX(id)：$sql")
                .that(normalized.contains("max(id)") && normalized.contains("group by"))
                .isTrue()
        }

        val userDedup = dedupStatements.first { it.contains("user") }
        val historyDedup = dedupStatements.first { it.contains("search_history") }
        assertWithMessage("user 去重应按 uid 分组：$userDedup")
            .that(userDedup.contains("GROUP BY uid"))
            .isTrue()
        assertWithMessage("search_history 去重应按 keyword 分组：$historyDedup")
            .that(historyDedup.contains("GROUP BY keyword"))
            .isTrue()
    }

    // ===== 辅助函数 =====

    /**
     * 执行 [MIGRATION_4_5].migrate，将 [SupportSQLiteDatabase.execSQL] 转发到内存 SQLite。
     *
     * 迁移内部抛出的任何 SQLException（例如唯一索引冲突）会原样传播，使用例失败。
     */
    private fun runMigration() {
        val database = mockk<SupportSQLiteDatabase>()
        every { database.execSQL(any<String>()) } answers {
            val sql = firstArg<String>()
            executedSql += sql
            connection.createStatement().use { statement -> statement.executeUpdate(sql) }
        }
        MIGRATION_4_5.migrate(database)
    }

    /** 直接执行 SQL（建表/写入辅助）。 */
    private fun execute(sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }

    /** 插入一行 user。 */
    private fun insertUser(
        id: Long,
        uid: Long,
        username: String = "user-$uid",
    ) {
        execute(
            "INSERT INTO user (id, uid, username, avatar, auth, lock) " +
                "VALUES ($id, $uid, '$username', 'avatar', 'auth', '')",
        )
    }

    /** 插入一行 search_history。 */
    private fun insertHistory(
        id: Long,
        keyword: String,
    ) {
        // keyword 需原样写入（含空串），故不做任何转义：测试数据仅允许字母数字与空串。
        execute("INSERT INTO search_history (id, keyword, search_date) VALUES ($id, '$keyword', $id)")
    }

    /** 查询表内全部 id（按 id 升序）。 */
    private fun queryIds(table: String): List<Long> = querySingleColumn(table, "id").map { (it as Number).toLong() }

    /** 查询 user.uid 列表（按 id 升序）。 */
    private fun userUids(): List<Long> = querySingleColumn("user", "uid").map { (it as Number).toLong() }

    /** 查询 search_history.keyword 列表（按 id 升序）。 */
    private fun keywords(): List<String> = querySingleColumn("search_history", "keyword").map { it as String }

    /** 按 id 升序读取单列，返回原始值列表。 */
    private fun querySingleColumn(
        table: String,
        column: String,
    ): List<Any?> {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT $column FROM $table ORDER BY id ASC").use { resultSet ->
                val values = mutableListOf<Any?>()
                while (resultSet.next()) {
                    values += resultSet.getObject(1)
                }
                return values
            }
        }
    }

    /**
     * 断言指定表上存在目标唯一索引，且索引列正确。
     *
     * @param table 表名。
     * @param indexName 期望索引名。
     * @param column 期望被索引的列。
     */
    private fun assertUniqueIndexExists(
        table: String,
        indexName: String,
        column: String,
    ) {
        val indexRows = mutableListOf<Map<String, Any?>>()
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA index_list('$table')").use { resultSet ->
                val metadata = resultSet.metaData
                while (resultSet.next()) {
                    indexRows +=
                        (1..metadata.columnCount).associate { i -> metadata.getColumnName(i) to resultSet.getObject(i) }
                }
            }
        }

        val target = indexRows.firstOrNull { it["name"] == indexName }
        assertWithMessage("$table 上应存在唯一索引 $indexName，实际为 $indexRows")
            .that(target)
            .isNotNull()
        // PRAGMA index_list 的 unique 列：1 表示唯一索引。
        assertWithMessage("索引 $indexName 应为唯一索引")
            .that((target!!["unique"] as Number).toInt())
            .isEqualTo(1)

        val indexedColumns = mutableListOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA index_info('$indexName')").use { resultSet ->
                while (resultSet.next()) {
                    indexedColumns += resultSet.getString("name")
                }
            }
        }
        assertWithMessage("索引 $indexName 应建在 $column 列上")
            .that(indexedColumns)
            .containsExactly(column)
    }
}
