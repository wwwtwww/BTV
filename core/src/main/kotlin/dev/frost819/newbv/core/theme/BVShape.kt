package dev.frost819.newbv.core.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes as CommonShapes
import androidx.tv.material3.Shapes as TvShapes

/**
 * new BV 统一圆角规格（Shape Tokens）。
 *
 * 遵循 10-foot 大屏体验标准，收敛全仓分散圆角：
 * - [small] (8.dp): 微型标签（Tag）、清晰度角标、时间指示微标等。
 * - [medium] (12.dp): 功能按钮、抽屉导航项、菜单列表行、设置项等。
 * - [large] (16.dp): 视频卡片、弹窗对话框、功能浮层等。
 * - [pill] (50% 全圆角): 筛选胶囊（Filter Chips）、快捷按键圆形徽章等。
 * - [circle]: 正圆（头像、纯圆按键）。
 */
object BVShape {
    /** 8.dp 圆角，用于微型标签、清晰度角标、时间指示微标等。 */
    val small = RoundedCornerShape(8.dp)

    /** 12.dp 圆角，用于功能按钮、抽屉导航项、菜单列表行、设置项等。 */
    val medium = RoundedCornerShape(12.dp)

    /** 16.dp 圆角，用于视频卡片、弹窗对话框、功能浮层等。 */
    val large = RoundedCornerShape(16.dp)

    /** 50% 胶囊全圆角，用于筛选胶囊（Filter Chips）等。 */
    val pill = RoundedCornerShape(50)

    /** 正圆，用于头像、圆形按钮。 */
    val circle = CircleShape

    /** 右侧滑出面板的左侧入场大圆角（16.dp，用于评论抽屉等）。 */
    val drawerStart = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)

    /** 左侧贴边弹出的提示横幅右侧中圆角（12.dp，用于跳过片头等提示）。 */
    val bannerEnd = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)

    /** TV Material3 Shapes 配置。 */
    val tv = TvShapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = small,
        medium = medium,
        large = large,
        extraLarge = RoundedCornerShape(24.dp),
    )

    /** Common Material3 Shapes 配置。 */
    val common = CommonShapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = small,
        medium = medium,
        large = large,
        extraLarge = RoundedCornerShape(24.dp),
    )
}
