package dev.frost819.newbv.bilisubtitle

import dev.frost819.newbv.bilisubtitle.entity.BiliSubtitle
import dev.frost819.newbv.bilisubtitle.entity.SubtitleItem
import dev.frost819.newbv.bilisubtitle.entity.Timestamp
import kotlinx.serialization.json.Json

object SubtitleParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

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
