package dev.frost819.newbv.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.frost819.newbv.app.data.AccountRepositoryImpl
import dev.frost819.newbv.data.repository.AccountRepository
import dev.frost819.newbv.data.repository.SearchHistoryRepository
import dev.frost819.newbv.data.repository.SearchHistoryRepositoryImpl
import javax.inject.Singleton

/**
 * Repository 层接口绑定模块。
 *
 * 使用 `@Binds` 替代 `@Provides`，让 Dagger 直接生成方法调用而非工厂创建，
 * 性能更优且符合 Dagger 最佳实践。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindsModule {

    /** 绑定 [AccountRepository] 的实现。 */
    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    /** 绑定 [SearchHistoryRepository] 的实现。 */
    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository
}
