package dev.frost819.newbv.app.util

import android.content.Context
import dev.frost819.newbv.app.network.entity.GithubRelease
import dev.frost819.newbv.app.network.entity.compareSemanticVersions
import dev.frost819.newbv.app.network.entity.parseVersionCode

/**
 * 结构化更新说明文本块。
 *
 * 将原始 Markdown 更新说明清洗拆分为适合 Android TV 大屏排版的元素。
 */
sealed interface ReleaseNoteBlock {
    /**
     * 分类小标题（例如「✨ 新增功能」「🐛 问题修复」）。
     *
     * @property text 去除 `#` 标记后的标题文本。
     */
    data class SectionHeader(
        val text: String,
    ) : ReleaseNoteBlock

    /**
     * 列表条目（例如 `- 修复某问题`）。
     *
     * @property text 去除列表符号与行内 Markdown 标记后的正文。
     */
    data class BulletItem(
        val text: String,
    ) : ReleaseNoteBlock

    /**
     * 普通说明段落。
     *
     * @property text 清洗后的纯文本内容。
     */
    data class Paragraph(
        val text: String,
    ) : ReleaseNoteBlock
}

/**
 * 单个版本的更新说明条目。
 *
 * @property versionTag 规范化版本标签（例如 `v0.4.11`）。
 * @property title 版本展示标题（例如 `BTV v0.4.11`）。
 * @property publishedDate 发布日期（`YYYY-MM-DD`，未知时为 `null`）。
 * @property blocks 结构化内容块列表。
 */
data class ReleaseNoteEntry(
    val versionTag: String,
    val title: String,
    val publishedDate: String?,
    val blocks: List<ReleaseNoteBlock>,
)

/**
 * 远程版本更新评估结果。
 *
 * @property latestRelease 最新可升级的远程 [GithubRelease]；无可用更新时为 `null`。
 * @property notes 从最新目标版本到当前已安装版本之间所有待升级正式版的聚合更新说明（按新到旧排序）。
 */
data class UpgradeCheckResult(
    val latestRelease: GithubRelease?,
    val notes: List<ReleaseNoteEntry>,
)

/**
 * OTA 更新说明解析与跨版本聚合工具。
 *
 * 负责：
 * 1. 清洗 GitHub Release Markdown 正文（剥离无意义的 `Full Changelog` 裸链接与冗余符号）；
 * 2. 解析内置 `CHANGELOG.md` 资产，在历史 GitHub Release 缺少正文或离线查看时提供完整中文更新说明；
 * 3. 当用户跨多个版本升级（例如 `v0.4.8 -> v0.4.12`）时，自动按新到旧合并中间所有正式版的变更内容。
 */
object ReleaseNotesParser {
    private val fullChangelogRegex =
        Regex("""^\s*\*{0,2}Full Changelog\*{0,2}\s*:\s*\S+.*$""", RegexOption.IGNORE_CASE)
    private val horizontalRuleRegex = Regex("""^\s*([-*_])\1{2,}\s*$""")
    private val headingRegex = Regex("""^\s*#{1,6}\s+(.+?)\s*$""")
    private val bulletRegex = Regex("""^\s*(?:[-*+]|\d+\.)\s+(.+?)\s*$""")
    private val changelogVersionHeaderRegex =
        Regex("""^##\s+\[?([^\]\s-]+)\]?(?:\s*-\s*(\d{4}-\d{2}-\d{2}))?\s*$""")
    private val markdownLinkRegex = Regex("""\[([^\]]+)]\(([^)]+)\)""")
    private val boldAsteriskRegex = Regex("""\*\*(.+?)\*\*""")
    private val boldUnderscoreRegex = Regex("""__(.+?)__""")
    private val inlineCodeRegex = Regex("""`([^`]+)`""")
    private val isoDatePrefixRegex = Regex("""^(\d{4}-\d{2}-\d{2})""")

    /**
     * 将构建版本名（如 `0.4.11.r193.198cb3d` 或 `v0.4.11`）提取为简洁版本标签（如 `v0.4.11`）。
     *
     * @param versionName 原始版本字符串。
     * @return 规范化的 `vX.Y.Z` 标签；若无法解析数字段则返回原字符串。
     */
    fun formatBaseVersion(versionName: String): String {
        val trimmed = versionName.trim()
        if (trimmed.isEmpty()) return ""
        val core = trimmed.removePrefix("v").removePrefix("V").substringBefore("-")
        val numericSegments =
            core
                .split(".")
                .takeWhile { segment -> segment.isNotEmpty() && segment.all { it.isDigit() } }
        return if (numericSegments.isNotEmpty()) {
            "v${numericSegments.joinToString(".")}"
        } else {
            trimmed
        }
    }

    /**
     * 从 ISO-8601 时间戳（如 `2026-10-27T11:17:16Z`）中提取 `YYYY-MM-DD` 日期字符串。
     *
     * @param rawTimestamp 原始时间字符串。
     * @return 提取出的日期；格式不匹配时返回 `null`。
     */
    fun extractDate(rawTimestamp: String?): String? {
        if (rawTimestamp.isNullOrBlank()) return null
        return isoDatePrefixRegex.find(rawTimestamp.trim())?.groupValues?.getOrNull(1)
    }

    /**
     * 清理单行文本中的行内 Markdown 标记（链接、粗体、反引号），保留可读正文。
     *
     * @param text 原始单行文本。
     * @return 适合大屏直接展示的干净文本。
     */
    fun cleanInlineMarkdown(text: String): String =
        text
            .replace(markdownLinkRegex, "$1")
            .replace(boldAsteriskRegex, "$1")
            .replace(boldUnderscoreRegex, "$1")
            .replace(inlineCodeRegex, "$1")
            .trim()

    /**
     * 将单版本 Markdown 正文解析为结构化的 [ReleaseNoteBlock] 列表。
     *
     * 会自动剔除 GitHub 自动生成的 `Full Changelog: https://...` 裸链接与水平分割线。
     *
     * @param rawMarkdown 原始 Release 正文或 Changelog 章节正文。
     * @return 解析后的结构化块列表。
     */
    fun parseMarkdownBlocks(rawMarkdown: String): List<ReleaseNoteBlock> {
        if (rawMarkdown.isBlank()) return emptyList()
        return buildList {
            for (rawLine in rawMarkdown.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                if (fullChangelogRegex.matches(line)) continue
                if (horizontalRuleRegex.matches(line)) continue
                if (line.startsWith(">")) continue

                val headingMatch = headingRegex.matchEntire(line)
                if (headingMatch != null) {
                    val headerText = cleanInlineMarkdown(headingMatch.groupValues[1])
                    if (headerText.isNotEmpty()) {
                        add(ReleaseNoteBlock.SectionHeader(headerText))
                    }
                    continue
                }

                val bulletMatch = bulletRegex.matchEntire(line)
                if (bulletMatch != null) {
                    val bulletText = cleanInlineMarkdown(bulletMatch.groupValues[1])
                    if (bulletText.isNotEmpty()) {
                        add(ReleaseNoteBlock.BulletItem(bulletText))
                    }
                    continue
                }

                val paragraphText = cleanInlineMarkdown(line)
                if (paragraphText.isNotEmpty()) {
                    add(ReleaseNoteBlock.Paragraph(paragraphText))
                }
            }
        }
    }

    /**
     * 解析完整 `CHANGELOG.md` 文本，提取所有已发布版本的 [ReleaseNoteEntry] 列表。
     *
     * 会自动跳过尚未发布的 `[Unreleased]` 草稿章节。
     *
     * @param changelogText `CHANGELOG.md` 全文。
     * @return 按文档顺序（由新到旧）排列的版本说明列表。
     */
    fun parseChangelogMarkdown(changelogText: String): List<ReleaseNoteEntry> {
        if (changelogText.isBlank()) return emptyList()

        data class RawSection(
            val rawVersion: String,
            val date: String?,
            val bodyBuilder: StringBuilder = StringBuilder(),
        )

        val sections = mutableListOf<RawSection>()
        var current: RawSection? = null

        for (line in changelogText.lineSequence()) {
            val headerMatch = changelogVersionHeaderRegex.matchEntire(line.trim())
            if (headerMatch != null) {
                current?.let { sections.add(it) }
                val versionToken = headerMatch.groupValues[1].trim()
                val dateToken = headerMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
                current = RawSection(rawVersion = versionToken, date = dateToken)
            } else {
                current?.bodyBuilder?.appendLine(line)
            }
        }
        current?.let { sections.add(it) }

        return sections.mapNotNull { section ->
            if (section.rawVersion.equals("Unreleased", ignoreCase = true)) {
                return@mapNotNull null
            }
            val normalizedTag = formatBaseVersion(section.rawVersion)
            val blocks = parseMarkdownBlocks(section.bodyBuilder.toString())
            if (normalizedTag.isEmpty() || blocks.isEmpty()) {
                null
            } else {
                ReleaseNoteEntry(
                    versionTag = normalizedTag,
                    title = "BTV $normalizedTag",
                    publishedDate = section.date,
                    blocks = blocks,
                )
            }
        }
    }

    /**
     * 判断某个远程 [GithubRelease] 是否为比当前已安装版本更新的正式版。
     *
     * 草稿（`draft`）与预发布（`prerelease`）版本不纳入常规自动升级范围。
     *
     * @param release 远程版本对象。
     * @param currentVersionCode 当前已安装应用的 `versionCode`。
     * @param currentVersionName 当前已安装应用的 `versionName`。
     * @return 若远程版本高于本地版本返回 `true`。
     */
    fun isRemoteReleaseNewer(
        release: GithubRelease,
        currentVersionCode: Int,
        currentVersionName: String,
    ): Boolean {
        if (release.draft || release.prerelease) return false
        val targetCode = release.parseVersionCode()
        return if (targetCode != null) {
            targetCode > currentVersionCode
        } else {
            val semVerDiff = compareSemanticVersions(release.tagName, currentVersionName)
            semVerDiff != null && semVerDiff > 0
        }
    }

    /**
     * 评估远程版本列表并构建跨版本聚合更新说明。
     *
     * 当用户落后多个版本（如 `v0.4.8 -> v0.4.12`）时，会将中间所有正式版按版本号从新到旧聚合；
     * 若某个历史远程 Release 的 `body` 仅含 `Full Changelog` 链接，会自动回退匹配内置 `CHANGELOG.md` 补全说明。
     *
     * @param releases 从 GitHub Releases 接口拉取的列表。
     * @param currentVersionCode 本地 `versionCode`。
     * @param currentVersionName 本地 `versionName`。
     * @param bundledChangelog 应用内置 `CHANGELOG.md` 全文（可选兜底）。
     * @return 包含最新目标 Release 与聚合更新说明的 [UpgradeCheckResult]。
     */
    fun evaluateUpgrade(
        releases: List<GithubRelease>,
        currentVersionCode: Int,
        currentVersionName: String,
        bundledChangelog: String = "",
    ): UpgradeCheckResult {
        val newerReleases =
            releases
                .filter { isRemoteReleaseNewer(it, currentVersionCode, currentVersionName) }
                .sortedWith { a, b ->
                    val codeA = a.parseVersionCode()
                    val codeB = b.parseVersionCode()
                    when {
                        codeA != null && codeB != null && codeA != codeB -> codeB.compareTo(codeA)
                        else -> (compareSemanticVersions(b.tagName, a.tagName) ?: 0)
                    }
                }

        val latest = newerReleases.firstOrNull() ?: return UpgradeCheckResult(null, emptyList())
        val bundledMap = parseChangelogMarkdown(bundledChangelog).associateBy { it.versionTag }

        val notes =
            newerReleases.map { release ->
                val normTag = formatBaseVersion(release.tagName)
                val remoteBlocks = parseMarkdownBlocks(release.body)
                val fallbackEntry = bundledMap[normTag]
                val resolvedBlocks =
                    when {
                        remoteBlocks.isNotEmpty() -> remoteBlocks
                        fallbackEntry != null && fallbackEntry.blocks.isNotEmpty() -> fallbackEntry.blocks
                        else -> {
                            val fallbackText =
                                release.name
                                    .takeIf { it.isNotBlank() && it != release.tagName }
                                    ?: "常规稳定性优化与问题修复"
                            listOf(ReleaseNoteBlock.BulletItem(fallbackText))
                        }
                    }

                ReleaseNoteEntry(
                    versionTag = normTag,
                    title = release.name.ifBlank { "BTV $normTag" },
                    publishedDate = extractDate(release.publishedAt) ?: fallbackEntry?.publishedDate,
                    blocks = resolvedBlocks,
                )
            }

        return UpgradeCheckResult(
            latestRelease = latest,
            notes = notes,
        )
    }

    /**
     * 获取本地当前版本或完整历史版本的更新说明（用于「无可用更新」页或「关于 -> 版本更新说明」离线查阅）。
     *
     * @param currentVersionName 本地 `BuildConfig.VERSION_NAME`。
     * @param bundledChangelog 应用内置 `CHANGELOG.md` 文本。
     * @param remoteReleases 可选的已拉取远程版本列表（当内置文档未命中时作补充）。
     * @param includeHistory 为 `true` 时返回包含历史版本在内的完整列表；为 `false` 时仅返回当前版本说明。
     * @return 匹配的 [ReleaseNoteEntry] 列表。
     */
    fun resolveLocalVersionNotes(
        currentVersionName: String,
        bundledChangelog: String,
        remoteReleases: List<GithubRelease> = emptyList(),
        includeHistory: Boolean = false,
    ): List<ReleaseNoteEntry> {
        val bundledEntries = parseChangelogMarkdown(bundledChangelog)
        if (includeHistory) {
            if (bundledEntries.isNotEmpty()) return bundledEntries
            return remoteReleases
                .filter { !it.draft && !it.prerelease }
                .mapNotNull { release ->
                    val blocks = parseMarkdownBlocks(release.body)
                    if (blocks.isEmpty()) {
                        null
                    } else {
                        val normTag = formatBaseVersion(release.tagName)
                        ReleaseNoteEntry(
                            versionTag = normTag,
                            title = release.name.ifBlank { "BTV $normTag" },
                            publishedDate = extractDate(release.publishedAt),
                            blocks = blocks,
                        )
                    }
                }
        }

        val currentTag = formatBaseVersion(currentVersionName)
        bundledEntries.firstOrNull { it.versionTag == currentTag }?.let { return listOf(it) }

        remoteReleases
            .firstOrNull { !it.draft && formatBaseVersion(it.tagName) == currentTag }
            ?.let { remote ->
                val blocks = parseMarkdownBlocks(remote.body)
                if (blocks.isNotEmpty()) {
                    return listOf(
                        ReleaseNoteEntry(
                            versionTag = currentTag,
                            title = remote.name.ifBlank { "BTV $currentTag" },
                            publishedDate = extractDate(remote.publishedAt),
                            blocks = blocks,
                        ),
                    )
                }
            }

        return bundledEntries.take(1)
    }

    /**
     * 安全读取应用 APK 内置的 `CHANGELOG.md` 资产文本。
     *
     * @param context Android 上下文。
     * @return `CHANGELOG.md` 文本内容；资产不存在或读取异常时返回空字符串。
     */
    fun readBundledChangelog(context: Context): String =
        runCatching {
            context.assets
                .open("CHANGELOG.md")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }.getOrDefault("")
}
