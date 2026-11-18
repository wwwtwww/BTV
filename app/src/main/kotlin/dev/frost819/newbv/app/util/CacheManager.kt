package dev.frost819.newbv.app.util

import android.content.Context
import dev.frost819.newbv.data.datastore.Prefs
import java.io.File

/**
 * 缓存管理器（缓存满阈值自动清理）。
 *
 * 统一管理应用图片等通用缓存（更新安装包已迁移至独立的 ota_packages 目录专属管理）：
 * - 大小统计：递归计算全部缓存目录的总字节数。
 * - 阈值检查：App 启动 + 缓存写入后调用 [checkCache]。
 * - LRU 清理：总大小超过阈值时，按文件最后修改时间从旧到新删除（近似 LRU），
 *   清理至阈值的 [CLEAN_TARGET_RATIO] 比例，避免刚清理又立即触发。
 * - 受 [Prefs.cacheAutoClean] 开关控制，关闭后不自动清理。
 * - 图片缓存的常规淘汰由 Coil 在每次写入时自动执行（BVApplication 构建
 *   DiskCache 时按阈值设置 maxSizeBytes），本类检查是运行时兜底。
 *
 * 本类无自身状态（状态都在磁盘与 Prefs 中），可按需创建实例；
 * 构造参数全部可注入，便于纯 JVM 单元测试。
 *
 * @param cacheDirs 参与统计与清理的缓存目录列表。
 * @param thresholdMb 缓存阈值（MB）提供者，0 表示不限制；每次检查时读取最新值。
 * @param autoCleanEnabled 自动清空开关提供者，每次检查时读取最新值。
 */
class CacheManager(
    private val cacheDirs: List<File>,
    private val thresholdMb: () -> Int = { Prefs.cacheThreshold },
    private val autoCleanEnabled: () -> Boolean = { Prefs.cacheAutoClean },
) {
    /**
     * 从 Android [Context] 构建缓存管理器。
     *
     * @param context 任意 Context（自动取 applicationContext.cacheDir 下的缓存目录）。
     */
    constructor(context: Context) : this(
        cacheDirs =
            listOfNotNull(
                File(context.cacheDir, IMAGE_CACHE_DIR),
            ),
    )

    /** 当前缓存总大小（字节，全部缓存目录之和）。 */
    fun cacheSize(): Long = cacheDirs.sumOf { folderSize(it) }

    /** 手动清空全部缓存目录（删除目录内容，保留目录本身）。 */
    fun clearAll() {
        cacheDirs.forEach { dir ->
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    /**
     * 检查缓存：开启自动清理且总大小超过阈值时，清理至阈值的
     * [CLEAN_TARGET_RATIO] 比例。
     *
     * @param preserve 需要保留的文件（如刚下载完待安装的 APK），清理时跳过。
     * @return 本次清理删除的字节数，未触发清理返回 0。
     */
    fun checkCache(preserve: File? = null): Long {
        if (!autoCleanEnabled()) return 0L
        val thresholdBytes = thresholdMb().toBytes()
        if (thresholdBytes <= 0L) return 0L // 不限制
        if (cacheSize() <= thresholdBytes) return 0L
        return evictLru(
            cacheDirs,
            (thresholdBytes * CLEAN_TARGET_RATIO).toLong(),
            preserve,
        )
    }

    /**
     * LRU 清理核心：跨 [dirs] 删除最旧的文件，直到总大小不超过 [targetBytes]。
     *
     * 全部目录的文件合并后按 lastModified 升序删除（最旧优先），只删除文件
     * 不删除目录本身；[preserve] 指定的文件参与大小统计但不会被删除。
     *
     * @param dirs 目标目录列表，不存在的目录自动忽略。
     * @param targetBytes 目标总大小（字节）。
     * @param preserve 需要保留的文件，可为 null。
     * @return 实际删除的字节数。
     */
    internal fun evictLru(
        dirs: List<File>,
        targetBytes: Long,
        preserve: File? = null,
    ): Long {
        val files =
            dirs
                .filter { it.exists() }
                .flatMap { dir -> dir.walkTopDown().filter { it.isFile }.toList() }
        var total = files.sumOf { it.length() }
        if (total <= targetBytes) return 0L

        var freed = 0L
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= targetBytes) break
            if (file == preserve) continue
            val length = file.length()
            if (file.delete()) {
                total -= length
                freed += length
            }
        }
        return freed
    }

    companion object {
        /** 图片缓存目录名（与 Coil 磁盘缓存目录一致）。 */
        const val IMAGE_CACHE_DIR = "image_cache"

        /** 字节/MB 换算基数。 */
        const val BYTES_PER_MB: Long = 1024L * 1024L

        /** 阈值"不限制"标识（0）。 */
        const val THRESHOLD_UNLIMITED = 0

        /** 清理目标比例：清理至阈值的 80%，避免刚清理又立即触发。 */
        internal const val CLEAN_TARGET_RATIO = 0.8

        /** 自动清理关闭时 Coil 磁盘缓存的 maxSizeBytes（1TB，实际等同不限制）。 */
        const val UNLIMITED_DISK_CACHE_BYTES: Long = 1L shl 40

        /** 低内存设备（isLowRamDevice）图片内存缓存上限。 */
        const val LOW_RAM_MEMORY_CACHE_BYTES: Long = 24L * BYTES_PER_MB

        /** 小堆设备判定阈值（堆上限 ≤ 192MB 的低配盒子）。 */
        const val SMALL_HEAP_MAX_BYTES: Long = 192L * BYTES_PER_MB

        /** 小堆设备图片内存缓存上限。 */
        const val SMALL_HEAP_MEMORY_CACHE_BYTES: Long = 32L * BYTES_PER_MB

        /** 常规设备图片内存缓存上限。 */
        const val DEFAULT_MEMORY_CACHE_BYTES: Long = 64L * BYTES_PER_MB

        /**
         * 计算图片内存缓存上限（字节）（OOM 防护）。
         *
         * TV 设备内存差异极大（512MB~4GB），Coil 默认按「应用可用内存的 25%」分配
         * 内存缓存，高配设备上可达上百 MB，与弹幕/播放器并存时有 OOM 风险。
         * 本项目图片以 960x540 海报为主，显式分档上限足以维持流畅回滑体验：
         * - 低内存设备（[isLowRamDevice] 为 true）：24MB；
         * - 堆上限不超过 [SMALL_HEAP_MAX_BYTES]（低配盒子）：32MB；
         * - 其余设备：64MB。
         *
         * @param maxHeapBytes 应用堆上限（`Runtime.getRuntime().maxMemory()`）。
         * @param isLowRamDevice 系统低内存设备标记（`ActivityManager.isLowRamDevice`）。
         * @return 内存缓存上限字节数。
         */
        fun memoryCacheMaxBytes(
            maxHeapBytes: Long,
            isLowRamDevice: Boolean,
        ): Long =
            when {
                isLowRamDevice -> LOW_RAM_MEMORY_CACHE_BYTES
                maxHeapBytes <= SMALL_HEAP_MAX_BYTES -> SMALL_HEAP_MEMORY_CACHE_BYTES
                else -> DEFAULT_MEMORY_CACHE_BYTES
            }

        /**
         * 递归统计目录内所有文件的总大小（字节）。
         *
         * @param dir 目标目录，不存在时返回 0。
         */
        fun folderSize(dir: File): Long {
            if (!dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }

        private fun Int.toBytes(): Long = this * BYTES_PER_MB
    }
}
