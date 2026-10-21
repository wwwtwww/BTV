package dev.frost819.newbv.app.ui.component.settings

import dev.frost819.newbv.app.network.entity.GithubRelease

/**
 * 从 GitHub Release 附件中安全匹配最符合安装规范的 APK 附件。
 *
 * 匹配策略（优先匹配正式 Release APK，兼容新旧前缀，兜底任意 APK）：
 * 1. 优先匹配以 BTV / newBV / BV 开头且包含 release 并以 .apk 结尾的文件；
 * 2. 匹配以 BTV / newBV / BV 开头且以 .apk 结尾的文件；
 * 3. 兜底匹配任意以 .apk 结尾的文件；
 * 4. 若无 APK 附件则返回 null，避免抛出 NoSuchElementException。
 */
fun GithubRelease.findApkAsset(): GithubRelease.Asset? =
    assets.firstOrNull { asset ->
        val name = asset.name
        (name.startsWith("BTV", ignoreCase = true) ||
            name.startsWith("newBV", ignoreCase = true) ||
            name.startsWith("BV", ignoreCase = true)) &&
            name.contains("release", ignoreCase = true) &&
            name.endsWith(".apk", ignoreCase = true)
    } ?: assets.firstOrNull { asset ->
        val name = asset.name
        (name.startsWith("BTV", ignoreCase = true) ||
            name.startsWith("newBV", ignoreCase = true) ||
            name.startsWith("BV", ignoreCase = true)) &&
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
