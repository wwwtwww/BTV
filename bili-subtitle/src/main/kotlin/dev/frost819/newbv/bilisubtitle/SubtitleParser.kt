package dev.frost819.newbv.bilisubtitle

import dev.frost819.newbv.bilisubtitle.entity.BiliSubtitle
import dev.frost819.newbv.bilisubtitle.entity.SubtitleItem
import dev.frost819.newbv.bilisubtitle.entity.Timestamp
import kotlinx.serialization.json.Json

/**
 * 字幕解析器。
 *
 * 将 B 站字幕原始字符串解析为模块内部统一的 [SubtitleItem] 列表，
 * 支持 BCC（B 站 JSON 字幕）和 SRT 两种输入格式。
 * 解析失败或输入为空时返回空列表，不抛出异常。
 *
 * @see SubtitleItem
 * @see SubtitleEncoder
 */
object SubtitleParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

    /**
     * 解析 BCC 格式字幕 JSON 字符串。
     *
     * 将 B 站字幕 JSON 反序列化为 [BiliSubtitle]，再逐条转换为 [SubtitleItem]。
     * 若 JSON 解析失败（格式错误、字段缺失等），静默返回空列表。
     *
     * @param bcc BCC 格式字幕 JSON 原始字符串
     * @return 解析后的字幕条目列表；输入为空或解析失败时返回空列表
     *
     * @see Timestamp.fromBccString
     */
    fun fromBccString(bcc: String): List<SubtitleItem> {
        if (bcc.isBlank()) return emptyList()
        val result = mutableListOf<SubtitleItem>()
        val bccResult =
            runCatching {
                json.decodeFromString<BiliSubtitle>(bcc)
            }.getOrNull()
        bccResult?.body?.forEach { bccItem ->
            result.add(
                SubtitleItem(
                    from = Timestamp.fromBccString(bccItem.from),
                    to = Timestamp.fromBccString(bccItem.to),
                    content = bccItem.content,
                ),
            )
        }
        return result
    }

    /**
     * 解析 SRT 格式字幕字符串。
     *
     * 按空行分隔字幕块，从每个块中提取序号行、时间行（含 `-->` 分隔符）
     * 和后续内容行。时间解析或格式异常的块会被静默跳过。
     *
     * @param srt SRT 格式字幕原始字符串
     * @return 解析后的字幕条目列表；输入为空时返回空列表
     *
     * @see Timestamp.fromSrtString
     */
    fun fromSrtString(srt: String): List<SubtitleItem> {
        if (srt.isBlank()) return emptyList()
        val result = mutableListOf<SubtitleItem>()
        val blocks = srt.trim().split(Regex("(\\r?\\n){2,}"))
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 2) continue

            val timeIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeIndex == -1 || timeIndex + 1 >= lines.size) continue

            val timeLine = lines[timeIndex]
            val times = timeLine.split("-->").map { it.trim() }
            if (times.size < 2) continue

            val from = runCatching { Timestamp.fromSrtString(times[0]) }.getOrNull() ?: continue
            val to = runCatching { Timestamp.fromSrtString(times[1]) }.getOrNull() ?: continue
            val content = lines.drop(timeIndex + 1).joinToString("\n")

            result.add(
                SubtitleItem(
                    from = from,
                    to = to,
                    content = content,
                ),
            )
        }
        return result
    }
}
