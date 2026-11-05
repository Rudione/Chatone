package io.rudione.chatone.tv.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.follow_picker_empty
import chatone.composeapp.generated.resources.follow_picker_failed
import chatone.composeapp.generated.resources.follow_picker_offline
import chatone.composeapp.generated.resources.follow_picker_option_desc
import chatone.composeapp.generated.resources.follow_picker_option_title
import chatone.composeapp.generated.resources.follow_picker_signed_out
import chatone.composeapp.generated.resources.main_add_channel
import chatone.composeapp.generated.resources.tv_add_channel_already
import chatone.composeapp.generated.resources.tv_add_channel_by_name
import chatone.composeapp.generated.resources.tv_add_channel_by_name_desc
import chatone.composeapp.generated.resources.tv_add_channel_count
import chatone.composeapp.generated.resources.tv_add_channel_invalid
import chatone.composeapp.generated.resources.tv_add_channel_placeholder
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.follow.FollowedChannel
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Hash
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.browse.BrowseFormatting
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.main.follows.FollowedPickerState
import io.rudione.chatone.presentation.main.follows.FollowedPickerStatus
import io.rudione.chatone.presentation.main.follows.rememberFollowedPickerState
import io.rudione.chatone.tv.components.TvButtonTone
import io.rudione.chatone.tv.components.TvLoading
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.TvPillButton
import io.rudione.chatone.tv.components.claim
import io.rudione.chatone.util.link.TwitchLinks
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource

private enum class AddStep { CHOOSE, NAME, FOLLOWS }

@Composable
internal fun TvAddChannelPanel(
    openLogins: Set<String>,
    onAdd: (List<MainEvent.AddChannel>) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(AddStep.CHOOSE) }
    SystemBackHandler(enabled = step != AddStep.CHOOSE) { step = AddStep.CHOOSE }
    Text(
        stringResource(Res.string.main_add_channel),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    when (step) {
        AddStep.CHOOSE -> TvMenuList(
            listOf(
                TvMenuEntry(
                    Lucide.Hash,
                    stringResource(Res.string.tv_add_channel_by_name),
                    supporting = stringResource(Res.string.tv_add_channel_by_name_desc)
                ) { step = AddStep.NAME },
                TvMenuEntry(
                    Lucide.Heart,
                    stringResource(Res.string.follow_picker_option_title),
                    supporting = stringResource(Res.string.follow_picker_option_desc)
                ) { step = AddStep.FOLLOWS },
                TvMenuEntry(Lucide.X, stringResource(Res.string.cancel), onClick = onCancel)
            )
        )

        AddStep.NAME -> TvChannelNameForm(
            onAdd = { login -> onAdd(listOf(MainEvent.AddChannel(login))) },
            onCancel = { step = AddStep.CHOOSE }
        )

        AddStep.FOLLOWS -> TvFollowsPicker(
            picker = rememberFollowedPickerState(openLogins),
            onAdd = { picked -> onAdd(picked.map { MainEvent.AddChannel(it.login, it.avatarUrl, it.displayName) }) },
            onCancel = { step = AddStep.CHOOSE }
        )
    }
}

@Composable
private fun TvChannelNameForm(onAdd: (String) -> Unit, onCancel: () -> Unit) {
    val name = rememberTextFieldState()
    var invalid by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val textFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    val submit = {
        val login = TwitchLinks.channelLogin(name.text.toString())
        if (login != null) onAdd(login) else invalid = true
    }
    LaunchedEffect(name) {
        snapshotFlow { name.text.toString() }.drop(1).collect { invalid = false }
    }
    Surface(
        onClick = { textFocus.requestFocus() },
        modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, scheme.onSurface), shape = CircleShape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = scheme.surfaceVariant,
            focusedContainerColor = scheme.surfaceVariant
        )
    ) {
        BasicTextField(
            state = name,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = { submit() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .focusRequester(textFocus)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        event.key == Key.DirectionUp -> fieldFocus.requestFocus()
                        else -> false
                    }
                },
            decorator = { field ->
                if (name.text.isEmpty()) {
                    Text(
                        stringResource(Res.string.tv_add_channel_placeholder),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                field()
            }
        )
    }
    if (invalid) {
        Text(
            stringResource(Res.string.tv_add_channel_invalid),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.error
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvPillButton(
            stringResource(Res.string.main_add_channel),
            onClick = submit,
            enabled = name.text.isNotBlank()
        )
        TvPillButton(stringResource(Res.string.cancel), onClick = onCancel, tone = TvButtonTone.Tonal)
    }
    LaunchedEffect(Unit) { fieldFocus.claim() }
}

@Composable
private fun TvFollowsPicker(
    picker: FollowedPickerState,
    onAdd: (List<FollowedChannel>) -> Unit,
    onCancel: () -> Unit
) {
    val firstItem = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    val ready = picker.status == FollowedPickerStatus.Ready && picker.visible.isNotEmpty()
    LaunchedEffect(picker) { picker.load() }
    LaunchedEffect(ready) { if (ready) firstItem.claim() else cancelFocus.claim() }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvPillButton(
            stringResource(Res.string.tv_add_channel_count, picker.selectedCount),
            onClick = { onAdd(picker.picked()) },
            enabled = picker.selectedCount > 0,
            modifier = Modifier.focusRequester(addFocus)
        )
        TvPillButton(
            stringResource(Res.string.cancel),
            onClick = onCancel,
            tone = TvButtonTone.Tonal,
            modifier = Modifier.focusRequester(cancelFocus)
        )
    }
    when (picker.status) {
        FollowedPickerStatus.Loading -> TvLoading()
        FollowedPickerStatus.SignedOut -> TvMessage(stringResource(Res.string.follow_picker_signed_out), claimsFocus = false)
        FollowedPickerStatus.Failed -> TvMessage(
            stringResource(Res.string.follow_picker_failed),
            onRetry = { picker.load(force = true) },
            claimsFocus = false
        )

        FollowedPickerStatus.Ready -> if (picker.visible.isEmpty()) {
            TvMessage(stringResource(Res.string.follow_picker_empty), claimsFocus = false)
        } else {
            LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(picker.visible, key = { _, channel -> channel.login }) { index, channel ->
                    TvFollowedRow(
                        channel = channel,
                        selected = picker.isSelected(channel.login),
                        alreadyAdded = picker.isOpen(channel.login),
                        onClick = { picker.toggle(channel.login) },
                        modifier = if (index == 0) {
                            Modifier
                                .focusRequester(firstItem)
                                .focusProperties { up = if (picker.selectedCount > 0) addFocus else cancelFocus }
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvFollowedRow(
    channel: FollowedChannel,
    selected: Boolean,
    alreadyAdded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val viewers = channel.liveViewers
    val status = when {
        alreadyAdded -> stringResource(Res.string.tv_add_channel_already)
        viewers != null -> listOfNotNull(
            channel.liveCategory?.takeIf { it.isNotBlank() },
            BrowseFormatting.viewers(viewers)
        ).joinToString(" · ")

        else -> stringResource(Res.string.follow_picker_offline)
    }
    ListItem(
        selected = selected,
        onClick = onClick,
        enabled = !alreadyAdded,
        modifier = modifier,
        shape = ListItemDefaults.shape(RoundedCornerShape(20.dp)),
        leadingContent = {
            AsyncImage(
                model = channel.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(scheme.surfaceVariant)
            )
        },
        headlineContent = {
            Text(channel.displayName.ifBlank { channel.login }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { Text(status, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = if (selected) {
            { Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(ListItemDefaults.IconSize)) }
        } else {
            null
        }
    )
}
