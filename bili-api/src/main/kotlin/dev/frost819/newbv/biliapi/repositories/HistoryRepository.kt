package dev.frost819.newbv.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import bilibili.app.interfaces.v1.cursor
import bilibili.app.interfaces.v1.cursorV2Req
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.user.HistoryData
import dev.frost819.newbv.biliapi.http.BiliHttpApi

class HistoryRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val historyStub
        get() =
            runCatching {
                HistoryGrpcKt.HistoryCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    suspend fun getHistories(
        cursor: Long,
        preferApiType: ApiType,
    ): HistoryData =
        when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi
                        .getHistories(
                            viewAt = cursor,
                        ).getResponseData()
                HistoryData.fromHistoryResponse(data)
            }

            ApiType.App -> {
                val reply =
                    historyStub?.cursorV2(
                        cursorV2Req {
                            this.cursor =
                                cursor {
                                    max = cursor
                                }
                            business = "archive"
                        },
                    )
                HistoryData.fromHistoryResponse(reply!!)
            }
        }

    /**
     * 删除单条历史记录。
     *
     * @param kid 条目标识（如 "archive_12345" 或 "pgc_67890"）
     */
    suspend fun deleteHistory(kid: String) {
        val csrf =
            authRepository.biliJct?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("bili_jct is empty")
        BiliHttpApi
            .deleteHistory(
                kid = kid,
                csrf = csrf,
            ).getResponseData()
    }
}
