package io.rudione.chatone.presentation.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.OnlinePrediction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private val DURATION_PRESETS_SECONDS = listOf(30, 60, 120, 300, 600)

@Composable
private fun DurationChipsRow(
    selectedSeconds: Int,
    accent: Color,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        DURATION_PRESETS_SECONDS.forEach { secs ->
            val selected = secs == selectedSeconds
            val label = if (secs >= 60) "${secs / 60}m" else "${secs}s"
            val bg by animateColorAsState(
                if (selected) accent
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                animationSpec = tween(160),
                label = "durationChip"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(bg)
                    .clickable { onSelect(secs) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChoiceInputRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChatoneTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f)
        )
        if (onRemove != null) {
            ChatoneIconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun HistoryRepeatRow(
    titles: List<String>,
    onSelect: (Int) -> Unit
) {
    if (titles.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            LocalStrings.current.creationRepeatFromHistory,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            titles.forEachIndexed { index, title ->
                Text(
                    title.ifBlank { "…" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 140.dp)
                        .clip(RoundedCornerShape(100))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { onSelect(index) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEntryButton(label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(accent.copy(alpha = 0.14f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = accent)
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

@Composable
private fun SubmitButton(label: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (enabled) accent else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        animationSpec = tween(160),
        label = "creationSubmit"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun CreationPanelFrame(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accent: Color,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 18.dp, top = 4.dp, bottom = 4.dp)
            .chatoneGlassPanel(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            ChatoneIconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
        content()
    }
}

@Composable
internal fun PollCreationPanel(
    history: List<Triple<String, List<String>, Int>> = emptyList(),
    onSubmit: (title: String, choices: List<String>, durationSeconds: Int) -> Unit,
    onClose: () -> Unit
) {
    val s = LocalStrings.current
    val accent = MaterialTheme.colorScheme.secondary
    var title by remember { mutableStateOf("") }
    var choices by remember { mutableStateOf(listOf("", "")) }
    var durationSeconds by remember { mutableStateOf(120) }

    CreationPanelFrame(
        icon = Icons.Outlined.HowToVote,
        title = s.pollCreateTitle,
        accent = accent,
        onClose = onClose
    ) {
        HistoryRepeatRow(history.map { it.first }) { index ->
            val (histTitle, histChoices, histDuration) = history[index]
            title = histTitle
            choices = histChoices
            durationSeconds = histDuration
        }

        ChatoneTextField(
            value = title,
            onValueChange = { title = it.take(60) },
            placeholder = s.pollCreateQuestionHint,
            modifier = Modifier.fillMaxWidth()
        )

        choices.forEachIndexed { index, choice ->
            ChoiceInputRow(
                value = choice,
                placeholder = "${s.pollCreateChoiceHint} ${index + 1}",
                onValueChange = { new -> choices = choices.toMutableList().also { it[index] = new.take(25) } },
                onRemove = if (choices.size > 2) {
                    { choices = choices.toMutableList().also { it.removeAt(index) } }
                } else null
            )
        }
        if (choices.size < 5) {
            AddEntryButton(s.pollCreateAddChoice, accent) { choices = choices + "" }
        }

        Text(
            s.pollCreateDuration,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DurationChipsRow(durationSeconds, accent) { durationSeconds = it }

        SubmitButton(
            label = s.pollCreateSubmit,
            accent = accent,
            enabled = title.isNotBlank() && choices.count { it.isNotBlank() } >= 2
        ) {
            onSubmit(title.trim(), choices.map { it.trim() }.filter { it.isNotBlank() }, durationSeconds)
        }
    }
}

@Composable
internal fun PredictionCreationPanel(
    history: List<Triple<String, List<String>, Int>> = emptyList(),
    onSubmit: (title: String, outcomes: List<String>, windowSeconds: Int) -> Unit,
    onClose: () -> Unit
) {
    val s = LocalStrings.current
    val accent = MaterialTheme.colorScheme.tertiary
    var title by remember { mutableStateOf("") }
    var outcomes by remember { mutableStateOf(listOf("", "")) }
    var windowSeconds by remember { mutableStateOf(120) }

    CreationPanelFrame(
        icon = Icons.Outlined.OnlinePrediction,
        title = s.predictionCreateTitle,
        accent = accent,
        onClose = onClose
    ) {
        HistoryRepeatRow(history.map { it.first }) { index ->
            val (histTitle, histOutcomes, histWindow) = history[index]
            title = histTitle
            outcomes = histOutcomes
            windowSeconds = histWindow
        }

        ChatoneTextField(
            value = title,
            onValueChange = { title = it.take(45) },
            placeholder = s.predictionCreateQuestionHint,
            modifier = Modifier.fillMaxWidth()
        )

        outcomes.forEachIndexed { index, outcome ->
            ChoiceInputRow(
                value = outcome,
                placeholder = "${s.predictionCreateOutcomeHint} ${index + 1}",
                onValueChange = { new -> outcomes = outcomes.toMutableList().also { it[index] = new.take(25) } },
                onRemove = if (outcomes.size > 2) {
                    { outcomes = outcomes.toMutableList().also { it.removeAt(index) } }
                } else null
            )
        }
        if (outcomes.size < 10) {
            AddEntryButton(s.predictionCreateAddOutcome, accent) { outcomes = outcomes + "" }
        }

        Text(
            s.predictionCreateWindow,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DurationChipsRow(windowSeconds, accent) { windowSeconds = it }

        SubmitButton(
            label = s.predictionCreateSubmit,
            accent = accent,
            enabled = title.isNotBlank() && outcomes.count { it.isNotBlank() } >= 2
        ) {
            onSubmit(title.trim(), outcomes.map { it.trim() }.filter { it.isNotBlank() }, windowSeconds)
        }
    }
}
