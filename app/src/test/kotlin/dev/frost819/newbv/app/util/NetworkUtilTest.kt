package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * [NetworkUtil] 与相关数据模型的单元测试。
 *
 * 验证连接类型描述、诊断数据封装以及延迟探测与 IP 获取的容错逻辑。
 */
class NetworkUtilTest {
    @Test
    fun `networkType display names are human-readable`() {
        assertThat(NetworkType.Wifi.displayName).contains("Wi-Fi")
        assertThat(NetworkType.Ethernet.displayName).contains("以太网")
        assertThat(NetworkType.Cellular.displayName).contains("移动蜂窝")
        assertThat(NetworkType.Other.displayName).contains("其他")
        assertThat(NetworkType.Disconnected.displayName).contains("未连接")
    }

    @Test
    fun `networkDiagnosticReport retains all properties`() {
        val report =
            NetworkDiagnosticReport(
                networkType = NetworkType.Wifi,
                localIp = "192.168.1.50",
                apiLatencyMs = 28L,
                cdnLatencyMs = 35L,
            )

        assertThat(report.networkType).isEqualTo(NetworkType.Wifi)
        assertThat(report.localIp).isEqualTo("192.168.1.50")
        assertThat(report.apiLatencyMs).isEqualTo(28L)
        assertThat(report.cdnLatencyMs).isEqualTo(35L)
    }

    @Test
    fun `getLocalIpAddress returns valid ipv4 or null without throwing`() {
        val ip = NetworkUtil.getLocalIpAddress()
        if (ip != null) {
            val parts = ip.split(".")
            assertThat(parts).hasSize(4)
            parts.forEach { part ->
                val num = part.toIntOrNull()
                assertThat(num).isNotNull()
                assertThat(num).isIn(0..255)
            }
        }
    }

    @Test
    fun `probeLatency returns null for unreachable host`() =
        runTest {
            // 192.0.2.0/24 为 TEST-NET-1 保留网段，不可达且应在超时后返回 null
            val latency = NetworkUtil.probeLatency(targetUrl = "http://192.0.2.1:81/unreachable", timeoutMs = 200)
            assertThat(latency).isNull()
        }
}
