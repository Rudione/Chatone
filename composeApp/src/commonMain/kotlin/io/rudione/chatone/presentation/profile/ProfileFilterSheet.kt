package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.channel_page_kind_all
import chatone.composeapp.generated.resources.channel_page_kind_archive
import chatone.composeapp.generated.resources.channel_page_kind_highlight
import chatone.composeapp.generated.resources.channel_page_kind_upload
import chatone.composeapp.generated.resources.channel_page_period_all
import chatone.composeapp.generated.resources.channel_page_period_day
import chatone.composeapp.generated.resources.channel_page_period_month
import chatone.composeapp.generated.resources.channel_page_period_week
import chatone.composeapp.generated.resources.channel_page_sort_new
import chatone.composeapp.generated.resources.channel_page_sort_popular
import chatone.composeapp.generated.resources.channel_page_sort_top
import chatone.composeapp.generated.resources.channel_page_sort_trending
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_apply
import chatone.composeapp.generated.resources.channel_page_featured_only
import chatone.composeapp.generated.resources.channel_page_filters
import chatone.composeapp.generated.resources.channel_page_period
import chatone.composeapp.generated.resources.channel_page_reset
import chatone.composeapp.generated.resources.channel_page_sort
import chatone.composeapp.generated.resources.channel_page_video_kind
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.profile.ClipOrder
import io.rudione.chatone.domain.profile.ClipPeriod
import io.rudione.chatone.domain.profile.ClipQuery
import io.rudione.chatone.domain.profile.VideoKind
import io.rudione.chatone.domain.profile.VideoOrder
import io.rudione.chatone.domain.profile.VideoQuery
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.ExpressiveSheet

@Immutable
internal class FilterChoice<T>(val value: T, val label: String)

@Composable
internal fun clipPeriodChoices() = listOf(
    FilterChoice(ClipPeriod.DAY, stringResource(Res.string.channel_page_period_day)),
    FilterChoice(ClipPeriod.WEEK, stringResource(Res.string.channel_page_period_week)),
    FilterChoice(ClipPeriod.MONTH, stringResource(Res.string.channel_page_period_month)),
    FilterChoice(ClipPeriod.ALL, stringResource(Res.string.channel_page_period_all))
)

@Composable
internal fun clipOrderChoices() = listOf(
    FilterChoice(ClipOrder.TOP, stringResource(Res.string.channel_page_sort_top)),
    FilterChoice(ClipOrder.TRENDING, stringResource(Res.string.channel_page_sort_trending))
)

@Composable
internal fun videoKindChoices() = listOf(
    FilterChoice<VideoKind?>(null, stringResource(Res.string.channel_page_kind_all)),
    FilterChoice<VideoKind?>(VideoKind.ARCHIVE, stringResource(Res.string.channel_page_kind_archive)),
    FilterChoice<VideoKind?>(VideoKind.HIGHLIGHT, stringResource(Res.string.channel_page_kind_highlight)),
    FilterChoice<VideoKind?>(VideoKind.UPLOAD, stringResource(Res.string.channel_page_kind_upload))
)

@Composable
internal fun videoOrderChoices() = listOf(
    FilterChoice(VideoOrder.NEWEST, stringResource(Res.string.channel_page_sort_new)),
    FilterChoice(VideoOrder.POPULAR, stringResource(Res.string.channel_page_sort_popular))
)

@Composable
internal fun ClipFilterSheet(initial: ClipQuery, onApply: (ClipQuery) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    FilterSheetScaffold(
        activeCount = draft.activeCount(),
        onReset = { draft = ClipQuery() },
        onApply = { onApply(draft) },
        onDismiss = onDismiss
    ) {
        ChoiceGroup(stringResource(Res.string.channel_page_period), clipPeriodChoices(), draft.period) { draft = draft.copy(period = it) }
        ChoiceGroup(stringResource(Res.string.channel_page_sort), clipOrderChoices(), draft.order) { draft = draft.copy(order = it) }
        SwitchCard(stringResource(Res.string.channel_page_featured_only), draft.featuredOnly) { draft = draft.copy(featuredOnly = it) }
    }
}

@Composable
internal fun VideoFilterSheet(initial: VideoQuery, onApply: (VideoQuery) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    FilterSheetScaffold(
        activeCount = draft.activeCount(),
        onReset = { draft = VideoQuery() },
        onApply = { onApply(draft) },
        onDismiss = onDismiss
    ) {
        ChoiceGroup(stringResource(Res.string.channel_page_video_kind), videoKindChoices(), draft.kind) { draft = draft.copy(kind = it) }
        ChoiceGroup(stringResource(Res.string.channel_page_sort), videoOrderChoices(), draft.order) { draft = draft.copy(order = it) }
    }
}

@Composable
private fun FilterSheetScaffold(
    activeCount: Int,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ExpressiveSheet(onDismissRequest = onDismiss) { sheet ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(Res.string.channel_page_filters), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            AnimatedVisibility(visible = activeCount > 0, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        activeCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset, enabled = activeCount > 0) { Text(stringResource(Res.string.channel_page_reset)) }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            content()
        }
        PillButton(
            text = stringResource(Res.string.channel_page_apply),
            onClick = { sheet.close(onApply) },
            height = 52.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    title: String,
    choices: List<FilterChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { choice -> ChoicePill(choice.label, choice.value == selected) { onSelect(choice.value) } }
        }
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(180),
        label = "choiceContainer"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tween(180),
        label = "choiceContent"
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Lucide.Check, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = content)
    }
}

@Composable
private fun SwitchCard(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        ChatoneSwitch(checked = checked, onCheckedChange = onChange)
    }
}

internal fun ClipQuery.activeCount(): Int =
    listOf(period != ClipPeriod.ALL, order != ClipOrder.TOP, featuredOnly).count { it }

internal fun VideoQuery.activeCount(): Int = listOf(kind != null, order != VideoOrder.NEWEST).count { it }
