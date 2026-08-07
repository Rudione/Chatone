package io.rudione.chatone.presentation.components.rows

import io.rudione.chatone.presentation.components.ChatoneSlider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.unfold_more
import io.rudione.chatone.presentation.settings.theme_settings.ThinSlider
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.jetbrains.compose.resources.painterResource
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

internal val LocalSettingsSearch = compositionLocalOf { "" }

@Composable
fun HighlightedSettingsText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val q = LocalSettingsSearch.current.trim()
    val annotated = remember(text, q) {
        if (q.isEmpty() || q.length < 2) {
            buildAnnotatedString { append(text) }
        } else {
            val lowerText = text.lowercase()
            val lowerQ = q.lowercase()
            buildAnnotatedString {
                var i = 0
                while (i < text.length) {
                    val idx = lowerText.indexOf(lowerQ, i)
                    if (idx < 0) {
                        append(text.substring(i))
                        break
                    }
                    if (idx > i) append(text.substring(i, idx))
                    withStyle(
                        SpanStyle(
                            background = Color(0xFFFFEE58).copy(alpha = 0.25f),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append(text.substring(idx, idx + q.length)) }
                    i = idx + q.length
                }
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HighlightedSettingsText(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) HighlightedSettingsText(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        io.rudione.chatone.presentation.components.ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ListRow(
    title: String,
    value: String,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                HighlightedSettingsText(title, style = MaterialTheme.typography.bodyMedium)
                HighlightedSettingsText(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painterResource(Res.drawable.unfold_more),
                null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ChatoneDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(i); expanded = false })
            }
        }
    }
}

@Composable
fun DropdownRow(
    label: String,
    description: String,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                HighlightedSettingsText(label, style = MaterialTheme.typography.bodyLarge)
                HighlightedSettingsText(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                options.getOrElse(selected) { "" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ChatoneDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(i); expanded = false })
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HighlightedSettingsText(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ThinSlider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    isFloat: Boolean = false,
    onFloatChange: ((Float) -> Unit)? = null,
    onValueChange: ((Float) -> Unit)? = null
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HighlightedSettingsText(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (isFloat && onFloatChange != null) {
            ChatoneSlider(
                value = value / 100f,
                onValueChange = onFloatChange,
                valueRange = valueRange,
                steps = steps
            )
        } else if (onValueChange != null) {
            ChatoneSlider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
fun HotkeyRow(
    title: String,
    subtitle: String,
    currentHotkey: String,
    allowMouseButtons: Boolean = false,
    onHotkeyChanged: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HighlightedSettingsText(title, style = MaterialTheme.typography.bodyLarge)
            HighlightedSettingsText(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.widthIn(min = 110.dp)
                    .then(
                        if (allowMouseButtons) Modifier.pointerInput(isRecording) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (!isRecording) continue
                                    if (event.type != PointerEventType.Press) continue
                                    val name = when {
                                        event.buttons.isSecondaryPressed -> "mouse2"
                                        event.buttons.isTertiaryPressed -> "mouse3"
                                        event.buttons.isBackPressed -> "mouse4"
                                        event.buttons.isForwardPressed -> "mouse5"
                                        else -> null
                                    } ?: continue
                                    event.changes.forEach { it.consume() }
                                    onHotkeyChanged(name)
                                    isRecording = false
                                }
                            }
                        } else Modifier
                    )
                    .onKeyEvent { event ->
                        if (!isRecording) return@onKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent true

                        if (event.key in listOf(
                                Key.CtrlLeft,
                                Key.CtrlRight,
                                Key.MetaLeft,
                                Key.MetaRight
                            )
                        ) {
                            if (!event.isAltPressed && !event.isShiftPressed) {
                                onHotkeyChanged("ctrl"); isRecording = false; return@onKeyEvent true
                            }
                        }
                        if (event.key in listOf(Key.AltLeft, Key.AltRight)) {
                            val parts2 = mutableListOf<String>()
                            if (event.isCtrlPressed || event.isMetaPressed) parts2.add("ctrl")
                            parts2.add("alt")
                            if (event.isShiftPressed) parts2.add("shift")
                            onHotkeyChanged(parts2.joinToString("+")); isRecording =
                                false; return@onKeyEvent true
                        }
                        if (event.key in listOf(Key.ShiftLeft, Key.ShiftRight)) {
                            if (!event.isAltPressed && !(event.isCtrlPressed || event.isMetaPressed)) {
                                onHotkeyChanged("shift"); isRecording =
                                    false; return@onKeyEvent true
                            }
                        }
                        if (event.key == Key.Escape) {
                            onHotkeyChanged(""); isRecording = false; return@onKeyEvent true
                        }
                        val parts = mutableListOf<String>()
                        if (event.isCtrlPressed || event.isMetaPressed) parts.add("ctrl")
                        if (event.isAltPressed) parts.add("alt")
                        if (event.isShiftPressed) parts.add("shift")
                        val keyName = hotkeyKeyToName(event.key)
                        if (keyName.isNotEmpty()) parts.add(keyName)
                        onHotkeyChanged(parts.joinToString("+"))
                        isRecording = false; true
                    }
                    .clickable { isRecording = true }
            ) {
                val sh = LocalStrings.current
                Text(
                    when {
                        isRecording -> sh.settingsRecording; currentHotkey.isBlank() -> sh.settingsHotkeyNotSet; else -> currentHotkey.uppercase()
                        .replace("+", " + ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        isRecording -> MaterialTheme.colorScheme.onPrimaryContainer; currentHotkey.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.45f
                        ); else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            if (currentHotkey.isNotBlank() || isRecording) {
                ChatoneIconButton(
                    onClick = { onHotkeyChanged(""); isRecording = false },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun hotkeyKeyToName(key: Key): String = when (key) {
    Key.Spacebar -> "space"; Key.Enter -> "enter"; Key.Tab -> "tab"
    Key.Backspace -> "backspace"; Key.Delete -> "delete"
    Key.MoveHome -> "home"; Key.MoveEnd -> "end"
    Key.PageUp -> "pageup"; Key.PageDown -> "pagedown"
    Key.DirectionUp -> "up"; Key.DirectionDown -> "down"
    Key.DirectionLeft -> "left"; Key.DirectionRight -> "right"
    Key.F1 -> "f1"; Key.F2 -> "f2"; Key.F3 -> "f3"; Key.F4 -> "f4"
    Key.F5 -> "f5"; Key.F6 -> "f6"; Key.F7 -> "f7"; Key.F8 -> "f8"
    Key.F9 -> "f9"; Key.F10 -> "f10"; Key.F11 -> "f11"; Key.F12 -> "f12"
    else -> {
        val code = key.keyCode
        when {
            code in 65L..90L -> ('A' + (code - 65).toInt()).lowercaseChar().toString()
            code in 48L..57L -> (code - 48).toString()
            else -> ""
        }
    }
}
