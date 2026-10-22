package dev.frost819.newbv.biliapi.entity.home

import dev.frost819.newbv.biliapi.entity.ugc.UgcItem

/**
 * 推荐流数据。
 *
 * @property items 推荐视频列表。
 * @property nextPage 下一页请求游标。
 * @property hasMore 是否还有更多推荐内容。Web 通道由列表是否非空推导，App 通道由服务端 has_more 字段指示。
 */
data class RecommendData(
    val items: List<UgcItem>,
    val nextPage: RecommendPage,
    val hasMore: Boolean = true,
)

/**
 * 推荐流分页游标。
 *
 * @property nextWebIdx Web 接口分页索引。
 * @property nextAppIdx App 接口分页索引。
 */
data class RecommendPage(
    val nextWebIdx: Int = 1,
    val nextAppIdx: Int = 0,
)
