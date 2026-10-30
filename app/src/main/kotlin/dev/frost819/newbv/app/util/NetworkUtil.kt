package dev.frost819.newbv.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

/**
 * 网络连接类型。
 */
enum class NetworkType(
    val displayName: String,
) {
    Wifi("Wi-Fi 无线网络"),
    Ethernet("以太网有线连接"),
    Cellular("移动蜂窝网络"),
    Other("其他网络"),
    Disconnected("网络未连接"),
}

/**
 * 网络诊断报告。
 *
 * @property networkType 当前网络类型
 * @property localIp 本机局域网 IP
 * @property apiLatencyMs B 站 API 服务端往返延迟（毫秒），null 为不可达
 * @property cdnLatencyMs 当前视频 CDN 节点往返延迟（毫秒），null 为未测试或不可达
 */
data class NetworkDiagnosticReport(
    val networkType: NetworkType,
    val localIp: String?,
    val apiLatencyMs: Long?,
    val cdnLatencyMs: Long? = null,
)

/**
 * 网络状态检测与诊断工具类。
 */
object NetworkUtil {
    /**
     * 判断当前设备是否具备可用的互联网连接。
     *
     * @param context Context
     * @return true 为已连接互联网，false 为已断网
     */
    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * 获取当前设备的连接类型（Wi-Fi / 有线以太网 / 蜂窝 / 断开）。
     *
     * @param context Context
     * @return [NetworkType]
     */
    fun getNetworkType(context: Context): NetworkType {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkType.Disconnected

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return NetworkType.Disconnected
            val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkType.Disconnected
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return NetworkType.Disconnected
            }
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
                else -> NetworkType.Other
            }
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo ?: return NetworkType.Disconnected
            @Suppress("DEPRECATION")
            if (!info.isConnected) return NetworkType.Disconnected
            @Suppress("DEPRECATION")
            return when (info.type) {
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.Ethernet
                ConnectivityManager.TYPE_WIFI -> NetworkType.Wifi
                ConnectivityManager.TYPE_MOBILE -> NetworkType.Cellular
                else -> NetworkType.Other
            }
        }
    }

    /**
     * 获取本机首个非环回（Non-Loopback）IPv4 地址。
     *
     * @return IPv4 字符串（如 "192.168.1.100"），获取失败返回 null
     */
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 探测目标主机/URL 的往返延迟。
     *
     * @param targetUrl 探测目标 URL（默认为 B 站 API 服务）
     * @param timeoutMs 超时时间（毫秒）
     * @return 往返耗时（毫秒），失败或超时返回 null
     */
    suspend fun probeLatency(
        targetUrl: String = "https://api.bilibili.com/x/web-interface/nav",
        timeoutMs: Int = 3000,
    ): Long? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val start = System.currentTimeMillis()
                val url = URL(targetUrl)
                connection = (url.openConnection() as? HttpURLConnection) ?: return@withContext null
                connection.requestMethod = "HEAD"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.instanceFollowRedirects = false
                val code = connection.responseCode
                val elapsed = System.currentTimeMillis() - start
                if (code in 200..399 || code == 403 || code == 412) {
                    elapsed
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                try {
                    connection?.disconnect()
                } catch (_: Exception) {
                }
            }
        }

    /**
     * 生成完整的网络诊断报告。
     *
     * @param context Context
     * @param currentCdnUrl 当前播放器使用的视频/音频 CDN 地址
     */
    suspend fun runDiagnostics(
        context: Context,
        currentCdnUrl: String? = null,
    ): NetworkDiagnosticReport =
        withContext(Dispatchers.IO) {
            val type = getNetworkType(context)
            val ip = getLocalIpAddress()
            val apiLatency = probeLatency("https://api.bilibili.com/x/web-interface/nav", 3000)
            val cdnLatency = currentCdnUrl?.let { probeLatency(it, 3000) }

            NetworkDiagnosticReport(
                networkType = type,
                localIp = ip,
                apiLatencyMs = apiLatency,
                cdnLatencyMs = cdnLatency,
            )
        }
}
