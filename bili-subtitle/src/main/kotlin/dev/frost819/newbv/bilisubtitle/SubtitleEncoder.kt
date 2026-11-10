package dev.frost819.newbv.bilisubtitle

import dev.frost819.newbv.bilisubtitle.entity.BiliSubtitle
import dev.frost819.newbv.bilisubtitle.entity.BiliSubtitleItem
import dev.frost819.newbv.bilisubtitle.entity.SrtSubtitleItem
import dev.frost819.newbv.bilisubtitle.entity.SubtitleItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 字幕编码器。
 *
 * 将模块内部统一的 [SubtitleItem] 列表编码为目标格式字符串，
 * 支持 BCC（B 站 JSON 字幕）和 SRT 两种输出格式。
 *
 * @see SubtitleItem
 * @see SubtitleParser
 */
object SubtitleEncoder {
    /**
     * 将字幕条目列表编码为 BCC 格式 JSON 字符串。
     *
     * 使用默认样式配置（白色字体、紫色半透明背景、无描边），
     * 所有条目统一设置 `location = 2`（底部居中）。
     *
     * @param subtitles 待编码的字幕条目列表
     * @return 符合 B 站 BCC 字幕规范的 JSON 字符串
     *
     * @see BiliSubtitle
     * @see BiliSubtitleItem
     */
    fun encodeToBcc(subtitles: List<SubtitleItem>): String {
        val bccSubtitles = mutableListOf<BiliSubtitleItem>()
        subtitles.forEach {
            bccSubtitles.add(
                BiliSubtitleItem(
                    from = it.from.getBccTime(),
                    to = it.to.getBccTime(),
                    location = 2,
                    content = it.content,
                ),
            )
        }
        val bccSubtitle =
            BiliSubtitle(
                fontSize = 0.4f,
                fontColor = "#FFFFFF",
                backgroundAlpha = 0.5f,
                backgroundColor = "#9C27B0",
                stroke = "none",
                body = bccSubtitles,
            )
        return Json.encodeToString(bccSubtitle)
    }

    /**
     * 将字幕条目列表编码为 SRT 格式字符串。
     *
     * 条目按原始顺序编号（从 1 开始），换行符 `\n` 会被转义为字面量 `\\n`
     * 以符合 SRT 单条字幕内容规范。各条目之间以空行分隔。
     *
     * @param subtitles 待编码的字幕条目列表
     * @return 符合 SRT 规范的纯文本字符串
     *
     * @see SrtSubtitleItem
     */
    fun encodeToSrt(subtitles: List<SubtitleItem>): String {
        var result = ""
        subtitles.forEachIndexed { index, data ->
            val srtSubtitleItem =
                SrtSubtitleItem(
                    index = index + 1,
                    from = data.from.getSrtTime(),
                    to = data.to.getSrtTime(),
                    content = data.content.replace("\n", "\\n"),
                )
            if (index != 0) result += "\n"
            result += srtSubtitleItem.toRaw()
        }
        return result
    }
}
