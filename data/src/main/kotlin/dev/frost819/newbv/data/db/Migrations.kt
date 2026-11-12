package dev.frost819.newbv.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5 迁移。
 *
 * FIX-17 为 user 表和 search_history 表添加了唯一索引。
 * 迁移前需要先清理重复数据（保留每组中 id 最大的记录），
 * 否则创建唯一索引会因数据冲突而失败。
 *
 * 事务说明：Room 默认会将整个 [migrate] 调用包裹在单个事务中执行
 * （未标注 @AutoReclaimTransaction 的迁移即走此默认行为），因此任一
 * 语句失败都会整体回滚，不会留下半迁移状态；本文件不再显式包事务。
 *
 * 幂等说明：索引创建使用 IF NOT EXISTS（SQLite 3.3.0+ 支持，Android
 * 内置 SQLite 远高于此，minSdk 21 对应 3.8.6+），若历史 DB 已存在
 * 同名索引则直接跳过，避免创建失败导致升级崩溃。
 *
 * 去重范围说明：去重对全表执行，不做 NULL / 空串过滤。唯一索引只按
 * 值是否相等判重，并不区分空串——多行相同空串同样会冲突；而 Room 的
 * [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigration]
 * 只在「找不到迁移路径」时生效，不会捕获 [migrate] 内部抛出的异常，
 * 因此漏删任意一类重复值都会让老用户升级直接崩溃。两组列在实体中均
 * 声明为非空（uid: Long / keyword: String），也不存在需要保留的 NULL 行。
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. user 表去重：每个 uid 只保留 id 最大的记录（全表，不过滤空值）。
            db.execSQL(
                """
                DELETE FROM user WHERE id NOT IN (
                    SELECT MAX(id) FROM user GROUP BY uid
                )
                """.trimIndent(),
            )
            // 2. search_history 表去重：每个 keyword 只保留 id 最大的记录。
            // 空串 keyword 必须一并去重：它同样会撞上唯一索引（索引不区分空串），
            // 若跳过则 CREATE UNIQUE INDEX 抛 SQLiteConstraintException 导致升级崩溃。
            db.execSQL(
                """
                DELETE FROM search_history WHERE id NOT IN (
                    SELECT MAX(id) FROM search_history GROUP BY keyword
                )
                """.trimIndent(),
            )
            // 3. 创建唯一索引：使用 IF NOT EXISTS，历史 DB 已有同名索引时不崩溃。
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_uid ON user(uid)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_search_history_keyword ON search_history(keyword)")
        }
    }
