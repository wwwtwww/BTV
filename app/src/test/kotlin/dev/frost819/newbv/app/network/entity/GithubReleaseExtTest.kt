package dev.frost819.newbv.app.network.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [GithubRelease] 资产匹配与版本号解析的单元测试。
 *
 * 验证对当前 BTV 产物命名、旧版 newBV / BV 产物命名的安全匹配，
 * 确保即使附件命名格式异常或包含同名的非 APK 文件（如 zip），也绝不会误选或抛异常。
 */
class GithubReleaseExtTest {

    private fun createRelease(
        tagName: String = "v0.3.0",
        assetNames: List<String> = emptyList(),
    ): GithubRelease =
        GithubRelease(
            assets = assetNames.mapIndexed { index, name ->
                GithubRelease.Asset(
                    id = index,
                    name = name,
                    size = 1024,
                    browserDownloadUrl = "https://example.com/$name",
                    contentType = if (name.endsWith(".apk")) {
                        "application/vnd.android.package-archive"
                    } else {
                        "application/octet-stream"
                    },
                    downloadCount = 0,
                    state = "uploaded",
                    url = "https://api.github.com/assets/$index",
                )
            },
            assetsUrl = "",
            body = "Release description",
            createdAt = "",
            draft = false,
            htmlUrl = "",
            id = 1,
            name = "Releases $tagName",
            prerelease = false,
            publishedAt = "",
            tagName = tagName,
            tarballUrl = "",
            targetCommitish = "main",
            uploadUrl = "",
            url = "",
            zipballUrl = "",
        )

    @Test
    fun `findApkAsset matches BTV release apk first`() {
        val release = createRelease(
            assetNames = listOf("mapping.txt", "BTV_3_v0.3.0_release.apk", "output-metadata.json"),
        )
        val apk = release.findApkAsset()

        assertThat(apk).isNotNull()
        assertThat(apk?.name).isEqualTo("BTV_3_v0.3.0_release.apk")
        assertThat(apk?.browserDownloadUrl).isEqualTo("https://example.com/BTV_3_v0.3.0_release.apk")
    }

    @Test
    fun `findApkAsset rejects non-apk asset even if containing release keyword`() {
        // 关键用例：防止名字包含 "release" 的 zip/tar.gz 资产被误选为更新包
        val release = createRelease(
            assetNames = listOf(
                "BTV_3_source_release.zip",
                "checksum_release.txt",
                "BTV_3_v0.3.0_release.apk",
            ),
        )
        val apk = release.findApkAsset()

        assertThat(apk).isNotNull()
        assertThat(apk?.name).isEqualTo("BTV_3_v0.3.0_release.apk")
        assertThat(apk?.browserDownloadUrl).isEqualTo("https://example.com/BTV_3_v0.3.0_release.apk")
    }

    @Test
    fun `findApkAsset matches legacy newBV and BV releases`() {
        val newBvRelease = createRelease(
            assetNames = listOf("newBV_2_v0.2.0_release.apk"),
        )
        assertThat(newBvRelease.findApkAsset()?.name).isEqualTo("newBV_2_v0.2.0_release.apk")

        val bvRelease = createRelease(
            assetNames = listOf("BV_1_v0.1.0_release.apk"),
        )
        assertThat(bvRelease.findApkAsset()?.name).isEqualTo("BV_1_v0.1.0_release.apk")
    }

    @Test
    fun `findApkAsset falls back to any apk when release keyword missing`() {
        val release = createRelease(
            assetNames = listOf("notes.md", "BTV-tv-arm64.apk"),
        )
        val apk = release.findApkAsset()

        assertThat(apk).isNotNull()
        assertThat(apk?.name).isEqualTo("BTV-tv-arm64.apk")
    }

    @Test
    fun `findApkAsset returns null safely when no apk present`() {
        val release = createRelease(
            assetNames = listOf("notes.txt", "source.zip", "checksums.sha256"),
        )
        assertThat(release.findApkAsset()).isNull()
    }

    @Test
    fun `parseVersionCode extracts integer code from asset filename`() {
        val release = createRelease(
            assetNames = listOf("BTV_3_v0.3.0_release.apk"),
        )
        assertThat(release.parseVersionCode()).isEqualTo(3)
    }

    @Test
    fun `parseVersionCode falls back to tag_name`() {
        val release = createRelease(
            tagName = "v5",
            assetNames = listOf("BTV-custom.apk"),
        )
        assertThat(release.parseVersionCode()).isEqualTo(5)
    }

    @Test
    fun `parseVersionCode returns null when no numeric code available`() {
        val release = createRelease(
            tagName = "v-unstable",
            assetNames = listOf("readme.txt"),
        )
        assertThat(release.parseVersionCode()).isNull()
    }
}
