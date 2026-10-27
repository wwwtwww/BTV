package dev.frost819.newbv.app.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release 数据模型。
 *
 * 对应 `GET /repos/{owner}/{repo}/releases/latest` 响应。
 *
 * @property assets 附件列表（APK 等）。
 * @property body Release Notes 正文。
 * @property name Release 标题。
 * @property tagName Git 标签名。
 * @property prerelease 是否为预发布。
 */
@Serializable
data class GithubRelease(
    val assets: List<Asset>,
    @SerialName("assets_url")
    val assetsUrl: String,
    val body: String,
    @SerialName("created_at")
    val createdAt: String,
    val draft: Boolean,
    @SerialName("html_url")
    val htmlUrl: String,
    val id: Int,
    val name: String,
    val prerelease: Boolean,
    @SerialName("published_at")
    val publishedAt: String,
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("tarball_url")
    val tarballUrl: String,
    @SerialName("target_commitish")
    val targetCommitish: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    val url: String,
    @SerialName("zipball_url")
    val zipballUrl: String,
) {
    val isPreRelease: Boolean get() = prerelease

    /**
     * Release 附件（APK 文件等）。
     *
     * @property browserDownloadUrl 直链下载地址。
     * @property name 文件名。
     * @property size 文件大小（字节）。
     * @property contentType MIME 类型。
     */
    @Serializable
    data class Asset(
        @SerialName("browser_download_url")
        val browserDownloadUrl: String,
        @SerialName("content_type")
        val contentType: String,
        @SerialName("download_count")
        val downloadCount: Int,
        val id: Int,
        val name: String,
        val size: Int,
        val state: String,
        val url: String,
    )
}

/**
 * 从 GitHub Release 附件中安全匹配最符合安装规范的 APK 附件。
 *
 * 匹配策略（严格限定 `.apk` 后缀，杜绝非 APK 资产）：
 * 1. 优先匹配以 BTV / newBV / BV 开头且包含 release 并以 .apk 结尾的文件；
 * 2. 兼容匹配以 BTV / newBV / BV 开头且以 .apk 结尾的文件；
 * 3. 兜底匹配任意以 .apk 结尾的文件；
 * 4. 若无 APK 附件则返回 null，避免抛出 NoSuchElementException。
 */
fun GithubRelease.findApkAsset(): GithubRelease.Asset? =
    assets.firstOrNull { asset ->
        val name = asset.name
        (
            name.startsWith("BTV", ignoreCase = true) ||
                name.startsWith("newBV", ignoreCase = true) ||
                name.startsWith("BV", ignoreCase = true)
        ) &&
            name.contains("release", ignoreCase = true) &&
            name.endsWith(".apk", ignoreCase = true)
    } ?: assets.firstOrNull { asset ->
        val name = asset.name
        (
            name.startsWith("BTV", ignoreCase = true) ||
                name.startsWith("newBV", ignoreCase = true) ||
                name.startsWith("BV", ignoreCase = true)
        ) &&
            name.endsWith(".apk", ignoreCase = true)
    } ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

/**
 * 解析 Release 对应的版本代码 (versionCode)。
 *
 * 优先从 APK 文件名（如 `BTV_3_v0.3.0_release.apk`）中解析下划线第二段为整数。
 * 兜底尝试从 tag_name（如 `v3`）或去掉前缀的字符串中提取整数。
 */
fun GithubRelease.parseVersionCode(): Int? {
    val apkAsset = findApkAsset()
    if (apkAsset != null) {
        val parts = apkAsset.name.substringBeforeLast(".apk").split("_")
        if (parts.size >= 2) {
            val code = parts[1].toIntOrNull()
            if (code != null) return code
        }
    }
    return tagName.removePrefix("v").toIntOrNull()
}

/**
 * 比较两个语义版本字符串（如 "v0.3.0" 与 "0.2.9"）。
 *
 * @return 若 [remote] 大于 [local] 返回正数；相等返回 0；小于返回负数；若无法解析为标准数字语义格式则返回 null。
 */
fun compareSemanticVersions(
    remote: String,
    local: String,
): Int? {
    fun cleanVersion(v: String): List<Int>? {
        val s = v.trim().removePrefix("v").substringBefore("-")
        val segments =
            s
                .split(".")
                .takeWhile { part -> part.isNotEmpty() && part.all { it.isDigit() } }
        val numericParts = segments.map { it.toIntOrNull() ?: return null }
        return if (numericParts.isEmpty()) null else numericParts
    }

    val remoteParts = cleanVersion(remote) ?: return null
    val localParts = cleanVersion(local) ?: return null

    val maxLen = maxOf(remoteParts.size, localParts.size)
    for (i in 0 until maxLen) {
        val r = remoteParts.getOrNull(i) ?: 0
        val l = localParts.getOrNull(i) ?: 0
        if (r != l) {
            return r.compareTo(l)
        }
    }
    return 0
}
