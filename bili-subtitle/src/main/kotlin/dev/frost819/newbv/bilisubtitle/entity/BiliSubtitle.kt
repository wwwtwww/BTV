package dev.frost819.newbv.bilisubtitle.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * B 站 BCC 格式字幕数据模型。
 *
 * 对应 B 站字幕 JSON 接口的顶层结构，包含字幕样式配置与字幕条目列表。
 * 同时兼容 AI 字幕（`Stroke` 大写）和普通字幕（`stroke` 小写）的字段差异。
 *
 * @property fontSize 字幕字体大小（相对比例值）
 * @property fontColor 字幕字体颜色（十六进制色值，如 `#FFFFFF`）
 * @property backgroundAlpha 字幕背景透明度（0.0 ~ 1.0）
 * @property backgroundColor 字幕背景颜色（十六进制色值）
 * @property stroke 字幕描边样式，AI 字幕返回大写 `Stroke`，普通字幕返回小写 `stroke`
 * @property type 字幕类型标识
 * @property lang 字幕语言标识（如 `zh-CN`、`en`）
 * @property version 字幕数据版本号
 * @property body 字幕条目列表
 *
 * @see BiliSubtitleItem
 */
@Serializable
data class BiliSubtitle(
    @SerialName("font_size")
    val fontSize: Float? = null,
    @SerialName("font_color")
    val fontColor: String? = null,
    @SerialName("background_alpha")
    val backgroundAlpha: Float? = null,
    @SerialName("background_color")
    val backgroundColor: String? = null,
    // AI字幕返回的属性是大写的（Stroke），非AI字幕是小写的（stroke）
    // 兼容大小写写法，序列化输出使用小写 stroke
    @JsonNames("Stroke")
    val stroke: String? = null,
    val type: String? = null,
    val lang: String? = null,
    val version: String? = null,
    val body: List<BiliSubtitleItem> = emptyList(),
)

/**
 * B 站 BCC 格式单条字幕条目。
 *
 * 表示一个时间片段内的字幕内容，包含起止时间、显示位置、文本等属性。
 *
 * @property from 字幕起始时间（秒），相对于视频开头
 * @property to 字幕结束时间（秒），相对于视频开头
 * @property sid 字幕条目唯一标识
 * @property location 字幕显示位置（2 = 底部居中）
 * @property content 字幕文本内容
 * @property music 音乐标记（用于标记背景音乐时段）
 * @property version 自动翻译字幕特有属性，标识翻译版本
 */
@Serializable
data class BiliSubtitleItem(
    val from: Float,
    val to: Float,
    val sid: Int? = null,
    val location: Int? = null,
    val content: String,
    val music: Float? = null,
    // 自动翻译字幕特有属性
    val version: String? = null,
)
