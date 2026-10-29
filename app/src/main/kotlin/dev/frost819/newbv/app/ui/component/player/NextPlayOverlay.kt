package dev.frost819.newbv.app.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.frost819.newbv.app.ui.state.player.NextPlayOverlayInfo
import dev.frost819.newbv.core.theme.BVTypography

/**
 * 播毕连播推荐微浮层（Next-Up Overlay）。
 *
 * 视频播放结束前 5 秒或结束瞬间在屏幕右下角平滑滑入，
 * 清晰呈现下一部/推荐视频的封面、标题、UP 主与 5 秒倒计时。
 * 遥控器快捷操作：按【OK / 确定】立即起播，按【返回】取消连播。
 *
 * @param modifier 修饰符
 * @param visible 是否可见
 * @param info 下一部视频元数据信息
 * @param countdownSec 倒计时剩余秒数（1~5）
 */
@Composable
fun NextPlayOverlay(
    modifier: Modifier = Modifier,
    visible: Boolean,
    info: NextPlayOverlayInfo?,
    countdownSec: Int,
) {
    AnimatedVisibility(
        visible = visible && info != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        if (info == null) return@AnimatedVisibility

        Box(
            modifier =
                Modifier
                    .width(380.dp)
                    .padding(bottom = 32.dp, end = 36.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color(0xF0121622))
                    .border(1.5.dp, MaterialTheme.colorScheme.border.copy(alpha = 0.7f), MaterialTheme.shapes.large)
                    .padding(14.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 封面 16:9
                    Box(
                        modifier =
                            Modifier
                                .size(width = 112.dp, height = 63.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color(0xFF202636)),
                    ) {
                        if (!info.cover.isNullOrEmpty()) {
                            AsyncImage(
                                model = info.cover,
                                contentDescription = info.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // 左上角角标（下一集 / 推荐）
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopStart)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 0.dp,
                                            bottomEnd = 8.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 0.dp,
                                        ),
                                    ).background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (info.isEpisode) "下一集" else "推荐",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 标题与 UP 主
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (!info.author.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底部倒计时与遥控器按键引导
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = "$countdownSec 秒后自动播放",
                            style = BVTypography.tabular(MaterialTheme.typography.labelMedium),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Text(
                        text = "[OK] 立即播 · [返回] 取消",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}
