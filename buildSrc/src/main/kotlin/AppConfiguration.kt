/**
 * 应用全局配置。
 *
 * 集中管理包名、SDK 版本、版本号等构建配置，
 * 供各模块 build.gradle.kts 引用。
 */
object AppConfiguration {
    const val appId = "dev.frost819.newbv"
    const val applicationId = "dev.frost819.btv"
    const val compileSdk = 36
    const val minSdk = 21
    const val targetSdk = 36

    private const val major = 0
    private const val minor = 4
    private const val patch = 10
    private const val hotFix = 0

    @Suppress("KotlinConstantConditions")
    val versionName: String
        get() {
            val base =
                System.getenv("BTV_VERSION_BASE")?.takeIf { it.isNotBlank() }
                    ?: System.getenv("NEWBV_VERSION_BASE")?.takeIf { it.isNotBlank() }
                    ?: "$major.$minor.$patch${".$hotFix".takeIf { hotFix != 0 } ?: ""}"
            val commitHash = runCatching {
                "git rev-list HEAD --abbrev-commit --max-count=1".exec().takeIf { it.isNotBlank() }
            }.getOrNull() ?: "nogit"
            return "$base.r$versionCode.$commitHash"
        }

    val versionCode: Int
        get() {
            val injectedCode = System.getenv("BTV_VERSION_CODE")?.toIntOrNull()
                ?: System.getenv("VERSION_CODE")?.toIntOrNull()
                ?: System.getProperty("btv.versionCode")?.toIntOrNull()
            if (injectedCode != null && injectedCode > 0) return injectedCode

            val gitCount = runCatching {
                "git rev-list --count HEAD".exec().toIntOrNull()
            }.getOrNull()
            if (gitCount != null && gitCount > 0) return gitCount

            // 兜底基础版本号计算：无 .git 历史环境时采用小基值（如 100 + minor*10 + patch），
            // 避免产出过大的 versionCode 锁死后续 CI 正常基于 git commit count 的 OTA 覆盖升级链。
            // 【注意】：无 git 环境构建仅供本地调试使用，生产 OTA 发布必须具有完整 git 历史或显式注入 BTV_VERSION_CODE。
            return (100 + (minor * 10) + patch).coerceAtLeast(100)
        }
}

fun String.exec() = runCatching {
    String(Runtime.getRuntime().exec(this).inputStream.readBytes()).trim()
}.getOrDefault("")
