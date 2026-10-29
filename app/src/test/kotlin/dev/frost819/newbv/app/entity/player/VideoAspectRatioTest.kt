package dev.frost819.newbv.app.entity.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [VideoAspectRatio] 的单元测试。
 *
 * 验证枚举值、ratio 属性、缩放标记及 [VideoAspectRatio.fromOrdinal] 安全解析。
 */
class VideoAspectRatioTest {
    @Test
    fun `Default has null ratio and false flags`() {
        assertThat(VideoAspectRatio.Default.ratio).isNull()
        assertThat(VideoAspectRatio.Default.isCrop).isFalse()
        assertThat(VideoAspectRatio.Default.isStretch).isFalse()
        assertThat(VideoAspectRatio.Default.displayName).isEqualTo("默认（原始）")
    }

    @Test
    fun `Crop has null ratio and isCrop true`() {
        assertThat(VideoAspectRatio.Crop.ratio).isNull()
        assertThat(VideoAspectRatio.Crop.isCrop).isTrue()
        assertThat(VideoAspectRatio.Crop.isStretch).isFalse()
        assertThat(VideoAspectRatio.Crop.displayName).isEqualTo("去黑边（裁剪）")
    }

    @Test
    fun `Stretch has null ratio and isStretch true`() {
        assertThat(VideoAspectRatio.Stretch.ratio).isNull()
        assertThat(VideoAspectRatio.Stretch.isCrop).isFalse()
        assertThat(VideoAspectRatio.Stretch.isStretch).isTrue()
        assertThat(VideoAspectRatio.Stretch.displayName).isEqualTo("铺满（拉伸）")
    }

    @Test
    fun `FourToThree has 4f divided by 3f ratio`() {
        assertThat(VideoAspectRatio.FourToThree.ratio).isEqualTo(4f / 3f)
    }

    @Test
    fun `SixteenToNine has 16f divided by 9f ratio`() {
        assertThat(VideoAspectRatio.SixteenToNine.ratio).isEqualTo(16f / 9f)
    }

    @Test
    fun `TwentyOneToNine has 21f divided by 9f ratio`() {
        assertThat(VideoAspectRatio.TwentyOneToNine.ratio).isEqualTo(21f / 9f)
    }

    @Test
    fun `entries contains exactly six values`() {
        assertThat(VideoAspectRatio.entries).hasSize(6)
    }

    @Test
    fun `fromOrdinal returns correct value for valid ordinals`() {
        assertThat(VideoAspectRatio.fromOrdinal(0)).isEqualTo(VideoAspectRatio.Default)
        assertThat(VideoAspectRatio.fromOrdinal(1)).isEqualTo(VideoAspectRatio.Crop)
        assertThat(VideoAspectRatio.fromOrdinal(2)).isEqualTo(VideoAspectRatio.Stretch)
        assertThat(VideoAspectRatio.fromOrdinal(3)).isEqualTo(VideoAspectRatio.SixteenToNine)
        assertThat(VideoAspectRatio.fromOrdinal(4)).isEqualTo(VideoAspectRatio.FourToThree)
        assertThat(VideoAspectRatio.fromOrdinal(5)).isEqualTo(VideoAspectRatio.TwentyOneToNine)
    }

    @Test
    fun `fromOrdinal returns Default for negative ordinal`() {
        assertThat(VideoAspectRatio.fromOrdinal(-1)).isEqualTo(VideoAspectRatio.Default)
    }

    @Test
    fun `fromOrdinal returns Default for out-of-bounds ordinal`() {
        assertThat(VideoAspectRatio.fromOrdinal(99)).isEqualTo(VideoAspectRatio.Default)
    }

    @Test
    fun `FourToThree ratio is approximately 1_333`() {
        val ratio = VideoAspectRatio.FourToThree.ratio!!
        assertThat(ratio).isWithin(0.001f).of(1.3333f)
    }

    @Test
    fun `SixteenToNine ratio is approximately 1_778`() {
        val ratio = VideoAspectRatio.SixteenToNine.ratio!!
        assertThat(ratio).isWithin(0.001f).of(1.7778f)
    }
}
