package dev.frost819.newbv.bilisubtitle.entity

import java.util.Locale

/**
 * 字幕时间戳。
 *
 * 以时、分、秒、毫秒精确表示某一时刻，同时维护 [totalMills] 总毫秒数
 * 以便快速进行时间比较与区间判断。
 *
 * 提供与 BCC（秒浮点）和 SRT（`HH:MM:SS,mmm`）两种格式的互转能力。
 *
 * @property hours 小时部分（≥ 0）
 * @property minutes 分钟部分（0 ~ 59）
 * @property seconds 秒部分（0 ~ 59）
 * @property milliSeconds 毫秒部分（0 ~ 999）
 * @property totalMills 换算后的总毫秒数，由 `init` 块自动计算
 */
data class Timestamp(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val milliSeconds: Int,
    var totalMills: Long = 0,
) {
    companion object {
        /**
         * 从 SRT 时间字符串解析时间戳。
         *
         * 输入格式为 `HH:MM:SS,mmm`（如 `00:01:23,456`）。
         *
         * @param srtTime SRT 格式时间字符串
         * @return 解析后的 [Timestamp] 实例
         * @throws NumberFormatException 若字符串格式不符合 SRT 时间规范
         * @throws IndexOutOfBoundsException 若分隔后字段数不足 4 个
         */
        fun fromSrtString(srtTime: String): Timestamp {
            val parts = srtTime.split(":", ",")
            return Timestamp(
                hours = parts[0].toInt(),
                minutes = parts[1].toInt(),
                seconds = parts[2].toInt(),
                milliSeconds = parts[3].toInt(),
            )
        }

        /**
         * 从 BCC 时间浮点值解析时间戳。
         *
         * BCC 格式以秒为单位的小数浮点值（如 `83.456` 表示 1 分 23 秒 456 毫秒）。
         *
         * @param bccTime BCC 格式时间（秒，浮点数）
         * @return 解析后的 [Timestamp] 实例
         */
        fun fromBccString(bccTime: Float): Timestamp {
            val mils = (bccTime * 1000).toInt()
            val hours = mils / (1000 * 60 * 60)
            val minutes = (mils % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (mils % (1000 * 60)) / (1000)
            val milliSeconds = mils % 1000
            return Timestamp(hours, minutes, seconds, milliSeconds)
        }
    }

    init {
        totalMills = hours * 60 * 60 * 1000L + minutes * 60 * 1000L + seconds * 1000L + milliSeconds
    }

    /**
     * 转换为 BCC 格式时间（秒浮点值）。
     *
     * @return 以秒为单位的浮点时间，精度到毫秒级
     */
    fun getBccTime(): Float = hours * 60 * 60 + minutes * 60 + seconds + milliSeconds / 1000f

    /**
     * 转换为 SRT 格式时间字符串。
     *
     * 输出格式为 `HH:MM:SS,mmm`（如 `00:01:23,456`），毫秒部分固定 3 位补零。
     *
     * @return SRT 规范的时间字符串
     */
    fun getSrtTime(): String {
        val h = String.format(Locale.US, "%02d", hours)
        val m = String.format(Locale.US, "%02d", minutes)
        val s = String.format(Locale.US, "%02d", seconds)
        val ms = String.format(Locale.US, "%03d", milliSeconds.coerceIn(0, 999))
        return "$h:$m:$s,$ms"
    }
}
