package dev.frost819.newbv.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography as CommonTypography
import androidx.tv.material3.Typography as TvTypography

/**
 * 字体排版定义。
 *
 * 遵循 10-foot 远距电视观看标准：
 * - 核心标题（titleSmall/Medium/Large）：采用 SemiBold (600) ~ Bold (700)，增强轮廓清晰度；
 * - 核心元数据（bodySmall/Medium、labelSmall/Medium/Large）：采用 Medium (500)，彻底解决大屏远距离发虚问题；
 * - 提供 [tabular] 与 [timecode] 等宽数字特性（Tabular Figures `tnum`），消除时间码与数字跳动时的视觉抖动。
 */
object BVTypography {
    /** TV Material3 Typography（专为 TV 10-foot 大屏视觉强化优化）。 */
    val tv: TvTypography =
        TvTypography(
            displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
            displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Bold),
            displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
            headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
            headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )

    /** 普通 Material3 Typography（用于非 TV 组件）。 */
    val common: CommonTypography =
        CommonTypography(
            displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
            displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Bold),
            displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
            headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
            headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )

    /** 专用于播放器时间码、进度提示的等宽数字样式。 */
    val timecode: TextStyle =
        TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = "tnum",
            letterSpacing = 0.5.sp,
        )

    /**
     * 将给定的文本样式赋予等宽数字特性 (Tabular Figures, tnum)。
     *
     * 消除时间码、倒计时、播放量等数字跳动时的水平晃动。
     */
    fun tabular(baseStyle: TextStyle): TextStyle =
        baseStyle.copy(
            fontFeatureSettings = "tnum",
        )
}
