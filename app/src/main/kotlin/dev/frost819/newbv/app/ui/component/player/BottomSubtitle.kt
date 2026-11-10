package dev.frost819.newbv.app.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.bilisubtitle.entity.SubtitleItem
import dev.frost819.newbv.core.theme.BVTheme

/**
 * 底部字幕显示组件。
 *
 * 根据当前播放位置查找匹配的字幕条目并显示在屏幕底部居中位置。
 * 字幕背景为半透明黑色圆角矩形，可配置字体大小、透明度和底部间距。
 *
 * 当无匹配字幕时：
 * - Debug 构建显示 "【DEBUG】无内容"
 * - Release 构建不显示任何内容
 *
 * @param modifier 修饰符
 * @param subtitleData 字幕数据列表
 * @param currentTime 当前播放位置（毫秒）
 * @param fontSize 字体大小
 * @param opacity 背景透明度（0-1）
 * @param padding 底部间距
 */
@Composable
fun BottomSubtitle(
    modifier: Modifier = Modifier,
    subtitleData: List<SubtitleItem>,
    currentTime: Long,
    fontSize: TextUnit,
    opacity: Float,
    padding: Dp,
) {
    // 直接根据 subtitleData 和 currentTime 计算当前字幕，避免 LaunchedEffect 每 100ms 重启导致不必要的重组
    val currentText = remember(subtitleData, currentTime) {
        // 字幕数据按时间排序，使用 lastOrNull 找到 startTime <= currentTime 的最后一条
        // 然后验证该字幕的结束时间是否 >= currentTime（即正在显示中）
        subtitleData.lastOrNull { it.from.totalMills <= currentTime }
            ?.takeIf { it.to.totalMills >= currentTime }
            ?.content
            ?: if (BuildConfig.DEBUG) "【DEBUG】无内容" else ""
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (currentText.isNotEmpty()) {
            Text(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = padding)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Black.copy(alpha = opacity))
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                text = currentText,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BottomSubtitlePreview() {
    val sampleSubtitle =
        listOf(
            SubtitleItem(
                from =
                    dev.frost819.newbv.bilisubtitle.entity.Timestamp(
                        hours = 0,
                        minutes = 0,
                        seconds = 0,
                        milliSeconds = 0,
                    ),
                to =
                    dev.frost819.newbv.bilisubtitle.entity.Timestamp(
                        hours = 0,
                        minutes = 0,
                        seconds = 10,
                        milliSeconds = 0,
                    ),
                content = "这是一条示例字幕",
            ),
        )

    BVTheme {
        BottomSubtitle(
            subtitleData = sampleSubtitle,
            currentTime = 5000L,
            fontSize = 24.sp,
            opacity = 0.4f,
            padding = 12.dp,
        )
    }
}

// endregion
