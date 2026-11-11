package dev.frost819.newbv.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5 迁移。
 *
 * FIX-17 为 user 表和 search_history 表添加了唯一索引。
 * 迁移前需要先清理重复数据（保留每组中 id 最大的记录），
 * 否则创建唯一索引会因数据冲突而失败。
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. user 表去重：保留每个 uid 中 id 最大的记录
            db.execSQL(
                """
                DELETE FROM user WHERE id NOT IN (
                    SELECT MAX(id) FROM user GROUP BY uid
                )
                """.trimIndent(),
            )
            // 2. search_history 表去重：保留每个 keyword 中 id 最大的记录
            db.execSQL(
                """
                DELETE FROM search_history WHERE id NOT IN (
                    SELECT MAX(id) FROM search_history GROUP BY keyword
                )
                """.trimIndent(),
            )
            // 3. 创建唯一索引
            db.execSQL("CREATE UNIQUE INDEX index_user_uid ON user(uid)")
            db.execSQL("CREATE UNIQUE INDEX index_search_history_keyword ON search_history(keyword)")
        }
    }
