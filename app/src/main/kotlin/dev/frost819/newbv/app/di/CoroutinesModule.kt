package dev.frost819.newbv.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** 用于注入 [Dispatchers.IO] 的 Hilt 限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** 用于注入 [Dispatchers.Main] 的 Hilt 限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * 协程调度器 Hilt 依赖注入模块。
 *
 * 为生产环境提供标准的 [Dispatchers.IO] 与 [Dispatchers.Main]，
 * 便于 ViewModel 与 Repository 注入受控调度器并在单元测试中使用 TestDispatcher。
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
