package dev.frost819.newbv.core.focus

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** 按键事件是否为 KeyDown 类型。 */
fun KeyEvent.isKeyDown(): Boolean = type == KeyEventType.KeyDown

/** 按键事件是否为 KeyUp 类型。 */
fun KeyEvent.isKeyUp(): Boolean = type == KeyEventType.KeyUp

/** 按键是否为 D-Pad 上方向键。 */
fun KeyEvent.isDpadUp(): Boolean = key == Key.DirectionUp

/** 按键是否为 D-Pad 下方向键。 */
fun KeyEvent.isDpadDown(): Boolean = key == Key.DirectionDown

/** 按键是否为 D-Pad 左方向键。 */
fun KeyEvent.isDpadLeft(): Boolean = key == Key.DirectionLeft

/** 按键是否为 D-Pad 右方向键。 */
fun KeyEvent.isDpadRight(): Boolean = key == Key.DirectionRight

/** 按键是否为确认键（D-Pad 中心 / Enter）。 */
fun KeyEvent.isConfirm(): Boolean = key == Key.DirectionCenter || key == Key.Enter

/** 按键是否为 D-Pad 导航与确认操作按键。 */
fun KeyEvent.isDpadNav(): Boolean = isDpadUp() || isDpadDown() || isDpadLeft() || isDpadRight() || isConfirm()

/** 按键是否为返回键。 */
fun KeyEvent.isBack(): Boolean = key == Key.Back

/**
 * 按键是否为遥控器菜单键。
 *
 * 除标准 [Key.Menu] 外，部分遥控器固件将菜单键上报为键码 763，
 * 需一并兼容，保证同一物理按键在全应用内行为一致。
 */
fun KeyEvent.isMenuKey(): Boolean = key == Key.Menu || key == Key(763)
