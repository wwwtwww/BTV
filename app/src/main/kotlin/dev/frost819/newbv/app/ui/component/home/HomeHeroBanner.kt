package dev.frost819.newbv.app.ui.component.home

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.size.Precision
import dev.frost819.newbv.R
import dev.frost819.newbv.app.ui.component.videocard.VideoCardData
import dev.frost819.newbv.app.ui.component.videocard.isPgc
import dev.frost819.newbv.biliapi.entity.home.VideoBriefMeta
import dev.frost819.newbv.biliapi.entity.ugc.UgcItem
import dev.frost819.newbv.core.theme.BVColors
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTypography
import dev.frost819.newbv.core.theme.LocalIsDarkTheme
import dev.frost819.newbv.data.datastore.HomeHeroMode

/** Hero 看板背景与文案交叉淡入淡出时长（毫秒）。 */
const val HERO_CROSSFADE_MS = 300

/** 首页 Hero 看板容器测试标签。 */
const val HOME_HERO_BANNER_TAG = "home_hero_banner"

/** 首页 Hero 看板动态海报测试标签。 */
const val HOME_HERO_POSTER_TAG = "home_hero_poster"

/** 首页 Hero 看板静态降级底板测试标签。 */
const val HOME_HERO_STATIC_BACKDROP_TAG = "home_hero_static_backdrop"

/** 首页 Hero 看板静态省流角标测试标签。 */
const val HOME_HERO_STATIC_BADGE_TAG = "home_hero_static_badge"

/** 首页 Hero 看板分区标签测试标签。 */
const val HOME_HERO_CATEGORY_TAG = "home_hero_category"

/** 低配设备判定物理内存阈值（1.6 GB）。 */
private const val LOW_SPEC_TOTAL_RAM_BYTES = 1_600L * 1024L * 1024L

/** 低配设备判定 JVM 最大堆阈值（192 MB）。 */
private const val LOW_SPEC_MAX_HEAP_BYTES = 192L * 1024L * 1024L

/** Hero 海报最大解码宽度（像素，限流显存占用）。 */
private const val HERO_POSTER_DECODE_WIDTH = 960

/** Hero 海报最大解码高度（像素，限流显存占用）。 */
private const val HERO_POSTER_DECODE_HEIGHT = 540

/** B 站 BFS 图床 16:9 裁剪 WebP 后缀。 */
private const val BILI_HERO_COVER_SUFFIX = "@960w_540h_1c.webp"

/**
 * 首页 Hero Showcase 实际生效的渲染模式。
 */
enum class EffectiveHeroMode {
    /** 完整高清海报 + 300ms 交叉淡入淡出 + 按需补全分区与简介。 */
    FullDynamic,

    /** 低配/省流静态降级模式：不解码大图 Bitmap、关闭双缓冲动画、跳过后台详情请求。 */
    StaticDegraded,

    /** 完全隐藏顶部看板。 */
    Off,
}

/**
 * 首页 Hero Showcase 展示数据模型。
 *
 * @property avid 视频 AV 号。
 * @property bvid 视频 BV 号。
 * @property title 视频标题。
 * @property cover 封面 URL。
 * @property upName UP 主名称。
 * @property playString 播放量格式化文本。
 * @property danmakuString 弹幕数格式化文本。
 * @property timeString 时长格式化文本。
 * @property pubTime 发布时间文本。
 * @property tagName 分区标签（如「数码」「知识」）。
 * @property rcmdReason 推荐理由角标（如「百万播放」「飙升」）。
 * @property description 清洗后的单段视频简介（无有效简介时为 null）。
 * @property sourceShelfTitle 来源泳道标题（如「为你推荐」「今日热门」）。
 * @property isPgc 是否为番剧/影视条目。
 */
data class HeroShowcaseData(
    val avid: Long,
    val bvid: String = "",
    val title: String,
    val cover: String,
    val upName: String,
    val playString: String = "",
    val danmakuString: String = "",
    val timeString: String = "",
    val pubTime: String? = null,
    val tagName: String? = null,
    val rcmdReason: String? = null,
    val description: String? = null,
    val sourceShelfTitle: String = "",
    val isPgc: Boolean = false,
) {
    /**
     * 用于 UI 展示的简介或兜底导览文案。
     */
    val displaySummary: String
        get() =
            description?.takeIf { it.isNotBlank() }
                ?: if (isPgc) {
                    "番剧精选内容 · 按确认键即刻进入剧集播放，按菜单键查看更多操作"
                } else if (upName.isNotBlank()) {
                    "来自 $upName 的精选投稿 · 按确认键即刻播放，按菜单键打开快捷菜单"
                } else {
                    "精选大屏推荐内容 · 按确认键即刻播放"
                }

    /**
     * 合并按需查询到的轻量补充元数据 [VideoBriefMeta]。
     *
     * 仅在当前字段缺失时填充，避免覆盖列表自带的推荐理由或分区名。
     */
    fun mergeBriefMeta(meta: VideoBriefMeta): HeroShowcaseData =
        copy(
            tagName = tagName?.takeIf { it.isNotBlank() } ?: meta.tagName?.trim()?.takeIf { it.isNotBlank() },
            rcmdReason =
                rcmdReason?.takeIf { it.isNotBlank() }
                    ?: meta.rcmdReason?.trim()?.takeIf { it.isNotBlank() },
            description =
                description?.takeIf { it.isNotBlank() }
                    ?: normalizeHeroDescription(meta.description),
        )

    companion object {
        /**
         * 由 [VideoCardData] 构建 Hero 看板数据。
         *
         * @param card 视频卡片数据。
         * @param shelfTitle 来源泳道名称。
         */
        fun fromVideoCardData(
            card: VideoCardData,
            shelfTitle: String = "",
        ): HeroShowcaseData {
            val cleanTag =
                card.tagName?.trim()?.takeIf { it.isNotBlank() }
                    ?: if (card.isPgc) "番剧影视" else null
            return HeroShowcaseData(
                avid = card.avid,
                bvid = card.bvid,
                title = card.title.trim(),
                cover = card.cover,
                upName = card.upName.trim(),
                playString = card.playString,
                danmakuString = card.danmakuString,
                timeString = card.timeString,
                pubTime = card.pubTime?.trim()?.takeIf { it.isNotBlank() },
                tagName = cleanTag,
                rcmdReason = card.rcmdReason?.trim()?.takeIf { it.isNotBlank() },
                description = normalizeHeroDescription(card.description),
                sourceShelfTitle = shelfTitle.trim(),
                isPgc = card.isPgc,
            )
        }

        /**
         * 由 [UgcItem] 构建 Hero 看板数据。
         *
         * @param item UGC 视频条目。
         * @param shelfTitle 来源泳道名称。
         */
        fun fromUgcItem(
            item: UgcItem,
            shelfTitle: String = "",
        ): HeroShowcaseData = fromVideoCardData(VideoCardData.fromUgcItem(item), shelfTitle)
    }
}

/**
 * 清洗并规范化 B 站视频简介，适配大屏双行摘要排版。
 *
 * 将多行换行符折叠为间隔点，过滤无意义的纯横线（B 站空简介默认值 `-`）与空白串。
 *
 * @param raw 原始简介字符串。
 * @return 清洗后的简介文本，无有效内容时返回 null。
 */
fun normalizeHeroDescription(raw: String?): String? {
    if (raw == null) return null
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed == "-") return null
    val collapsed =
        trimmed
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "-" }
            .joinToString(" · ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    return collapsed.takeIf { it.isNotEmpty() }
}

/**
 * 根据用户偏好与设备内存能力解析实际生效的 Hero 渲染模式。
 *
 * @param mode 用户在设置中选择的 [HomeHeroMode]。
 * @param isLowSpecDevice 当前设备是否被识别为低内存/低配大屏设备。
 * @return 实际生效的 [EffectiveHeroMode]。
 */
fun resolveEffectiveHeroMode(
    mode: HomeHeroMode,
    isLowSpecDevice: Boolean,
): EffectiveHeroMode =
    when (mode) {
        HomeHeroMode.Auto ->
            if (isLowSpecDevice) {
                EffectiveHeroMode.StaticDegraded
            } else {
                EffectiveHeroMode.FullDynamic
            }

        HomeHeroMode.Full -> EffectiveHeroMode.FullDynamic
        HomeHeroMode.StaticLowSpec -> EffectiveHeroMode.StaticDegraded
        HomeHeroMode.Off -> EffectiveHeroMode.Off
    }

/**
 * 检测当前 Android TV 设备是否属于低内存/显存敏感型盒子。
 *
 * 满足任一条件即视为低配设备：
 * 1. 系统声明 [ActivityManager.isLowRamDevice]；
 * 2. 物理总内存 <= 1.6 GB；
 * 3. 应用可用 JVM 最大堆内存 <= 192 MB。
 *
 * @param context Android 上下文。
 * @return 低配设备返回 true，否则返回 false。
 */
fun isDeviceLowSpecForHero(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val isLowRamFlag = activityManager?.isLowRamDevice == true
    val memInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memInfo)
    val isLowPhysicalRam = memInfo.totalMem in 0L..LOW_SPEC_TOTAL_RAM_BYTES
    val isLowHeap = Runtime.getRuntime().maxMemory() <= LOW_SPEC_MAX_HEAP_BYTES
    return isLowRamFlag || isLowPhysicalRam || isLowHeap
}

/**
 * 将 B 站封面 URL 转换为适合 TV Hero 看板的 960x540 WebP 裁剪地址。
 *
 * 避免直接拉取 UP 主上传的数 MB 原图（如 4K PNG），大幅降低网络带宽与显存解码峰值。
 *
 * @param url 原始封面 URL。
 * @return 优化后的 HTTPS WebP 封面 URL。
 */
fun optimizeBiliHeroCoverUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return ""
    val httpsUrl =
        when {
            trimmed.startsWith("http://", ignoreCase = true) -> "https://${trimmed.substring(7)}"
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> trimmed
        }
    val queryIndex = httpsUrl.indexOf('?')
    val basePath = if (queryIndex >= 0) httpsUrl.substring(0, queryIndex) else httpsUrl
    val queryPart = if (queryIndex >= 0) httpsUrl.substring(queryIndex) else ""
    val isBiliCdn = basePath.contains("hdslb.com", ignoreCase = true)
    return if (isBiliCdn && !basePath.contains('@')) {
        "$basePath$BILI_HERO_COVER_SUFFIX$queryPart"
    } else {
        httpsUrl
    }
}

/**
 * 首页顶部 Hero Showcase 沉浸式大屏看板。
 *
 * 特性：
 * - **零焦点劫持**：自身不参与 D-Pad 焦点竞争，焦点直达下方泳道卡片并随卡片获焦联动刷新；
 * - **300ms 丝滑过渡**：在 [EffectiveHeroMode.FullDynamic] 下对右侧 16:9 海报与左侧文案执行 300ms 淡入淡出；
 * - **显存保护与生命周期释放**：进入播放器或退至后台（`ON_STOP`）时立即卸载大图组件释放 Bitmap 引用，
 *   并在低配设备/省流模式下自动切换为纯矢量品牌渐变静态底板。
 *
 * @param showcase 当前展示的精选视频数据，为 null 时不渲染。
 * @param effectiveMode 实际生效的看板模式。
 * @param modifier 修饰符。
 */
@Composable
fun HomeHeroBanner(
    showcase: HeroShowcaseData?,
    effectiveMode: EffectiveHeroMode,
    modifier: Modifier = Modifier,
) {
    if (effectiveMode == EffectiveHeroMode.Off || showcase == null) return

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val bannerHeight: Dp = (screenHeightDp * 0.37f).dp.coerceIn(168.dp, 208.dp)
    val isBackdropVisible = rememberLifecycleStartedState()
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(backgroundColor)
                .clipToBounds()
                .testTag(HOME_HERO_BANNER_TAG),
    ) {
        if (effectiveMode == EffectiveHeroMode.FullDynamic && isBackdropVisible) {
            HeroDynamicBackdrop(
                coverUrl = showcase.cover,
                backgroundColor = backgroundColor,
                isDark = isDark,
            )
        } else {
            HeroStaticBackdrop(
                backgroundColor = backgroundColor,
                isDark = isDark,
            )
        }

        HeroEditorialPane(
            showcase = showcase,
            effectiveMode = effectiveMode,
            isDark = isDark,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.64f)
                    .align(Alignment.CenterStart),
        )
    }
}

/**
 * 监听当前页面是否处于 `STARTED` 及以上生命周期状态。
 *
 * 当导航进入全屏播放器或应用退至后台触发 `ON_STOP` 时返回 false，
 * 使 Hero 看板立即卸载大图纹理，为 Media3 解码器腾出宝贵显存。
 */
@Composable
private fun rememberLifecycleStartedState(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isStarted by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME,
                    -> isStarted = true

                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY,
                    -> isStarted = false

                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return isStarted
}

/**
 * 动态高清横版海报背景层（含 300ms 交叉淡入淡出与双轴电影感渐隐遮罩）。
 */
@Composable
private fun HeroDynamicBackdrop(
    coverUrl: String,
    backgroundColor: Color,
    isDark: Boolean,
) {
    val context = LocalContext.current
    val optimizedUrl = remember(coverUrl) { optimizeBiliHeroCoverUrl(coverUrl) }
    val primaryGlow =
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.08f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 右侧 66% 画幅承载 16:9 横版海报，避免被全宽强制拉伸裁切主体
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.66f)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(backgroundColor, primaryGlow),
                        ),
                    ),
        ) {
            Crossfade(
                targetState = optimizedUrl,
                animationSpec = tween(durationMillis = HERO_CROSSFADE_MS),
                label = "hero-poster-crossfade",
            ) { targetUrl ->
                if (targetUrl.isNotBlank()) {
                    val imageRequest =
                        remember(context, targetUrl) {
                            ImageRequest
                                .Builder(context)
                                .data(targetUrl)
                                .size(HERO_POSTER_DECODE_WIDTH, HERO_POSTER_DECODE_HEIGHT)
                                .precision(Precision.INEXACT)
                                .allowRgb565(true)
                                .crossfade(false)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .testTag(HOME_HERO_POSTER_TAG),
                    )
                }
            }
        }

        // 横向电影感渐隐遮罩：保证左侧 60% 文字排版区具备极高对比度，右侧透出通透海报氛围
        val midAlpha = if (isDark) 0.72f else 0.80f
        val farAlpha = if (isDark) 0.22f else 0.30f
        val edgeAlpha = if (isDark) 0.08f else 0.14f
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.00f to backgroundColor,
                            0.35f to backgroundColor.copy(alpha = 0.97f),
                            0.56f to backgroundColor.copy(alpha = midAlpha),
                            0.82f to backgroundColor.copy(alpha = farAlpha),
                            1.00f to backgroundColor.copy(alpha = edgeAlpha),
                        ),
                    ),
        )

        // 纵向顶底融合遮罩：底部无缝融入下方视频泳道背景色，消除生硬分割线
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to backgroundColor.copy(alpha = if (isDark) 0.38f else 0.24f),
                            0.24f to Color.Transparent,
                            0.68f to Color.Transparent,
                            0.90f to backgroundColor.copy(alpha = 0.82f),
                            1.00f to backgroundColor,
                        ),
                    ),
        )
    }
}

/**
 * 低配/省流静态渐变背景层（零 Bitmap 分配，纯轻量着色渐变）。
 */
@Composable
private fun HeroStaticBackdrop(
    backgroundColor: Color,
    isDark: Boolean,
) {
    val primaryWash = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f)
    val secondaryWash = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.12f else 0.07f)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(HOME_HERO_STATIC_BACKDROP_TAG)
                .background(
                    Brush.horizontalGradient(
                        0.0f to backgroundColor,
                        0.45f to primaryWash,
                        1.0f to secondaryWash,
                    ),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1.0f to backgroundColor,
                        ),
                    ),
        )
    }
}

/**
 * 左侧沉浸式导览信息面板。
 *
 * 在动态模式下以 `avid` 作为 `contentKey` 执行 300ms 淡入淡出：
 * - 切换不同视频时执行平滑渐隐渐显；
 * - 同一视频异步补全分区/简介时原地无感刷新，避免标题二次闪烁。
 */
@Composable
private fun HeroEditorialPane(
    showcase: HeroShowcaseData,
    effectiveMode: EffectiveHeroMode,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(start = 24.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (effectiveMode == EffectiveHeroMode.FullDynamic) {
            AnimatedContent(
                targetState = showcase,
                contentKey = { it.avid },
                transitionSpec = {
                    fadeIn(animationSpec = tween(HERO_CROSSFADE_MS)) togetherWith
                        fadeOut(animationSpec = tween(HERO_CROSSFADE_MS))
                },
                label = "hero-editorial-crossfade",
            ) { item ->
                HeroEditorialBody(
                    item = item,
                    isDark = isDark,
                    showStaticBadge = false,
                )
            }
        } else {
            HeroEditorialBody(
                item = showcase,
                isDark = isDark,
                showStaticBadge = true,
            )
        }
    }
}

/**
 * 看板正文排版主体：标签胶囊行 + 超大标题 + UP 主与统计元数据 + 双行简介。
 */
@Composable
private fun HeroEditorialBody(
    item: HeroShowcaseData,
    isDark: Boolean,
    showStaticBadge: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HeroBadgeRow(
            sourceShelfTitle = item.sourceShelfTitle,
            tagName = item.tagName,
            rcmdReason = item.rcmdReason,
            isDark = isDark,
            showStaticBadge = showStaticBadge,
        )

        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 30.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        HeroStatsRow(item = item)

        Text(
            text = item.displaySummary,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 顶部标签胶囊行（来源泳道、视频分区、推荐理由、省流模式标识）。
 */
@Composable
private fun HeroBadgeRow(
    sourceShelfTitle: String,
    tagName: String?,
    rcmdReason: String?,
    isDark: Boolean,
    showStaticBadge: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sourceShelfTitle.isNotBlank()) {
            val container = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.28f else 0.14f)
            val content = if (isDark) BVColors.PrimaryLight else BVColors.PrimaryStrong
            HeroPillBadge(
                text = sourceShelfTitle,
                containerColor = container,
                contentColor = content,
            )
        }

        if (!tagName.isNullOrBlank()) {
            HeroPillBadge(
                text = tagName,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(HOME_HERO_CATEGORY_TAG),
            )
        }

        if (!rcmdReason.isNullOrBlank()) {
            val reasonContainer = BVColors.Yellow.copy(alpha = if (isDark) 0.22f else 0.16f)
            val reasonContent = if (isDark) BVColors.Yellow else MaterialTheme.colorScheme.onSurface
            HeroPillBadge(
                text = rcmdReason,
                containerColor = reasonContainer,
                contentColor = reasonContent,
            )
        }

        if (showStaticBadge) {
            HeroPillBadge(
                text = "省流静态",
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(HOME_HERO_STATIC_BADGE_TAG),
            )
        }
    }
}

/**
 * 单个圆角胶囊标签。
 */
@Composable
private fun HeroPillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(color = containerColor, shape = BVShape.pill)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * UP 主、播放量、弹幕数、时长与发布时间等宽统计行。
 */
@Composable
private fun HeroStatsRow(item: HeroShowcaseData) {
    val tabularLabel = BVTypography.tabular(MaterialTheme.typography.labelLarge)
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (item.upName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = item.upName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (item.playString.isNotBlank()) {
            HeroIconStat(
                iconRes = R.drawable.ic_play_count,
                text = item.playString,
                style = tabularLabel,
                color = metaColor,
            )
        }

        if (item.danmakuString.isNotBlank()) {
            HeroIconStat(
                iconRes = R.drawable.ic_danmaku_count,
                text = item.danmakuString,
                style = tabularLabel,
                color = metaColor,
            )
        }

        if (item.timeString.isNotBlank()) {
            Text(
                text = item.timeString,
                style = tabularLabel,
                color = metaColor,
                maxLines = 1,
            )
        }

        val pubTime = item.pubTime
        if (!pubTime.isNullOrBlank()) {
            Text(
                text = pubTime,
                style = MaterialTheme.typography.labelMedium,
                color = metaColor.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}

/**
 * 带小图标的统计项。
 */
@Composable
private fun HeroIconStat(
    iconRes: Int,
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}
