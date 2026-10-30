package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.network.entity.GithubRelease
import org.junit.jupiter.api.Test
import java.io.File

/**
 * [ReleaseNotesParser] 的单元测试。
 *
 * 验证版本号标签提取、Markdown 清洗（剥离 `Full Changelog` 裸链接）、
 * `CHANGELOG.md` 章节解析（跳过 Unreleased）、跨多版本升级说明聚合及历史空内容回退补全逻辑。
 */
class ReleaseNotesParserTest {
    private fun fakeRelease(
        tagName: String,
        versionCode: Int,
        body: String,
        name: String = "BTV $tagName",
        publishedAt: String = "2026-10-27T12:00:00Z",
        prerelease: Boolean = false,
        draft: Boolean = false,
    ): GithubRelease {
        val apkName = "BTV_${versionCode}_${tagName.removePrefix("v")}_release.apk"
        return GithubRelease(
            assets =
                listOf(
                    GithubRelease.Asset(
                        id = versionCode,
                        name = apkName,
                        size = 25_000_000,
                        browserDownloadUrl = "https://github.com/wwwtwww/BTV/releases/download/$tagName/$apkName",
                        contentType = "application/vnd.android.package-archive",
                        downloadCount = 10,
                        state = "uploaded",
                        url = "https://api.github.com/assets/$versionCode",
                    ),
                ),
            assetsUrl = "",
            body = body,
            createdAt = publishedAt,
            draft = draft,
            htmlUrl = "https://github.com/wwwtwww/BTV/releases/tag/$tagName",
            id = versionCode,
            name = name,
            prerelease = prerelease,
            publishedAt = publishedAt,
            tagName = tagName,
            tarballUrl = "",
            targetCommitish = "main",
            uploadUrl = "",
            url = "https://api.github.com/releases/$versionCode",
            zipballUrl = "",
        )
    }

    @Test
    fun `formatBaseVersion extracts clean vX_Y_Z from build version strings`() {
        // Given & When & Then
        assertThat(ReleaseNotesParser.formatBaseVersion("0.4.11.r193.198cb3d")).isEqualTo("v0.4.11")
        assertThat(ReleaseNotesParser.formatBaseVersion("v0.4.12")).isEqualTo("v0.4.12")
        assertThat(ReleaseNotesParser.formatBaseVersion("0.4.12-beta1")).isEqualTo("v0.4.12")
        assertThat(ReleaseNotesParser.formatBaseVersion("")).isEmpty()
    }

    @Test
    fun `extractDate parses ISO-8601 timestamp prefix`() {
        // Given & When & Then
        assertThat(ReleaseNotesParser.extractDate("2026-10-27T11:17:16Z")).isEqualTo("2026-10-27")
        assertThat(ReleaseNotesParser.extractDate("2026-10-28")).isEqualTo("2026-10-28")
        assertThat(ReleaseNotesParser.extractDate(null)).isNull()
        assertThat(ReleaseNotesParser.extractDate("invalid")).isNull()
    }

    @Test
    fun `parseMarkdownBlocks strips Full Changelog link and formats headers and bullets`() {
        // Given
        val raw =
            """
            ### ✨ 新增功能
            - **更新说明**：支持升级前查看 `CHANGELOG` 与 [发布详情](https://example.com)
            ---
            **Full Changelog**: https://github.com/wwwtwww/BTV/compare/v0.4.10...v0.4.11
            """.trimIndent()

        // When
        val blocks = ReleaseNotesParser.parseMarkdownBlocks(raw)

        // Then
        assertThat(blocks)
            .containsExactly(
                ReleaseNoteBlock.SectionHeader("✨ 新增功能"),
                ReleaseNoteBlock.BulletItem("更新说明：支持升级前查看 CHANGELOG 与 发布详情"),
            ).inOrder()
    }

    @Test
    fun `parseMarkdownBlocks returns empty list when body only has Full Changelog link`() {
        // Given
        val raw = "**Full Changelog**: https://github.com/wwwtwww/BTV/compare/v0.4.10...v0.4.11"

        // When
        val blocks = ReleaseNotesParser.parseMarkdownBlocks(raw)

        // Then
        assertThat(blocks).isEmpty()
    }

    @Test
    fun `parseChangelogMarkdown skips Unreleased and parses version sections`() {
        // Given
        val sampleChangelog =
            """
            # 更新日志

            ## [Unreleased]
            ### ✨ 新增功能
            - 未发布的开发内容

            ## [v0.4.11] - 2026-10-27
            ### 🔧 优化与调整
            - 真机 OTA 验证版本

            ## [v0.4.10] - 2026-10-26
            ### 🐛 问题修复
            - 修复低配大屏 ANR 风险
            """.trimIndent()

        // When
        val entries = ReleaseNotesParser.parseChangelogMarkdown(sampleChangelog)

        // Then
        assertThat(entries).hasSize(2)
        assertThat(entries[0].versionTag).isEqualTo("v0.4.11")
        assertThat(entries[0].publishedDate).isEqualTo("2026-10-27")
        assertThat(entries[1].versionTag).isEqualTo("v0.4.10")
    }

    @Test
    fun `evaluateUpgrade aggregates intermediate versions and enriches empty GitHub bodies`() {
        // Given: 用户当前在 v0.4.9 (code=190)，远程有 v0.4.12(195)、v0.4.11(193)、v0.4.10(192)、v0.4.9(190) 及一个预发布版
        val bundledChangelog =
            """
            ## [v0.4.11] - 2026-10-27
            - 补齐自内置文档的 v0.4.11 说明

            ## [v0.4.10] - 2026-10-27
            - 补齐自内置文档的 v0.4.10 说明
            """.trimIndent()

        val releases =
            listOf(
                fakeRelease(
                    tagName = "v0.4.13-beta",
                    versionCode = 199,
                    body = "- 预发布版本不应纳入正式升级",
                    prerelease = true,
                ),
                fakeRelease(
                    tagName = "v0.4.12",
                    versionCode = 195,
                    body = "### ✨ 新增功能\n- 支持升级前展示详细更新说明",
                    publishedAt = "2026-10-28T08:00:00Z",
                ),
                fakeRelease(
                    tagName = "v0.4.11",
                    versionCode = 193,
                    body = "**Full Changelog**: https://github.com/wwwtwww/BTV/compare/v0.4.10...v0.4.11",
                ),
                fakeRelease(
                    tagName = "v0.4.10",
                    versionCode = 192,
                    body = "**Full Changelog**: https://github.com/wwwtwww/BTV/compare/v0.4.9...v0.4.10",
                ),
                fakeRelease(
                    tagName = "v0.4.9",
                    versionCode = 190,
                    body = "- 已安装的当前版本",
                ),
            )

        // When
        val result =
            ReleaseNotesParser.evaluateUpgrade(
                releases = releases,
                currentVersionCode = 190,
                currentVersionName = "0.4.9.r190.b139faf",
                bundledChangelog = bundledChangelog,
            )

        // Then: 按新到旧合并 v0.4.12、v0.4.11、v0.4.10 共 3 个正式版，且 v0.4.11/v0.4.10 自动从内置 CHANGELOG 补全正文
        assertThat(result.latestRelease?.tagName).isEqualTo("v0.4.12")
        assertThat(result.notes.map { it.versionTag }).containsExactly("v0.4.12", "v0.4.11", "v0.4.10").inOrder()
        assertThat(result.notes[1].blocks).containsExactly(
            ReleaseNoteBlock.BulletItem("补齐自内置文档的 v0.4.11 说明"),
        )
    }

    @Test
    fun `evaluateUpgrade returns null latestRelease when already up to date`() {
        // Given
        val releases =
            listOf(
                fakeRelease(
                    tagName = "v0.4.11",
                    versionCode = 193,
                    body = "- 最新版本",
                ),
            )

        // When
        val result =
            ReleaseNotesParser.evaluateUpgrade(
                releases = releases,
                currentVersionCode = 193,
                currentVersionName = "0.4.11.r193.198cb3d",
            )

        // Then
        assertThat(result.latestRelease).isNull()
        assertThat(result.notes).isEmpty()
    }

    @Test
    fun `resolveLocalVersionNotes matches current version and supports full history mode`() {
        // Given
        val changelog =
            """
            ## [v0.4.11] - 2026-10-27
            - 当前版本说明

            ## [v0.4.10] - 2026-10-26
            - 上一版本说明
            """.trimIndent()

        // When
        val currentOnly =
            ReleaseNotesParser.resolveLocalVersionNotes(
                currentVersionName = "0.4.11.r193.198cb3d",
                bundledChangelog = changelog,
                includeHistory = false,
            )
        val fullHistory =
            ReleaseNotesParser.resolveLocalVersionNotes(
                currentVersionName = "0.4.11.r193.198cb3d",
                bundledChangelog = changelog,
                includeHistory = true,
            )

        // Then
        assertThat(currentOnly).hasSize(1)
        assertThat(currentOnly[0].versionTag).isEqualTo("v0.4.11")
        assertThat(fullHistory).hasSize(2)
    }

    @Test
    fun `repository root CHANGELOG_md parses cleanly with historical releases`() {
        // Given: 读取仓库根目录真实的 CHANGELOG.md
        val rootChangelog =
            listOf(File("../CHANGELOG.md"), File("CHANGELOG.md"))
                .firstOrNull { it.exists() }
        assertThat(rootChangelog).isNotNull()

        // When
        val entries =
            ReleaseNotesParser.parseChangelogMarkdown(
                requireNotNull(rootChangelog).readText(Charsets.UTF_8),
            )

        // Then
        assertThat(entries).isNotEmpty()
        assertThat(entries.first().versionTag).isEqualTo("v0.5.0")
        assertThat(entries.last().versionTag).isEqualTo("v0.1.0")
    }

    @Test
    fun `resolveLocalVersionNotes returns emptyList when current version not found in changelog or remote`() {
        // Given
        val changelog =
            """
            ## [v0.4.11] - 2026-10-27
            - 某些更新说明
            """.trimIndent()

        // When
        val result =
            ReleaseNotesParser.resolveLocalVersionNotes(
                currentVersionName = "0.9.99-unknown",
                bundledChangelog = changelog,
                remoteReleases = emptyList(),
                includeHistory = false,
            )

        // Then
        assertThat(result).isEmpty()
    }
}
