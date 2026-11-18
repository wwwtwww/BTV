package dev.frost819.newbv.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.frost819.newbv.data.db.AppDatabase
import dev.frost819.newbv.data.db.MIGRATION_4_5
import dev.frost819.newbv.data.db.dao.SearchHistoryDao
import dev.frost819.newbv.data.db.dao.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * 数据层 Hilt 模块。
 *
 * 提供 Room 数据库、DAO、DataStore 的单例绑定。
 * 所有绑定注册到 [SingletonComponent]，全应用生命周期共享。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * 提供 [AppDatabase] 单例。
     *
     * 使用应用上下文构建，数据库名由 [AppDatabase.DATABASE_NAME] 定义。
     * 不使用手动单例（区别于原版 BV），由 Hilt 管理生命周期。
     *
     * 迁移策略：只注册显式 [MIGRATION_4_5]，**不开启** `fallbackToDestructiveMigration()`。
     * 破坏式降级会静默清空账户/搜索历史（含用户锁），且让“漏写迁移”这类
     * 错误逃过开发/CI 阶段；缺失迁移路径时应直接抛 [IllegalStateException] 暴露问题。
     * 任何 Entity 结构变更都必须同步在 `Migrations.kt` 中补充 Migration 并在此注册。
     *
     * 移除 fallback 的前提条件：所有已发布版本的 DB schema 均 >= version 4，
     * 因此不存在更早版本的用户数据需要迁移，[MIGRATION_4_5] 是当前唯一迁移路径。
     * 若未来需要回退支持更早版本（如 v3 及以下），必须先补充完整迁移链
     * （MIGRATION_3_4 等）并在此注册，方可安全升级；否则老用户升级会直接抛
     * [IllegalStateException]（这优于静默清库，能在开发阶段立即暴露漏迁移问题）。
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_4_5)
            .build()

    /** 提供搜索历史 DAO。 */
    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

    /** 提供账户 DAO。 */
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    /**
     * 提供 DataStore 单例。
     *
     * 使用 [PreferenceDataStoreFactory] 创建，持久化文件名为 `Settings.preferences_pb`。
     * 协程作用域使用 IO 调度器 + SupervisorJob。
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { java.io.File(context.filesDir, "datastore/Settings.preferences_pb") },
        )
}
