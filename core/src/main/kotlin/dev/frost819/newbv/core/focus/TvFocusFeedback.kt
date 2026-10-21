package dev.frost819.newbv.core.focus

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import dev.frost819.newbv.core.interaction.InputMethod
import dev.frost819.newbv.core.interaction.LocalInteractionTracker
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape

/**
 * 电视端三维立体焦点体系辅助配置与修饰符。
 *
 * 遵循 10-foot 电视体验规范（参考 YouTube TV 与 Apple TV tvOS 17）：
 * 1. 物理放大（Scale 1.06x）：获焦元素平滑浮起，邻近卡片产生微空间感；
 * 2. 双主题氛围层：
 *    - 深色模式（Dark）：在卡片后方投射青紫/电光青极光外发光辉光（Ambient Glow）；
 *    - 浅色模式（Light）：投射柔和悬浮投影（Drop Shadow / Elevation）增强对比度；
 * 3. 高光轮廓边框：深浅双主题自适应高对比边框。
 */
object TvFocusDefaults {
    /** 电视端卡片推荐聚焦放大比例。 */
    const val FOCUSED_SCALE = 1.06f

    /** 边框线宽。 */
    val BorderWidth: Dp = 2.5.dp

    /** 极光发光扩散范围。 */
    val GlowRadius: Dp = 8.dp

    /**
     * 构建卡片自适应边框（深色高亮微光，浅色深色对比边）。
     */
    @Composable
    fun cardBorder(shape: Shape = BVShape.large): CardBorder {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val borderColor = if (isDark) {
            MaterialTheme.colorScheme.border
        } else {
            BVColors.PrimaryStrong
        }
        return CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(BorderWidth, borderColor),
                shape = shape,
            ),
        )
    }

    /**
     * 构建 Surface 自适应边框。
     */
    @Composable
    fun surfaceBorder(shape: Shape = BVShape.large): ClickableSurfaceBorder {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val borderColor = if (isDark) {
            MaterialTheme.colorScheme.border
        } else {
            BVColors.PrimaryStrong
        }
        return ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(BorderWidth, borderColor),
                shape = shape,
            ),
        )
    }

    /**
     * 构建 TV Card 推荐缩放配置（1.06x 放大）。
     */
    @Composable
    fun cardScale(focusedScale: Float = FOCUSED_SCALE): CardScale =
        CardDefaults.scale(focusedScale = focusedScale)

    /**
     * 构建 TV ClickableSurface 推荐缩放配置（1.06x 放大）。
     */
    @Composable
    fun surfaceScale(focusedScale: Float = FOCUSED_SCALE): ClickableSurfaceScale =
        ClickableSurfaceDefaults.scale(focusedScale = focusedScale)
}

/**
 * 电视端通用立体焦点反馈修饰符（实验性 / 扩展动效封装）。
 *
 * 注：当前卡片等生产组件（如 SmallVideoCard、SeasonCard、LiveRoomCard）优先使用
 * [TvFocusDefaults.cardScale] 与 [TvFocusDefaults.cardBorder] 实现 1.06x 物理放大与双主题自适应边框。
 * 本修饰符额外提供了深色极光发光（Glow）与浅色悬浮投影（Drop Shadow）扩展绘制能力，
 * 暂作底层能力储备，待后续大屏容器重构消除外溢裁剪后再统一评估接入。
 *
 * 自动感知遥控器 D-Pad 获焦状态，并根据当前深浅主题自适应应用：
 * - 物理缩放（默认 [focusedScale] = 1.06f）
 * - 深色模式极光呼吸发光（Glow）
 * - 浅色模式环境悬浮投影（Drop Shadow）
 * - 高光/加粗边框
 *
 * @param shape 组件形状（默认 [BVShape.large]）
 * @param focusedScale 获焦缩放比例（默认 1.06f）
 * @param glowColor 深色模式极光辉光主色（默认取低饱和电光青 [BVColors.SecondaryLight]）
 * @param borderWidth 边框宽度（默认 2.5dp）
 */
fun Modifier.tvFocusFeedback(
    shape: Shape = BVShape.large,
    focusedScale: Float = TvFocusDefaults.FOCUSED_SCALE,
    glowColor: Color? = null,
    borderWidth: Dp = TvFocusDefaults.BorderWidth,
): Modifier =
    composed {
        val tracker = LocalInteractionTracker.current
        var hasFocus by remember { mutableStateOf(false) }

        val showFeedback =
            if (tracker != null) {
                val method by tracker.inputMethod.collectAsState()
                hasFocus && method == InputMethod.DPad
            } else {
                hasFocus
            }

        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val activeGlowColor = glowColor ?: BVColors.SecondaryLight
        val activeBorderColor = if (isDark) {
            MaterialTheme.colorScheme.border
        } else {
            BVColors.PrimaryStrong
        }

        // 平滑弹性缩放动画
        val scale by animateFloatAsState(
            targetValue = if (showFeedback) focusedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "tv-focus-scale",
        )

        // 发光透明度动画
        val glowAlpha by animateFloatAsState(
            targetValue = if (showFeedback && isDark) 0.35f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "tv-focus-glow-alpha",
        )

        val baseModifier = this
            .onFocusChanged { hasFocus = it.hasFocus }
            .scale(scale)

        val withShadow = if (!isDark && showFeedback) {
            baseModifier.shadow(elevation = 8.dp, shape = shape)
        } else {
            baseModifier
        }

        val withGlow = if (isDark && glowAlpha > 0f) {
            withShadow.drawBehind {
                val spreadPx = 6.dp.toPx()
                val cornerPx = 16.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeGlowColor.copy(alpha = glowAlpha),
                            activeGlowColor.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.maxDimension * 0.8f,
                    ),
                    topLeft = Offset(-spreadPx, -spreadPx),
                    size = Size(size.width + spreadPx * 2, size.height + spreadPx * 2),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            }
        } else {
            withShadow
        }

        withGlow.border(
            width = borderWidth,
            color = if (showFeedback) activeBorderColor else Color.Transparent,
            shape = shape,
        )
    }
