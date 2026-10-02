package io.rudione.chatone.presentation.components.rows

import io.rudione.chatone.presentation.components.ChatoneSlider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import io.rudione.chatone.presentation.components.expressive.HelpHint
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.presentation.settings.theme_settings.ThinSlider
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.icons.lucide.ChevronsUpDown
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X

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
        modifier = Modifier.padding(horizontal = SettingsRowMetrics.horizontal),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}

internal object SettingsRowMetrics {
    val horizontal: Dp = if (isDesktopPlatform) 14.dp else 18.dp
    val vertical: Dp = if (isDesktopPlatform) 6.dp else 10.dp
    val minHeight: Dp = if (isDesktopPlatform) 44.dp else 56.dp
}

@Composable
private fun settingsTitleStyle(): TextStyle =
    if (isDesktopPlatform) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge

@Composable
private fun RowScope.RowTitle(title: String, help: String?) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HighlightedSettingsText(
            title,
            style = settingsTitleStyle(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!help.isNullOrBlank()) HelpHint(title = title, text = help)
    }
}

@Composable
private fun ValuePill(value: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(start = 12.dp, end = if (trailing != null) 8.dp else 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HighlightedSettingsText(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp)
        )
        trailing?.invoke()
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsRowMetrics.minHeight)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowTitle(title, subtitle)
        Spacer(Modifier.width(12.dp))
        ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ListRow(
    title: String,
    value: String,
    options: List<String>,
    help: String? = null,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsRowMetrics.minHeight)
                .clickable { expanded = true }
                .padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowTitle(title, help)
            Spacer(Modifier.width(12.dp))
            ValuePill(value) {
                Icon(
                    Lucide.ChevronsUpDown,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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
    ListRow(
        title = label,
        value = options.getOrElse(selected) { "" },
        options = options,
        help = description.takeIf { it.isNotBlank() },
        onSelected = onSelected
    )
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
    Column(
        modifier = Modifier.padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RowTitle(label, null)
            ValuePill(valueLabel)
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
    Column(
        modifier = Modifier.padding(horizontal = SettingsRowMetrics.horizontal, vertical = SettingsRowMetrics.vertical)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RowTitle(title, null)
            ValuePill(valueLabel)
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
                        Lucide.X,
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
