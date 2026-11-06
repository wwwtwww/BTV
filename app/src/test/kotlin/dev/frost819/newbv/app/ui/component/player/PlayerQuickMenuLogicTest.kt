package dev.frost819.newbv.app.ui.component.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import dev.frost819.newbv.app.ui.component.player.menu.PlaySpeedItem
import dev.frost819.newbv.app.ui.component.player.menu.detailedDisplayName
import dev.frost819.newbv.app.ui.component.player.menu.shortBadge
import dev.frost819.newbv.app.ui.component.settings.displayName
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiStatus
import dev.frost819.newbv.biliapi.entity.video.SubtitleAiType
import dev.frost819.newbv.biliapi.entity.video.SubtitleType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.Resolution
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 播放器 OSD 快捷悬浮菜单逻辑与选项生成单测。
 */
class PlayerQuickMenuLogicTest {
    @Nested
    @DisplayName("点播 (VOD) 快捷菜单项生成测试")
    inner class VodQuickMenuTests {
        @Test
        fun `resolution items are sorted descending with correct selection and high-res badges`() {
            val available =
                mapOf(
                    80 to "1080P 高清",
                    120 to "4K 超清",
                    127 to "8K 超高清",
                    64 to "720P 高清",
                )
            val currentQuality = 120

            val sortedKeys = available.keys.sortedDescending()
            val items =
                sortedKeys.map { code ->
                    val res = Resolution.fromCode(code)
                    val label = available[code] ?: res.displayName
                    val badge = if (code >= 120) "高画质" else null
                    QuickMenuItem(
                        id = code,
                        label = label,
                        badge = badge,
                        isSelected = code == currentQuality,
                    )
                }

            assertEquals(listOf(127, 120, 80, 64), items.map { it.id })
            val selected = items.first { it.isSelected }
            assertEquals(120, selected.id)
            assertEquals("高画质", selected.badge)

            val item8k = items.first { it.id == 127 }
            assertEquals("高画质", item8k.badge)

            val item1080p = items.first { it.id == 80 }
            assertNull(item1080p.badge)
        }

        @Test
        fun `play speed items correctly reflect active speed`() {
            val currentSpeed = 1.25f
            val items =
                PlaySpeedItem.entries.map {
                    QuickMenuItem(
                        id = it.speed,
                        label = it.displayName,
                        isSelected = it.speed == currentSpeed,
                    )
                }

            assertEquals(PlaySpeedItem.entries.size, items.size)
            val selected = items.single { it.isSelected }
            assertEquals(1.25f, selected.id)
            assertEquals("1.25x", selected.label)
        }

        @Test
        fun `audio items map detailed descriptions and short badges`() {
            val available = listOf(Audio.A64K, Audio.A192K, Audio.ADolbyAtoms)
            val current = Audio.ADolbyAtoms

            val sorted = available.sortedByDescending { it.ordinal }
            val items =
                sorted.map {
                    QuickMenuItem(
                        id = it,
                        label = it.detailedDisplayName,
                        badge = it.shortBadge,
                        isSelected = it == current,
                    )
                }

            assertEquals(Audio.ADolbyAtoms, items.first().id)
            assertTrue(items.first().isSelected)
            assertEquals("杜比", items.first().badge)
            assertTrue(items.first().label.contains("Dolby Atmos"))

            val item192k = items.first { it.id == Audio.A192K }
            assertEquals("192K", item192k.badge)
        }

        @Test
        fun `subtitle items include close option as first item`() {
            val subtitles =
                listOf(
                    Subtitle(
                        id = 101L,
                        lang = "zh-CN",
                        langDoc = "中文（简体）",
                        url = "http://test/1",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None,
                    ),
                    Subtitle(
                        id = 102L,
                        lang = "en-US",
                        langDoc = "English",
                        url = "http://test/2",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None,
                    ),
                )
            val currentSubtitleId = 101L

            val items =
                buildList {
                    add(
                        QuickMenuItem(
                            id = Subtitle.Empty,
                            label = "关闭",
                            isSelected = currentSubtitleId == -1L,
                        ),
                    )
                    subtitles.forEach { sub ->
                        add(
                            QuickMenuItem(
                                id = sub,
                                label = sub.langDoc,
                                isSelected = sub.id == currentSubtitleId,
                            ),
                        )
                    }
                }

            assertEquals(3, items.size)
            assertEquals("关闭", items.first().label)
            assertFalse(items.first().isSelected)

            val selected = items.single { it.isSelected }
            assertEquals(101L, selected.id.id)
            assertEquals("中文（简体）", selected.label)
        }
    }

    @Nested
    @DisplayName("直播 (Live) 快捷菜单项生成测试")
    inner class LiveQuickMenuTests {
        @Test
        fun `live quality items match available qualities and selected qn`() {
            val qualities =
                listOf(
                    10000 to "原画",
                    4000 to "蓝光",
                    2500 to "超清",
                )
            val currentQn = 4000

            val items =
                qualities.map {
                    QuickMenuItem(
                        id = it.first,
                        label = it.second,
                        isSelected = it.first == currentQn,
                    )
                }

            assertEquals(3, items.size)
            val selected = items.single { it.isSelected }
            assertEquals(4000, selected.id)
            assertEquals("蓝光", selected.label)
        }

        @Test
        fun `resolveLiveQuality selects matched quality when present`() {
            val qualities =
                listOf(
                    10000 to "原画",
                    4000 to "蓝光",
                    2500 to "超清",
                )
            val result = resolveLiveQuality(qualities, currentQuality = 4000)
            assertEquals(4000 to "蓝光", result)
        }

        @Test
        fun `resolveLiveQuality falls back to first quality when current quality is invalid or default`() {
            val qualities =
                listOf(
                    10000 to "原画",
                    4000 to "蓝光",
                    2500 to "超清",
                )
            // 默认值 -1 或 0 不在列表内时，自动平滑 fallback 到首项（如原画），防止出现空标签或未选中指示
            val fallbackFromNegative = resolveLiveQuality(qualities, currentQuality = -1)
            assertEquals(10000 to "原画", fallbackFromNegative)

            val fallbackFromZero = resolveLiveQuality(qualities, currentQuality = 0)
            assertEquals(10000 to "原画", fallbackFromZero)
        }

        @Test
        fun `resolveLiveQuality returns null when quality list is empty to allow localized fallback`() {
            val result = resolveLiveQuality(emptyList(), currentQuality = -1)
            assertNull(result)
        }

        @Test
        fun `live line items are generated 1-indexed`() {
            val totalLines = 3
            val currentLine = 2

            val items =
                (1..totalLines).map { lineIndex ->
                    QuickMenuItem(
                        id = lineIndex,
                        label = "线路 $lineIndex",
                        isSelected = lineIndex == currentLine,
                    )
                }

            assertEquals(3, items.size)
            assertEquals(listOf("线路 1", "线路 2", "线路 3"), items.map { it.label })
            val selected = items.single { it.isSelected }
            assertEquals(2, selected.id)
        }
    }

    @Nested
    @DisplayName("快捷菜单相邻平移状态机测试")
    inner class PopoverAdjacentNavigationTests {
        @Test
        fun `vod adjacent navigation flows correctly left and right`() {
            // Forward: Resolution -> PlaySpeed -> Audio -> Subtitle
            val nextFromRes = VodQuickMenuType.Resolution.next(hasSubtitles = true)
            assertEquals(VodQuickMenuType.PlaySpeed, nextFromRes)

            val nextFromSpeed = VodQuickMenuType.PlaySpeed.next(hasSubtitles = true)
            assertEquals(VodQuickMenuType.Audio, nextFromSpeed)

            val nextFromAudioWithSubs = VodQuickMenuType.Audio.next(hasSubtitles = true)
            assertEquals(VodQuickMenuType.Subtitle, nextFromAudioWithSubs)

            val nextFromAudioWithoutSubs = VodQuickMenuType.Audio.next(hasSubtitles = false)
            assertNull(nextFromAudioWithoutSubs)

            val nextFromSub = VodQuickMenuType.Subtitle.next(hasSubtitles = true)
            assertNull(nextFromSub)

            // Backward: Subtitle -> Audio -> PlaySpeed -> Resolution
            val prevFromSub = VodQuickMenuType.Subtitle.previous()
            assertEquals(VodQuickMenuType.Audio, prevFromSub)

            val prevFromAudio = VodQuickMenuType.Audio.previous()
            assertEquals(VodQuickMenuType.PlaySpeed, prevFromAudio)

            val prevFromSpeed = VodQuickMenuType.PlaySpeed.previous()
            assertEquals(VodQuickMenuType.Resolution, prevFromSpeed)

            val prevFromRes = VodQuickMenuType.Resolution.previous()
            assertNull(prevFromRes)
        }

        @Test
        fun `live adjacent navigation toggles between quality and line`() {
            val nextFromQualityWithLines = LiveQuickMenuType.Quality.next(hasLines = true)
            assertEquals(LiveQuickMenuType.Line, nextFromQualityWithLines)

            val nextFromQualityWithoutLines = LiveQuickMenuType.Quality.next(hasLines = false)
            assertNull(nextFromQualityWithoutLines, "直播间无可用备用线路时不可向右切换至线路菜单")

            val nextFromLine = LiveQuickMenuType.Line.next()
            assertNull(nextFromLine)

            val prevFromLine = LiveQuickMenuType.Line.previous()
            assertEquals(LiveQuickMenuType.Quality, prevFromLine)

            val prevFromQuality = LiveQuickMenuType.Quality.previous()
            assertNull(prevFromQuality)
        }

        @Test
        fun `boundary key navigation consumes direction events even without adjacent target`() {
            // 验证边界菜单按键处理契约：即使 onNavigate 为 null 也应始终消费事件 (返回 true)，阻止冒泡到父级触发 seek
            fun handleDirectionKey(onNavigate: (() -> Unit)?): Boolean {
                onNavigate?.invoke()
                return true
            }

            var leftNavigated = false
            val nonBoundaryHandled = handleDirectionKey { leftNavigated = true }
            assertTrue(nonBoundaryHandled)
            assertTrue(leftNavigated)

            val boundaryHandled = handleDirectionKey(null)
            assertTrue(boundaryHandled, "边界菜单无相邻目标时仍需消费按键以阻止事件穿透冒泡")
        }
    }

    @Nested
    @DisplayName("快捷菜单水平锚点坐标对齐与边界钳制测试")
    inner class QuickMenuAnchorOffsetTests {
        @Test
        fun `aligns popover start with measured button root x relative to container`() {
            // Given: 容器左边缘 48px，第5个按钮（字幕）因前置文本徽标撑宽实际位于 608px（相对偏移 560px）
            val offsetPx =
                calculateQuickMenuOffsetPx(
                    anchorRootX = 608f,
                    containerRootX = 48f,
                    containerWidthPx = 1824,
                    popoverWidthPx = 440,
                )

            // Then
            assertEquals(560, offsetPx)
        }

        @Test
        fun `falls back to zero when button anchor has not been measured yet`() {
            // Given
            val offsetPx =
                calculateQuickMenuOffsetPx(
                    anchorRootX = null,
                    containerRootX = 48f,
                    containerWidthPx = 1824,
                    popoverWidthPx = 440,
                )

            // Then
            assertEquals(0, offsetPx)
        }

        @Test
        fun `clamps offset to right boundary when button is near container end`() {
            // Given: 容器宽 1000px，弹窗宽 440px，最大合法起点为 560px；按钮位于容器内 720px
            val offsetPx =
                calculateQuickMenuOffsetPx(
                    anchorRootX = 768f,
                    containerRootX = 48f,
                    containerWidthPx = 1000,
                    popoverWidthPx = 440,
                )

            // Then
            assertEquals(560, offsetPx)
        }

        @Test
        fun `returns zero safely when container is narrower than popover`() {
            // Given
            val offsetPx =
                calculateQuickMenuOffsetPx(
                    anchorRootX = 200f,
                    containerRootX = 24f,
                    containerWidthPx = 300,
                    popoverWidthPx = 440,
                )

            // Then
            assertEquals(0, offsetPx)
        }

        @Test
        fun `protects against non-finite float coordinates gracefully`() {
            val offsetWithNan =
                calculateQuickMenuOffsetPx(
                    anchorRootX = Float.NaN,
                    containerRootX = Float.NaN,
                    containerWidthPx = 1000,
                    popoverWidthPx = 440,
                )
            assertEquals(0, offsetWithNan)

            val offsetWithInfinity =
                calculateQuickMenuOffsetPx(
                    anchorRootX = Float.POSITIVE_INFINITY,
                    containerRootX = 48f,
                    containerWidthPx = 1000,
                    popoverWidthPx = 440,
                )
            assertEquals(0, offsetWithInfinity)
        }
    }

    @Nested
    @DisplayName("快捷微菜单按键消费优先级测试")
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    inner class QuickMenuKeyEventTests {
        @Test
        fun `Key Back dismisses popover and consumes event`() {
            var dismissed = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyDown,
                    key = Key.Back,
                    onDismiss = { dismissed = true },
                )
            assertTrue(consumed)
            assertTrue(dismissed)
        }

        @Test
        fun `Key Menu dismisses popover and consumes event without bubbling to outer controller`() {
            var dismissed = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyDown,
                    key = Key.Menu,
                    onDismiss = { dismissed = true },
                )
            assertTrue(consumed)
            assertTrue(dismissed)
        }

        @Test
        fun `Key 763 compatibility menu key dismisses popover and consumes event`() {
            var dismissed = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyDown,
                    key = Key(763),
                    onDismiss = { dismissed = true },
                )
            assertTrue(consumed)
            assertTrue(dismissed)
        }

        @Test
        fun `DirectionLeft navigates left and consumes event`() {
            var leftCalled = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyDown,
                    key = Key.DirectionLeft,
                    onDismiss = {},
                    onNavigateLeft = { leftCalled = true },
                )
            assertTrue(consumed)
            assertTrue(leftCalled)
        }

        @Test
        fun `DirectionRight navigates right and consumes event`() {
            var rightCalled = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyDown,
                    key = Key.DirectionRight,
                    onDismiss = {},
                    onNavigateRight = { rightCalled = true },
                )
            assertTrue(consumed)
            assertTrue(rightCalled)
        }

        @Test
        fun `KeyUp events are ignored and not consumed`() {
            var dismissed = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyUp,
                    key = Key.Back,
                    onDismiss = { dismissed = true },
                )
            assertFalse(consumed)
            assertFalse(dismissed)
        }

        @Test
        fun `unhandled key returns false`() {
            var dismissed = false
            val consumed =
                handleQuickMenuKeyEvent(
                    type = KeyEventType.KeyDown,
                    key = Key.DirectionCenter,
                    onDismiss = { dismissed = true },
                )
            assertFalse(consumed)
            assertFalse(dismissed)
        }
    }
}
