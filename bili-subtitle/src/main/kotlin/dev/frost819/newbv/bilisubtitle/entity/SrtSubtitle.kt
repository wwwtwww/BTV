package dev.frost819.newbv.bilisubtitle.entity

import kotlinx.serialization.Serializable

/**
 * SRT 格式字幕容器。
 *
 * 封装一组 SRT 字幕条目列表，用于 SRT 格式的序列化与反序列化。
 *
 * @property content SRT 字幕条目列表
 *
 * @see SrtSubtitleItem
 */
@Serializable
data class SrtSubtitle(
    val content: List<SrtSubtitleItem> = emptyList(),
)

/**
 * SRT 格式单条字幕条目。
 *
 * 表示标准 SRT 字幕格式中的一条记录，包含序号、起止时间和字幕文本。
 *
 * @property index 字幕条目序号（从 1 开始）
 * @property from 起始时间，SRT 时间格式（`HH:MM:SS,mmm`）
 * @property to 结束时间，SRT 时间格式（`HH:MM:SS,mmm`）
 * @property content 字幕文本内容
 */
@Serializable
data class SrtSubtitleItem(
    val index: Int,
    val from: String,
    val to: String,
    val content: String,
) {
    /**
     * 将当前条目转换为 SRT 原始文本格式。
     *
     * 输出格式为：
     * ```
     * {index}
     * {from} --> {to}
     * {content}
     * ```
     *
     * @return 符合 SRT 规范的文本块
     */
    fun toRaw() =
        """
        $index
        $from --> $to
        $content
        
        """.trimIndent()
}
