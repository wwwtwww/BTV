package dev.frost819.newbv.core.theme

import androidx.compose.ui.graphics.Color

/**
 * new BV 品牌色板。
 *
 * 以低饱和蓝紫与青绿色为核心，延伸出辅助色与中性色。
 * 所有颜色以 `Color` 表示，供 [BVTheme] 构建 colorScheme。
 */
object BVColors {
    /** 低饱和蓝紫，作为 new BV 的主品牌色。 */
    val Primary = Color(0xFF7773AD)

    /** 深色背景下使用的主色变体。 */
    val PrimaryStrong = Color(0xFF5E5A8B)

    /** 浅色背景下使用的主色变体。 */
    val PrimaryLight = Color(0xFF9995CF)

    /** 低饱和青绿，用于辅助操作和信息状态。 */
    val Secondary = Color(0xFF5B9B94)
    val SecondaryLight = Color(0xFF79B8AE)

    /** 浅色背景下使用的中性绿色变体，作为强调色文字/图标时保证对比度。 */
    val SecondaryStrong = Color(0xFF3F7A73)

    /** 黄色，用于投币/警告。 */
    val Yellow = Color(0xFFC49B5C)

    /** 绿色，用于成功状态。 */
    val Green = Color(0xFF70A684)

    /** 红色，用于错误/直播。 */
    val Red = Color(0xFFC87878)

    /** 深色主题中性色（严格遵循 10-foot 大屏 ≥ 4.5:1 高对比度规范，OLED 深度幽蓝黑底色）。 */
    val DarkBackground = Color(0xFF0C101A)
    val DarkSurface = Color(0xFF141926)
    val DarkSurfaceVariant = Color(0xFF1E2538)
    val DarkOnBackground = Color(0xFFFFFFFF)
    val DarkOnSurface = Color(0xFFFFFFFF)
    val DarkOnSurfaceVariant = Color(0xFFC0C6D8)
    val DarkBorder = PrimaryLight

    /** 浅色主题中性色（雅致冷瓷白，兼顾高对比度与防过曝刺眼）。 */
    val LightBackground = Color(0xFFF4F6FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE8ECF5)
    val LightOnBackground = Color(0xFF111827)
    val LightOnSurface = Color(0xFF111827)
    val LightOnSurfaceVariant = Color(0xFF4B5563)
    val LightBorder = PrimaryStrong

    // --- 播放器视听专属高光微标色彩 (Quality & Audio Badges) ---

    /** 8K 超高清微标主色（青蓝高动态）。 */
    val Badge8kContent = Color(0xFF67E8F9)
    val Badge8kBackground = Color(0xFF1E293B)

    /** 4K 超高清微标主色（天蓝）。 */
    val Badge4kContent = Color(0xFF38BDF8)
    val Badge4kBackground = Color(0xFF1E293B)

    /** 杜比视界 (Dolby Vision) 金黄色。 */
    val BadgeDolbyVisionContent = Color(0xFFFDE047)
    val BadgeDolbyVisionBackground = Color(0xFF18181B)

    /** HDR 高动态范围暖金色。 */
    val BadgeHdrContent = Color(0xFFFDBA74)
    val BadgeHdrBackground = Color(0xFF18181B)

    /** 杜比全景声 (Dolby Atmos) 专属紫罗兰金标。 */
    val BadgeDolbyAtmosContent = Color(0xFFD8B4FE)
    val BadgeDolbyAtmosBackground = Color(0xFF2E1065)

    /** Hi-Res 无损音质小金标。 */
    val BadgeHiResContent = Color(0xFFFDE047)
    val BadgeHiResBackground = Color(0xFF78350F)
}
