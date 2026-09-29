package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.Lucide

enum class DurationUnit(val seconds: Int) {
    SECONDS(1),
    MINUTES(60),
    HOURS(3_600),
    DAYS(86_400),
    WEEKS(604_800);

    companion object {
        fun bestFor(totalSeconds: Int, allowed: List<DurationUnit> = entries): DurationUnit {
            val largestExact = allowed
                .sortedByDescending { it.seconds }
                .firstOrNull { totalSeconds >= it.seconds && totalSeconds % it.seconds == 0 }
            return largestExact ?: allowed.minBy { it.seconds }
        }
    }
}

@Composable
fun DurationUnit.shortLabel(): String {
    val s = LocalStrings.current
    return when (this) {
        DurationUnit.SECONDS -> s.unitSecondsLabel
        DurationUnit.MINUTES -> s.unitMinutesLabel
        DurationUnit.HOURS -> s.unitHoursLabel
        DurationUnit.DAYS -> s.unitDaysLabel
        DurationUnit.WEEKS -> s.unitWeeksLabel
    }
}

private val SelectorShape = RoundedCornerShape(6.dp)
private const val MAX_DIGITS = 9

@Composable
fun <T> ChatoneInlineSelector(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val accent = MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        accent.copy(alpha = if (hovered || expanded) 0.24f else 0.14f),
        tween(140),
        label = "selectorContainer"
    )
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(SelectorShape)
                .background(container)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.DropdownList,
                    onClick = { expanded = true }
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(start = 7.dp, end = 2.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label(selected),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Icon(Lucide.ChevronDown, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        ChatoneDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option), style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = if (option == selected) {
                        { Icon(Lucide.Check, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ChatoneNumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    range: IntRange = 0..Int.MAX_VALUE,
    suffix: String? = null,
    enabled: Boolean = true
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value.toString()
    }
    ChatoneTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(MAX_DIGITS)
            text = digits
            val parsed = digits.toIntOrNull() ?: return@ChatoneTextField
            when {
                parsed > range.last -> {
                    text = range.last.toString()
                    if (range.last != value) onValueChange(range.last)
                }
                parsed >= range.first && parsed != value -> onValueChange(parsed)
            }
        },
        modifier = modifier.onFocusChanged { state ->
            if (!state.hasFocus && text.toIntOrNull() != value) text = value.toString()
        },
        label = label,
        hint = hint,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailing = suffix?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun ChatoneDurationField(
    totalSeconds: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    units: List<DurationUnit> = DurationUnit.entries,
    minSeconds: Int = 1,
    maxSeconds: Int = Int.MAX_VALUE
) {
    var unit by remember { mutableStateOf(DurationUnit.bestFor(totalSeconds, units)) }
    var text by remember { mutableStateOf((totalSeconds / unit.seconds).toString()) }
    var lastEmitted by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(totalSeconds) {
        if (totalSeconds != lastEmitted) {
            unit = DurationUnit.bestFor(totalSeconds, units)
            text = (totalSeconds / unit.seconds).toString()
            lastEmitted = totalSeconds
        }
    }

    fun commit(amount: Int, newUnit: DurationUnit) {
        val requested = amount.toLong() * newUnit.seconds
        val seconds = requested.coerceIn(minSeconds.toLong(), maxSeconds.toLong()).toInt()
        if (seconds.toLong() != requested) {
            unit = DurationUnit.bestFor(seconds, units)
            text = (seconds / unit.seconds).toString()
        }
        lastEmitted = seconds
        if (seconds != totalSeconds) onValueChange(seconds)
    }

    ChatoneTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(MAX_DIGITS)
            text = digits
            digits.toIntOrNull()?.takeIf { it > 0 }?.let { commit(it, unit) }
        },
        modifier = modifier.onFocusChanged { state ->
            if (!state.hasFocus && (text.toIntOrNull() ?: 0) <= 0) {
                text = (totalSeconds / unit.seconds).coerceAtLeast(1).toString()
            }
        },
        label = label,
        hint = hint,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        contentPaddingVertical = 5.dp,
        trailing = {
            ChatoneInlineSelector(
                options = units,
                selected = unit,
                label = { it.shortLabel() },
                onSelect = { newUnit ->
                    unit = newUnit
                    commit(text.toIntOrNull()?.takeIf { it > 0 } ?: 1, newUnit)
                }
            )
        }
    )
}
