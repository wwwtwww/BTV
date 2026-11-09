package dev.frost819.newbv.player.impl.exo

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.player.VideoPlayerOptions
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [ExoMediaPlayer.reset] 的单元测试。
 *
 * 通过 [TestableExoMediaPlayer] 子类覆写工厂方法，
 * 避免在 JVM 测试中触发 Android 框架依赖（Context.applicationContext、OkHttpUtil 等）。
 * 使用反射设置 [mPlayer] 以注入 Mock ExoPlayer 实例。
 *
 * 测试覆盖：
 * - reset() 调用 stop() 和 clearMediaItems()
 * - reset() 将 mMediaSource 置为 null
 * - mPlayer 为 null 时 reset() 安全（不崩溃）
 * - reset() 可安全重复调用
 */
class ExoMediaPlayerTest {
    /**
     * 可测试的 ExoMediaPlayer 子类。
     *
     * 覆写 [initPlayer] 为空操作（避免创建真实 ExoPlayer），
     * 覆写数据源工厂方法返回 Mock（避免 Android 框架调用）。
     */
    private class TestableExoMediaPlayer(
        context: Context,
        options: VideoPlayerOptions,
    ) : ExoMediaPlayer(context, options) {
        override fun initPlayer() {
            // 不创建真实 ExoPlayer，由测试通过反射注入 mock
        }

        override fun createHttpDataSourceFactory(): OkHttpDataSource.Factory {
            // 不应在 reset 测试中被调用
            throw UnsupportedOperationException("Test stub: createHttpDataSourceFactory")
        }

        override fun createDataSourceFactory(httpFactory: OkHttpDataSource.Factory): DefaultDataSource.Factory {
            // 不应在 reset 测试中被调用
            throw UnsupportedOperationException("Test stub: createDataSourceFactory")
        }

        /** 暴露 mPlayer 供测试通过反射注入后验证 */
        fun playerForTest(): ExoPlayer? = mPlayer

        /** 暴露 mMediaSource 供测试验证 */
        fun mediaSourceForTest(): MediaSource? = mMediaSource
    }

    private lateinit var mockContext: Context
    private lateinit var options: VideoPlayerOptions
    private lateinit var mediaPlayer: TestableExoMediaPlayer

    @BeforeEach
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        options = VideoPlayerOptions()
        mediaPlayer = TestableExoMediaPlayer(mockContext, options)
    }

    @Test
    fun `reset stops player and clears media items`() {
        // Given
        val mockPlayer = mockk<ExoPlayer>(relaxed = true)
        val mockSource = mockk<MediaSource>(relaxed = true)
        setPlayerViaReflection(mockPlayer)
        setMediaSourceViaReflection(mockSource)

        // When
        mediaPlayer.reset()

        // Then
        verify { mockPlayer.stop() }
        verify { mockPlayer.clearMediaItems() }
        assertThat(mediaPlayer.mediaSourceForTest()).isNull()
    }

    @Test
    fun `reset does not release player instance`() {
        // Given
        val mockPlayer = mockk<ExoPlayer>(relaxed = true)
        setPlayerViaReflection(mockPlayer)

        // When
        mediaPlayer.reset()

        // Then — player 实例仍保留（未调用 release），reset 后可继续使用
        assertThat(mediaPlayer.playerForTest()).isSameInstanceAs(mockPlayer)
        verify(exactly = 0) { mockPlayer.release() }
    }

    @Test
    fun `reset with null player does not throw`() {
        // Given — mPlayer 为 null（TestableExoMediaPlayer.initPlayer 为空操作）
        assertThat(mediaPlayer.playerForTest()).isNull()

        // When & Then — 不崩溃
        mediaPlayer.reset()
    }

    @Test
    fun `reset with null mediaSource only stops player`() {
        // Given
        val mockPlayer = mockk<ExoPlayer>(relaxed = true)
        setPlayerViaReflection(mockPlayer)
        // mMediaSource 默认为 null

        // When
        mediaPlayer.reset()

        // Then
        verify { mockPlayer.stop() }
        verify { mockPlayer.clearMediaItems() }
        assertThat(mediaPlayer.mediaSourceForTest()).isNull()
    }

    @Test
    fun `reset can be called multiple times safely`() {
        // Given
        val mockPlayer = mockk<ExoPlayer>(relaxed = true)
        val mockSource = mockk<MediaSource>(relaxed = true)
        setPlayerViaReflection(mockPlayer)
        setMediaSourceViaReflection(mockSource)

        // When — 连续调用两次 reset
        mediaPlayer.reset()
        mediaPlayer.reset()

        // Then — stop 和 clearMediaItems 各被调用两次，不崩溃
        verify(exactly = 2) { mockPlayer.stop() }
        verify(exactly = 2) { mockPlayer.clearMediaItems() }
        assertThat(mediaPlayer.mediaSourceForTest()).isNull()
    }

    // ------------------------------------------------------------------
    //  Reflection helpers
    // ------------------------------------------------------------------

    /**
     * 通过反射设置 [ExoMediaPlayer.mPlayer]（private set 属性）。
     */
    private fun setPlayerViaReflection(player: ExoPlayer) {
        val field = ExoMediaPlayer::class.java.getDeclaredField("mPlayer")
        field.isAccessible = true
        field.set(mediaPlayer, player)
    }

    /**
     * 通过反射设置 [ExoMediaPlayer.mMediaSource]（protected 属性）。
     */
    private fun setMediaSourceViaReflection(source: MediaSource?) {
        val field = ExoMediaPlayer::class.java.getDeclaredField("mMediaSource")
        field.isAccessible = true
        field.set(mediaPlayer, source)
    }
}
