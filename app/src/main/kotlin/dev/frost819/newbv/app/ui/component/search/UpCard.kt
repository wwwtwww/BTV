package dev.frost819.newbv.app.ui.component.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.core.focus.TvFocusDefaults
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.LocalIsDarkTheme

/**
 * UP 主搜索结果横向宽卡片（适配 3 列宽幅网格）。
 *
 * 采用「左圆头像 + 右侧用户名与双行签名」横向信息流布局，
 * 固定双行签名高度以保障多列网格行高整齐划一，并接入大屏 1.06x 聚焦缩放与双主题高亮轮廓边框。
 *
 * @param modifier 修饰符。
 * @param avatar 头像 URL。
 * @param username 用户名。
 * @param sign 个性签名。
 * @param onClick 点击跳转 UP 主空间回调。
 */
@Composable
fun UpCard(
    modifier: Modifier = Modifier,
    avatar: String,
    username: String,
    sign: String,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val focusedBorderColor =
        if (isDark) {
            MaterialTheme.colorScheme.border
        } else {
            BVColors.PrimaryStrong
        }
    val idleBorderColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.06f)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.large),
        scale = TvFocusDefaults.surfaceScale(),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    Border(
                        border = BorderStroke(1.dp, idleBorderColor),
                        shape = BVShape.large,
                    ),
                focusedBorder =
                    Border(
                        border = BorderStroke(TvFocusDefaults.borderWidth, focusedBorderColor),
                        shape = BVShape.large,
                    ),
            ),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                pressedContainerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = avatar,
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(BVShape.circle)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = sign.ifBlank { "这个人很神秘" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
