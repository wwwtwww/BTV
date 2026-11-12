package dev.frost819.newbv.biliapi.entity.video

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.util.BiliLogger
import dev.frost819.newbv.biliapi.util.runSuspendCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.DataInputStream

data class VideoShot(
    val times: List<UShort>,
    val images: List<ByteArray?>,
    val imageCountX: Int,
    val imageCountY: Int,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    companion object {
        /** 模块内统一日志入口（见 [BiliLogger]），避免直接使用裸 [println]。 */
        private val logger = BiliLogger

        suspend fun fromVideoShot(videoShot: dev.frost819.newbv.biliapi.http.entity.video.VideoShot): VideoShot? =
            withContext(Dispatchers.IO) {
                val images =
                    videoShot.image
                        .map { imageUrl ->
                            async {
                                runSuspendCatching {
                                    BiliHttpApi.download(imageUrl)
                                }.getOrNull()
                            }
                        }.awaitAll()
                if (images.contains(null)) {
                    logger.warn { "download video shot images failed" }
                    return@withContext null
                }

                val timeBinary =
                    runSuspendCatching {
                        BiliHttpApi.download(
                            videoShot.pvData ?: throw IllegalStateException("pvData is null"),
                        )
                    }.onFailure {
                        logger.error(it) { "download video shot times binary failed" }
                        return@withContext null
                    }.getOrNull()

                val times = mutableListOf<UShort>()
                runSuspendCatching {
                    DataInputStream(ByteArrayInputStream(timeBinary)).use {
                        // if has next
                        while (it.available() > 0) {
                            times.add(it.readUnsignedShort().toUShort())
                        }
                    }
                }.onFailure {
                    logger.error(it) { "parse video shot times binary failed" }
                    return@withContext null
                }

                return@withContext VideoShot(
                    times = times.drop(1),
                    images = images,
                    imageCountX = videoShot.imgXLen,
                    imageCountY = videoShot.imgYLen,
                    imageWidth = videoShot.imgXSize,
                    imageHeight = videoShot.imgYSize,
                )
            }
    }
}
