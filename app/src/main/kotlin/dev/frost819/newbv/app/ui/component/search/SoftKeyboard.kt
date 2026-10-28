package dev.frost819.newbv.app.ui.component.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.core.focus.focusInvertedColors
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.theme.BVShape
import dev.frost819.newbv.core.theme.BVTheme
import dev.frost819.newbv.core.theme.LocalIsDarkTheme

private val keyboardKeys =
    listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "O", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X"),
        listOf("Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", "0"),
    )

/**
 * 紧凑型 TV 软键盘（适配 10-foot 双栏搜索窄左栏）。
 *
 * 采用扁平化 6×6 字母数字矩阵 + 底部「清除 / 删除 / 搜索」三联操作栏，
 * 释放左栏下半屏高度给历史流式标签与关联搜索建议接力列表。
 * 支持深浅双主题高对比聚焦反色，并支持记录最后聚焦按键以便从右栏无缝返回。
 *
 * @param modifier 修饰符。
 * @param firstButtonFocusRequester 首个按键（A）的焦点请求器。
 * @param searchButtonModifier 搜索按钮的额外修饰符（用于跨页焦点记忆）。
 * @param onFocusedRequesterChanged 当任意按键获焦时回调其 [FocusRequester]，用于左右分栏焦点穿梭记忆。
 * @param onClick 字符键点击回调。
 * @param onClear 清除全部输入回调。
 * @param onDelete 删除末尾字符回调。
 * @param onSearch 提交搜索回调。
 */
@Composable
fun SoftKeyboard(
    modifier: Modifier = Modifier,
    firstButtonFocusRequester: FocusRequester,
    searchButtonModifier: Modifier = Modifier,
    onFocusedRequesterChanged: ((FocusRequester) -> Unit)? = null,
    onClick: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onSearch: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keyboardKeys.forEachIndexed { rowIndex, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowKeys.forEachIndexed { colIndex, key ->
                    val requester =
                        if (rowIndex == 0 && colIndex == 0) {
                            firstButtonFocusRequester
                        } else {
                            remember(rowIndex, colIndex) { FocusRequester() }
                        }
                    SoftKeyboardKey(
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(requester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        onFocusedRequesterChanged?.invoke(requester)
                                    }
                                },
                        key = key,
                        onClick = { onClick(key) },
                    )
                }
            }
        }

        val clearRequester = remember { FocusRequester() }
        val deleteRequester = remember { FocusRequester() }
        val searchRequester = remember { FocusRequester() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SoftKeyboardActionButton(
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(clearRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                onFocusedRequesterChanged?.invoke(clearRequester)
                            }
                        },
                label = "清除",
                isPrimary = false,
                onClick = onClear,
            )
            SoftKeyboardActionButton(
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(deleteRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                onFocusedRequesterChanged?.invoke(deleteRequester)
                            }
                        },
                label = "删除",
                isPrimary = false,
                onClick = onDelete,
            )
            SoftKeyboardActionButton(
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(searchRequester)
                        .then(searchButtonModifier)
                        .onFocusChanged {
                            if (it.isFocused) {
                                onFocusedRequesterChanged?.invoke(searchRequester)
                            }
                        },
                label = "搜索",
                isPrimary = true,
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun SoftKeyboardKey(
    modifier: Modifier = Modifier,
    key: String,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.78f)
    val borderColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.1f else 0.08f)

    Surface(
        modifier =
            modifier
                .height(29.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    Border(
                        border = BorderStroke(1.dp, borderColor),
                        shape = BVShape.small,
                    ),
                focusedBorder = Border.None,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SoftKeyboardActionButton(
    modifier: Modifier = Modifier,
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor =
        if (isPrimary) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.32f else 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.78f)
        }
    val contentColor =
        if (isPrimary && !isDark) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val borderColor =
        if (isPrimary) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.1f else 0.08f)
        }

    Surface(
        modifier =
            modifier
                .height(30.dp)
                .touchClickable(onClick = onClick),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(BVShape.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        colors =
            focusInvertedColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    Border(
                        border = BorderStroke(1.dp, borderColor),
                        shape = BVShape.small,
                    ),
                focusedBorder = Border.None,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C101A)
@Composable
private fun SoftKeyboardPreview() {
    BVTheme {
        Box(modifier = Modifier.width(228.dp)) {
            SoftKeyboard(
                firstButtonFocusRequester = remember { FocusRequester() },
                onClick = {},
                onClear = {},
                onDelete = {},
                onSearch = {},
            )
        }
    }
}
