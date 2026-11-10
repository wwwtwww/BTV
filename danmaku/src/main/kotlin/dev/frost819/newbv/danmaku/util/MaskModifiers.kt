package dev.frost819.newbv.danmaku.util

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.caverock.androidsvg.SVG
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuWebMaskFrame

/** 顶层缓存 Paint 对象，避免每次绘制都创建新实例 */
private val maskPaint = Paint()

/**
 * 基础蒙版 Modifier：将 Bitmap 以 [BlendMode.DstIn] 方式叠加到内容上。
 *
 * 通过 saveLayer + DstIn 混合实现"挖空"效果，仅保留 Bitmap 中非透明区域的内容。
 * 已迁移至 [drawWithContent]，避免 `composed` 每次重组创建新实例。
 *
 * @param bitmap      蒙版 Bitmap（ARGB_8888）
 * @param videoAspectRatio 视频宽高比（如 1920/1080 ≈ 1.777），用于将蒙版对齐到视频区域
 */
fun Modifier.bitmapMask(
    bitmap: Bitmap,
    videoAspectRatio: Float,
): Modifier {
    val imageBitmap = bitmap.asImageBitmap()
    return drawWithContent {
        // 安全保护：尺寸为 0 或宽高比非法时直接绘制原内容
        if (size.width <= 0f || size.height <= 0f || videoAspectRatio <= 0f) {
            drawContent()
            return@drawWithContent
        }

        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), maskPaint)
            drawContent()

            val screenWidth = size.width
            val screenHeight = size.height
            val screenAspectRatio = screenWidth / screenHeight

            val dstWidth: Float
            val dstHeight: Float
            val offsetX: Float
            val offsetY: Float

            if (videoAspectRatio > screenAspectRatio) {
                dstWidth = screenWidth
                dstHeight = dstWidth / videoAspectRatio
                offsetX = 0f
                offsetY = (screenHeight - dstHeight) / 2f
            } else {
                dstHeight = screenHeight
                dstWidth = dstHeight * videoAspectRatio
                offsetY = 0f
                offsetX = (screenWidth - dstWidth) / 2f
            }

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(dstWidth.toInt(), dstHeight.toInt()),
                blendMode = BlendMode.DstIn,
            )

            canvas.restore()
        }
    }
}

/**
 * WebMask 蒙版 Modifier：将 SVG 数据渲染为 Bitmap 后应用蒙版效果。
 *
 * [DanmakuWebMaskFrame] 包含 base64 编码的 SVG 字符串，先解码再渲染为 Bitmap。
 * Bitmap 创建结果会被 remember，只在帧数据变化时重新解析 SVG。
 * 保留 `composed` 以提供 `remember` 作用域缓存解码结果，绘制逻辑已迁移至 [drawWithContent]。
 *
 * @param frame      WebMask 帧数据
 * @param aspectRatio 视频宽高比（用于蒙版对齐）
 */
fun Modifier.danmakuWebMask(
    frame: DanmakuWebMaskFrame,
    aspectRatio: Float,
): Modifier =
    composed {
        val bitmap =
            remember(frame) {
                runCatching {
                    val svgObj = SVG.getFromString(frame.svg)
                    val svgWidth = svgObj.documentWidth.toInt().coerceAtLeast(1)
                    val svgHeight = svgObj.documentHeight.toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    svgObj.renderToCanvas(canvas)
                    bmp
                }.getOrNull()
            } ?: return@composed this

        bitmapMask(bitmap, aspectRatio)
    }

/**
 * MobMask 蒙版 Modifier：将 1bpp 像素数据解码为 Bitmap 后应用蒙版效果。
 *
 * [DanmakuMobMaskFrame] 包含帧的宽高和 1bit/pixel 的位图数据（MSB first）。
 * 位图数据被解码为 ARGB_8888 Bitmap（0bit = BLACK，1bit = TRANSPARENT），实现"挖空"效果。
 * Bitmap 创建结果会被 remember，只在帧数据变化时重新解码。
 * 保留 `composed` 以提供 `remember` 作用域缓存解码结果，绘制逻辑已迁移至 [drawWithContent]。
 *
 * @param frame      MobMask 帧数据
 * @param aspectRatio 视频宽高比（用于蒙版对齐）
 */
fun Modifier.danmakuMobMask(
    frame: DanmakuMobMaskFrame,
    aspectRatio: Float,
): Modifier =
    composed {
        val bitmap =
            remember(frame) {
                val width = frame.width
                val height = frame.height
                // 安全保护：尺寸为 0 或数据长度不足时返回 null
                if (width <= 0 || height <= 0) return@remember null
                val expectedBytes = (width * height + 7) / 8
                if (frame.image.size < expectedBytes) return@remember null

                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    val pixels =
                        IntArray(width * height) { i ->
                            val byteIndex = i / 8
                            val bitOffset = 7 - (i % 8)
                            val bit = (frame.image[byteIndex].toInt() shr bitOffset) and 1
                            // 1bit = TRANSPARENT（挖空），0bit = BLACK
                            if (bit == 1) android.graphics.Color.TRANSPARENT else android.graphics.Color.BLACK
                        }
                    setPixels(pixels, 0, width, 0, 0, width, height)
                }
            } ?: return@composed this

        bitmapMask(bitmap, aspectRatio)
    }

/**
 * 弹幕蒙版 Modifier（统一入口）。
 *
 * 自动根据 [DanmakuMaskFrame] 的具体类型（[DanmakuWebMaskFrame] / [DanmakuMobMaskFrame]）
 * 分发到对应的 Modifier 实现。
 *
 * @param frame      蒙版帧数据，null 时不应用任何效果
 * @param aspectRatio 视频宽高比（用于蒙版对齐到视频区域）
 */
fun Modifier.danmakuMask(
    frame: DanmakuMaskFrame?,
    aspectRatio: Float,
): Modifier =
    if (frame == null) {
        this
    } else {
        composed {
            when (frame) {
                is DanmakuWebMaskFrame -> danmakuWebMask(frame, aspectRatio)
                is DanmakuMobMaskFrame -> danmakuMobMask(frame, aspectRatio)
            }
        }
    }
