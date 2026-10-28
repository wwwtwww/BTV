package dev.frost819.newbv.app.ui.component.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.LocalIsDarkTheme

/**
 * 紧凑型搜索关键词列表项（用于左栏底部关联建议接力列表）。
 *
 * 采用深浅双主题自适应高对比聚焦反色，严禁硬编码纯白文字，确保浅色模式下同样清晰可读。
 *
 * @param modifier 修饰符。
 * @param keyword 关键词文本。
 * @param selected 是否为当前已激活的搜索词。
 * @param onClick 点击回调。
 * @param trailingIcon 可选尾部图标槽位。
 */
@Composable
fun SearchKeyword(
    modifier: Modifier = Modifier,
    keyword: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.24f else 0.14f)
        } else {
            Color.Transparent
        }
    val contentColor =
        if (selected) {
            if (isDark) MaterialTheme.colorScheme.primary else BVColors.PrimaryStrong
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = LocalContentColor.current.copy(alpha = 0.65f),
            )
            Text(
                text = keyword,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailingIcon?.invoke()
        }
    }
}

/**
 * 宽幅搜索建议卡片项（用于右栏双列宽敞实时搜索建议面板）。
 *
 * 单列宽度约 300dp，支持最多 2 行完整展示长尾关联词条，彻底消除窄栏截断痛点。
 *
 * @param modifier 修饰符。
 * @param keyword 建议词条文本。
 * @param isDirectAction 是否为顶部「直接搜索当前输入词」快捷项。
 * @param onClick 点击回调。
 */
@Composable
fun WideSearchSuggestItem(
    modifier: Modifier = Modifier,
    keyword: String,
    isDirectAction: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        if (isDirectAction) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.26f else 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.72f)
        }
    val contentColor =
        if (isDirectAction && !isDark) {
            BVColors.PrimaryStrong
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val borderColor =
        if (isDirectAction) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.06f)
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    Border(
                        border = BorderStroke(1.dp, borderColor),
                        shape = BVShape.medium,
                    ),
                focusedBorder = Border.None,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = LocalContentColor.current.copy(alpha = 0.75f),
            )
            Text(
                text = keyword,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isDirectAction) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 搜索历史流式胶囊标签（用于左栏底部 `FlowRow` 历史词云）。
 *
 * @param modifier 修饰符。
 * @param keyword 历史关键词。
 * @param deleteMode 是否处于删除管理模式。
 * @param onClick 点击回调（普通模式触发搜索，删除模式删除单条）。
 */
@Composable
fun SearchHistoryChip(
    modifier: Modifier = Modifier,
    keyword: String,
    deleteMode: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        if (deleteMode) {
            BVColors.Red.copy(alpha = if (isDark) 0.2f else 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.52f else 0.78f)
        }
    val borderColor =
        if (deleteMode) {
            BVColors.Red.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.1f else 0.08f)
        }

    Surface(
        modifier =
            modifier
                .height(28.dp)
                .widthIn(max = 216.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    Border(
                        border = BorderStroke(1.dp, borderColor),
                        shape = BVShape.pill,
                    ),
                focusedBorder = Border.None,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = keyword,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (deleteMode) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/**
 * 热搜榜单宽幅双列条目（用于右栏初始探索态热搜榜）。
 *
 * 前三名采用高辨识度暖色徽标高亮，4 名及以后采用等宽数字弱化序号，
 * 并支持渲染热词右侧官方角标（热 / 新 / 梗等）。
 *
 * @param modifier 修饰符。
 * @param rank 排名序号（从 1 开始）。
 * @param showName 热搜展示文本。
 * @param iconUrl 热搜右侧角标图片 URL（可选）。
 * @param onClick 点击回调。
 */
@Composable
fun SearchHotwordItem(
    modifier: Modifier = Modifier,
    rank: Int,
    showName: String,
    iconUrl: String? = null,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.4f else 0.68f)
    val rankBadgeColor =
        when (rank) {
            1 -> Color(0xFFE05252)
            2 -> Color(0xFFE07A38)
            3 -> Color(0xFFC99A2E)
            else -> null
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(42.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(BVShape.small)
                        .background(rankBadgeColor ?: Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rank.toString(),
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    fontWeight = FontWeight.Bold,
                    color =
                        if (rankBadgeColor != null) {
                            Color.White
                        } else {
                            LocalContentColor.current.copy(alpha = 0.55f)
                        },
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = showName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!iconUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.height(15.dp),
                )
            }
        }
    }
}
