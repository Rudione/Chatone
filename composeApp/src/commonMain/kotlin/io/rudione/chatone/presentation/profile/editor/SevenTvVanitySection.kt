package io.rudione.chatone.presentation.profile.editor

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_editor_section_seven_tv
import chatone.composeapp.generated.resources.profile_editor_seven_tv_badge
import chatone.composeapp.generated.resources.profile_editor_seven_tv_command_copied
import chatone.composeapp.generated.resources.profile_editor_seven_tv_connect
import chatone.composeapp.generated.resources.profile_editor_seven_tv_connected
import chatone.composeapp.generated.resources.profile_editor_seven_tv_copy_command
import chatone.composeapp.generated.resources.profile_editor_seven_tv_disconnect
import chatone.composeapp.generated.resources.profile_editor_seven_tv_empty
import chatone.composeapp.generated.resources.profile_editor_seven_tv_manual
import chatone.composeapp.generated.resources.profile_editor_seven_tv_none
import chatone.composeapp.generated.resources.profile_editor_seven_tv_open_site
import chatone.composeapp.generated.resources.profile_editor_seven_tv_other_account
import chatone.composeapp.generated.resources.profile_editor_seven_tv_paint
import chatone.composeapp.generated.resources.profile_editor_seven_tv_rejected
import chatone.composeapp.generated.resources.profile_editor_seven_tv_sign_in
import chatone.composeapp.generated.resources.profile_editor_seven_tv_sign_in_hint
import chatone.composeapp.generated.resources.profile_editor_seven_tv_step_command
import chatone.composeapp.generated.resources.profile_editor_seven_tv_step_open
import chatone.composeapp.generated.resources.profile_editor_seven_tv_step_paste
import chatone.composeapp.generated.resources.profile_editor_seven_tv_token
import chatone.composeapp.generated.resources.profile_editor_seven_tv_token_hint
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.profile.SevenTvLink
import io.rudione.chatone.domain.profile.SevenTvVanity
import io.rudione.chatone.domain.profile.VanityDraft
import io.rudione.chatone.presentation.chat.ProfileDisplayName
import io.rudione.chatone.presentation.components.ChatoneSecretField
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.launch

private val SevenTvColor = Color(0xFF29D8F6)
private const val TOKEN_COMMAND = "(()=>{copy(localStorage.getItem('7tv-token'));return 'ok'})()"

@Composable
internal fun SevenTvVanitySection(
    link: SevenTvLink,
    busy: Boolean,
    draft: VanityDraft,
    displayName: String,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onPaint: (String?) -> Unit,
    onBadge: (String?) -> Unit
) {
    EditorSection(stringResource(Res.string.profile_editor_section_seven_tv)) {
        AnimatedContent(
            targetState = link is SevenTvLink.Connected,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "sevenTvLink"
        ) { connected ->
            if (connected && link is SevenTvLink.Connected) {
                Inventory(link.vanity, draft, displayName, onDisconnect, onPaint, onBadge)
            } else {
                ConnectForm(link, busy, onConnect)
            }
        }
    }
}

@Composable
private fun ConnectForm(link: SevenTvLink, busy: Boolean, onConnect: (String) -> Unit) {
    var signingIn by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(!sevenTvSignInSupported) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (sevenTvSignInSupported) {
            Hint(stringResource(Res.string.profile_editor_seven_tv_sign_in_hint))
            if (link == SevenTvLink.Rejected || link == SevenTvLink.OtherAccount || link == SevenTvLink.Unreachable) {
                Text(
                    if (link == SevenTvLink.OtherAccount) stringResource(Res.string.profile_editor_seven_tv_other_account) else stringResource(
                        Res.string.profile_editor_seven_tv_rejected
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            PillButton(
                text = stringResource(Res.string.profile_editor_seven_tv_sign_in),
                onClick = { signingIn = true },
                loading = busy,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth()
            )
            if (!manual) {
                PillButton(
                    text = stringResource(Res.string.profile_editor_seven_tv_manual),
                    onClick = { manual = true },
                    tone = PillTone.Ghost,
                    height = 36.dp
                )
            }
        }
        AnimatedVisibility(
            visible = manual,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ManualTokenForm(link, busy, onConnect)
        }
    }
    if (signingIn) {
        SevenTvSignInDialog(
            onToken = { token ->
                signingIn = false
                onConnect(token)
            },
            onDismiss = { signingIn = false }
        )
    }
}

@Composable
private fun ManualTokenForm(link: SevenTvLink, busy: Boolean, onConnect: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Step(1, stringResource(Res.string.profile_editor_seven_tv_step_open))
        Step(2, stringResource(Res.string.profile_editor_seven_tv_step_command))
        Step(3, stringResource(Res.string.profile_editor_seven_tv_step_paste))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = stringResource(Res.string.profile_editor_seven_tv_open_site),
                onClick = { runCatching { uriHandler.openUri("https://7tv.app") } },
                tone = PillTone.Tonal,
                height = 40.dp,
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = if (copied) stringResource(Res.string.profile_editor_seven_tv_command_copied) else stringResource(
                    Res.string.profile_editor_seven_tv_copy_command
                ),
                onClick = {
                    scope.launch { clipboard.setText(TOKEN_COMMAND) }
                    copied = true
                },
                tone = PillTone.Tonal,
                height = 40.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        ChatoneSecretField(
            value = token,
            onValueChange = { token = it.take(4096) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.profile_editor_seven_tv_token),
            hint = when (link) {
                SevenTvLink.Rejected, SevenTvLink.Unreachable -> stringResource(Res.string.profile_editor_seven_tv_rejected)
                SevenTvLink.OtherAccount -> stringResource(Res.string.profile_editor_seven_tv_other_account)
                else -> stringResource(Res.string.profile_editor_seven_tv_token_hint)
            },
            isError = link == SevenTvLink.Rejected || link == SevenTvLink.OtherAccount || link == SevenTvLink.Unreachable
        )
        PillButton(
            text = stringResource(Res.string.profile_editor_seven_tv_connect),
            onClick = { onConnect(token) },
            enabled = token.isNotBlank(),
            loading = busy,
            height = 46.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(SevenTvColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SevenTvColor
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Inventory(
    vanity: SevenTvVanity,
    draft: VanityDraft,
    displayName: String,
    onDisconnect: () -> Unit,
    onPaint: (String?) -> Unit,
    onBadge: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    Res.string.profile_editor_seven_tv_connected,
                    vanity.displayName.ifBlank { displayName }),
                style = MaterialTheme.typography.bodyMedium,
                color = SevenTvColor,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = stringResource(Res.string.profile_editor_seven_tv_disconnect),
                onClick = onDisconnect,
                tone = PillTone.Ghost,
                height = 36.dp
            )
        }
        if (vanity.paints.isEmpty() && vanity.badges.isEmpty()) {
            Hint(stringResource(Res.string.profile_editor_seven_tv_empty))
            return@Column
        }
        if (vanity.paints.isNotEmpty()) {
            SubTitle(stringResource(Res.string.profile_editor_seven_tv_paint))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaintChip(
                    null,
                    displayName,
                    stringResource(Res.string.profile_editor_seven_tv_none),
                    draft.paintId == null
                ) { onPaint(null) }
                vanity.paints.forEach { paint ->
                    PaintChip(paint, displayName, paint.name, draft.paintId == paint.id) {
                        onPaint(
                            paint.id
                        )
                    }
                }
            }
        }
        if (vanity.badges.isNotEmpty()) {
            SubTitle(stringResource(Res.string.profile_editor_seven_tv_badge))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoneTile(draft.sevenTvBadgeId == null) { onBadge(null) }
                vanity.badges.forEach { badge ->
                    SelectableTile(draft.sevenTvBadgeId == badge.id, { onBadge(badge.id) }) {
                        AsyncImage(
                            model = badge.url3x.ifEmpty { badge.url2x },
                            contentDescription = badge.name,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaintChip(
    paint: io.rudione.chatone.domain.model.SevenTvCosmetics.Paint?,
    displayName: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(180),
        label = "paintBorder"
    )
    Column(
        Modifier.width(132.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ProfileDisplayName(
            displayName = displayName,
            color = null,
            paint = paint,
            style = MaterialTheme.typography.titleSmall,
            fallbackLogin = displayName
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
