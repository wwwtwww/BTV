package dev.frost819.newbv.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** 用于注入 [Dispatchers.IO] 的 Hilt 限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** 用于注入 [Dispatchers.Main] 的 Hilt 限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** 用于注入与应用生命周期绑定的 [CoroutineScope] 的 Hilt 限定符。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * 协程调度器与全局作用域 Hilt 依赖注入模块。
 *
 * 为生产环境提供标准的 [Dispatchers.IO]、[Dispatchers.Main] 与应用级 [ApplicationScope] 作用域，
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

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
}
