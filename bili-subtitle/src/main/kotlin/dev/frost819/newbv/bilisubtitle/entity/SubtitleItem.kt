package dev.frost819.newbv.bilisubtitle.entity

/**
 * 通用字幕条目（模块内部统一表示）。
 *
 * 与格式无关的字幕数据载体，由 [dev.frost819.newbv.bilisubtitle.SubtitleParser]
 * 从 BCC/SRT 原始格式解析而来，供 [dev.frost819.newbv.bilisubtitle.SubtitleEncoder]
 * 编码输出为目标格式。
 *
 * @property from 字幕起始时间戳
 * @property to 字幕结束时间戳
 * @property content 字幕文本内容（支持 `\n` 换行）
 *
 * @see Timestamp
 */
data class SubtitleItem(
    val from: Timestamp,
    val to: Timestamp,
    val content: String,
) {
    /**
     * 判断指定播放时刻该字幕是否处于显示状态。
     *
     * @param time 当前播放位置（毫秒）
     * @return 若 `time` 落在 [from] 与 [to] 之间（含边界）则返回 `true`
     */
    fun isShowing(time: Long) = from.totalMills <= time && to.totalMills >= time
}
