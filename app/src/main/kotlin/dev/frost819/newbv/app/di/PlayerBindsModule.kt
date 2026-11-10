package dev.frost819.newbv.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.frost819.newbv.app.util.MediaCodecVideoCapabilityProvider
import dev.frost819.newbv.app.util.VideoCapabilityProvider
import javax.inject.Singleton

/**
 * 播放器层接口绑定模块。
 *
 * 使用 `@Binds` 替代 `@Provides`，让 Dagger 直接生成方法调用而非工厂创建，
 * 性能更优且符合 Dagger 最佳实践。
 *
 * 注：`CdnSelector` 的实现 `OkHttpCdnSelector` 位于 `player` 模块，
 * 构造需要自定义 SSL 的 `OkHttpClient`，无法使用 `@Inject constructor`，
 * 因此仍通过 [PlayerModule] 中的 `@Provides` 提供。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerBindsModule {

    /** 绑定 [VideoCapabilityProvider] 的实现。 */
    @Binds
    @Singleton
    abstract fun bindVideoCapabilityProvider(impl: MediaCodecVideoCapabilityProvider): VideoCapabilityProvider
}
