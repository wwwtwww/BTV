package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [VideoShot.getSpriteFrame] 和 [VideoShotImageCache] 的单元测试。
 *
 * 验证空数据、除零边界、越界访问及异常图片解码时的防御性安全处理。
 */
@RunWith(RobolectricTestRunner::class)
class VideoShotExtendsTest {
    private val cache = VideoShotImageCache()

    @Test
    fun `getSpriteFrame returns null when times is empty`() =
        runTest {
            val videoShot =
                VideoShot(
                    times = emptyList(),
                    images = listOf(byteArrayOf(1, 2, 3)),
                    imageCountX = 10,
                    imageCountY = 10,
                    imageWidth = 160,
                    imageHeight = 90,
                )

            val frame = videoShot.getSpriteFrame(0, cache)
            assertThat(frame).isNull()
        }

    @Test
    fun `getSpriteFrame returns null when images is empty`() =
        runTest {
            val videoShot =
                VideoShot(
                    times = listOf(0.toUShort()),
                    images = emptyList(),
                    imageCountX = 10,
                    imageCountY = 10,
                    imageWidth = 160,
                    imageHeight = 90,
                )

            val frame = videoShot.getSpriteFrame(0, cache)
            assertThat(frame).isNull()
        }

    @Test
    fun `getSpriteFrame returns null when imageCountX is zero`() =
        runTest {
            val videoShot =
                VideoShot(
                    times = listOf(0.toUShort()),
                    images = listOf(byteArrayOf(1, 2, 3)),
                    imageCountX = 0,
                    imageCountY = 10,
                    imageWidth = 160,
                    imageHeight = 90,
                )

            val frame = videoShot.getSpriteFrame(0, cache)
            assertThat(frame).isNull()
        }

    @Test
    fun `getSpriteFrame returns null when imageCountY is negative`() =
        runTest {
            val videoShot =
                VideoShot(
                    times = listOf(0.toUShort()),
                    images = listOf(byteArrayOf(1, 2, 3)),
                    imageCountX = 10,
                    imageCountY = -1,
                    imageWidth = 160,
                    imageHeight = 90,
                )

            val frame = videoShot.getSpriteFrame(0, cache)
            assertThat(frame).isNull()
        }

    @Test
    fun `getOrDecodeImage returns null on invalid bytes without crashing`() =
        runTest {
            val invalidBytes = byteArrayOf(0x00, 0x11, 0x22, 0x33)
            val bitmap = cache.getOrDecodeImage(0, invalidBytes)
            assertThat(bitmap).isNull()
        }
}
