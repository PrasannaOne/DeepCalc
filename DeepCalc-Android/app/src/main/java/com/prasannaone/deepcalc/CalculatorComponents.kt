package com.prasannaone.deepcalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

@Composable
internal fun MenuHeader(label: String, colors: Palette) {
    Text(
        label,
        Modifier.fillMaxWidth().padding(start = 15.dp, top = 5.dp, bottom = 2.dp),
        color = colors.muted.copy(alpha = .72f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}

@Composable
internal fun CompactMenuItem(label: String, colors: Palette, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.text, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
internal fun HistoryAction(label: String, primary: Boolean, colors: Palette, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (primary) colors.primaryContainer else colors.soft) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (primary) colors.primary else colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun GuideSection(title: String, body: String, colors: Palette) {
    Text(title, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 13.dp, bottom = 4.dp))
    Text(body, color = colors.text, fontSize = 13.sp, lineHeight = 20.sp)
}

internal fun Modifier.borderStart(color: Color): Modifier = this.then(Modifier.drawBehind {
    drawLine(color.copy(alpha = .60f), androidx.compose.ui.geometry.Offset(0f, 0f),
        androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 2.dp.toPx())
})

@Composable
internal fun CalculatorKeyboard(colors: Palette, onTap: (String) -> Unit, onLong: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val gap = when {
        configuration.screenWidthDp >= 700 -> 7.dp
        configuration.screenHeightDp <= 700 -> 4.dp
        else -> 6.dp
    }
    val cellHeight = when {
        configuration.screenHeightDp <= 560 -> (configuration.screenHeightDp * 0.067f).dp.coerceIn(34.dp, 49.dp)
        configuration.screenHeightDp <= 700 -> (configuration.screenHeightDp * 0.067f).dp.coerceIn(37.dp, 49.dp)
        configuration.screenWidthDp <= 420 -> (configuration.screenHeightDp * 0.074f).dp.coerceIn(42.dp, 54.dp)
        configuration.screenWidthDp >= 700 -> 58.dp
        else -> (configuration.screenHeightDp * 0.071f).dp.coerceIn(43.dp, 58.dp)
    }
    BoxWithConstraints(Modifier.widthIn(max = 960.dp).fillMaxWidth().background(colors.variant)
        .padding(horizontal = if (configuration.screenWidthDp >= 700) 14.dp else 6.dp,
            vertical = if (configuration.screenHeightDp <= 700) 5.dp else 9.dp)) {
        val keys = listOf(
            listOf("^" to "power", "#" to "hash", "$" to "dollar", "=" to "=", "▣" to "paste"),
            listOf("7" to "7", "8" to "8", "9" to "9", "÷" to "÷", "%" to "%"),
            listOf("4" to "4", "5" to "5", "6" to "6", "×" to "×", "()" to "paren")
        )
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth().height(cellHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { (label, key) -> KeyboardKey(label, key, Modifier.weight(1f).fillMaxHeight(), colors, onTap, onLong) }
                }
            }
            Box(Modifier.fillMaxWidth().height(cellHeight * 2 + gap)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.fillMaxWidth().height(cellHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        listOf("1" to "1", "2" to "2", "3" to "3", "−" to "−").forEach { (label, key) -> KeyboardKey(label, key, Modifier.weight(1f).fillMaxHeight(), colors, onTap, onLong) }
                        Spacer(Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().height(cellHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        listOf("0" to "0", "." to ".", "⌫" to "backspace", "+" to "+").forEach { (label, key) -> KeyboardKey(label, key, Modifier.weight(1f).fillMaxHeight(), colors, onTap, onLong) }
                        Spacer(Modifier.weight(1f))
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).width((this@BoxWithConstraints.maxWidth - gap * 4) / 5).height(cellHeight * 2 + gap)) {
                    KeyboardKey("↵", "enter", Modifier.fillMaxSize(), colors, onTap, onLong, enter = true)
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(label: String, key: String, modifier: Modifier, colors: Palette,
                        onTap: (String) -> Unit, onLong: (String) -> Unit, enter: Boolean = false) {
    val special = key in setOf("power", "hash", "dollar", "=", "paste", "÷", "%", "×", "paren", "−", "+")
    val danger = key == "backspace" || enter
    val bg = when { danger -> colors.keyAccent; special -> colors.secondaryContainer; else -> colors.raised }
    val fg = when { danger -> colors.keyAccentText; special -> colors.secondaryText; else -> colors.text }
    var longPressVisual by remember(key) { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(if (enter) 13.dp else 11.dp)).background(bg)
        .semantics { role = Role.Button; onClick { onTap(key); true } }
        .pointerInput(key) {
            var longTriggered = false
            detectTapGestures(
                onTap = { onTap(key) },
                onLongPress = { longTriggered = true; longPressVisual = true },
                onPress = {
                    longTriggered = false
                    val released = tryAwaitRelease()
                    if (released && longTriggered) {
                        if (key in setOf("power", "backspace", "dollar")) onLong(key) else onTap(key)
                    }
                    longPressVisual = false
                }
            )
        }, contentAlignment = Alignment.Center) {
        Text(if (key == "power" && longPressVisual) "√" else label, color = fg,
            fontSize = if (enter) 25.sp else 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    }
}
