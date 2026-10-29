package dev.frost819.newbv.app.entity.player

/**
 * 视频宽高比模式。
 *
 * 控制播放器画面的缩放与裁剪方式。
 *
 * @property ratio 宽高比数值，为 null 时根据 [isCrop] 或 [isStretch] 自适应
 * @property isCrop 是否为智能裁剪去黑边模式（Zoom，等比居中放大填满 16:9，裁切 21:9 等画面的黑边）
 * @property isStretch 是否为拉伸铺满模式（Fill，强制填满 16:9 屏幕）
 * @property displayName 模式显示名称
 */
enum class VideoAspectRatio(
    val ratio: Float?,
    val isCrop: Boolean = false,
    val isStretch: Boolean = false,
    val displayName: String,
) {
    /** 默认（使用视频原始宽高比，等比适应 Fit）。 */
    Default(ratio = null, displayName = "默认（原始）"),

    /** 智能裁剪去黑边（等比居中放大填满屏幕 Zoom，裁切边缘黑边）。 */
    Crop(ratio = null, isCrop = true, displayName = "去黑边（裁剪）"),

    /** 拉伸铺满（强制拉伸填满屏幕 Fill）。 */
    Stretch(ratio = null, isStretch = true, displayName = "铺满（拉伸）"),

    /** 16:9 标准宽屏。 */
    SixteenToNine(ratio = 16f / 9f, displayName = "16:9"),

    /** 4:3 传统比例。 */
    FourToThree(ratio = 4f / 3f, displayName = "4:3"),

    /** 21:9 宽银幕电影比例。 */
    TwentyOneToNine(ratio = 21f / 9f, displayName = "21:9"),
    ;

    companion object {
        /** 从序号安全解析，越界返回 [Default]。 */
        fun fromOrdinal(ordinal: Int): VideoAspectRatio = entries.getOrElse(ordinal) { Default }
    }
}
