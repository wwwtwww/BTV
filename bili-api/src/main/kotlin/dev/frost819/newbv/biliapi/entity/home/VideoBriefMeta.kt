package dev.frost819.newbv.biliapi.entity.home

/**
 * 首页 Hero Showcase 按需补充的视频轻量元信息。
 *
 * 用于在获焦防抖停稳后补全推荐流缺失的子分区标签与视频简介正文。
 *
 * @property aid 视频 AV 号。
 * @property tagName 视频子分区名称（如 "单机游戏"、"数码"）。
 * @property description 视频简介正文。
 * @property rcmdReason 推荐理由文案（可选）。
 */
data class VideoBriefMeta(
    val aid: Long,
    val tagName: String? = null,
    val description: String? = null,
    val rcmdReason: String? = null,
)
