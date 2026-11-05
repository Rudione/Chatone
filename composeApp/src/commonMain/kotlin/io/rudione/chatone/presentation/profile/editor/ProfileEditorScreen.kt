package io.rudione.chatone.presentation.profile.editor

import org.jetbrains.compose.resources.getString
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.profile_editor_blocked_by_twitch
import chatone.composeapp.generated.resources.profile_editor_load_failed
import chatone.composeapp.generated.resources.profile_editor_nothing_changed
import chatone.composeapp.generated.resources.profile_editor_part_failed_accent
import chatone.composeapp.generated.resources.profile_editor_part_failed_badge
import chatone.composeapp.generated.resources.profile_editor_part_failed_color
import chatone.composeapp.generated.resources.profile_editor_part_failed_links
import chatone.composeapp.generated.resources.profile_editor_part_failed_profile
import chatone.composeapp.generated.resources.profile_editor_part_failed_seven_tv
import chatone.composeapp.generated.resources.profile_editor_part_failed_stream
import chatone.composeapp.generated.resources.profile_editor_retry
import chatone.composeapp.generated.resources.profile_editor_save
import chatone.composeapp.generated.resources.profile_editor_saved
import chatone.composeapp.generated.resources.profile_editor_signed_out
import chatone.composeapp.generated.resources.profile_editor_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.domain.profile.VanityPart
import io.rudione.chatone.domain.profile.VanitySaveResult
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.HeroPillButton
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TWITCH_PROFILE_SETTINGS = "https://www.twitch.tv/settings/profile"

@Composable
fun ProfileEditorScreen(channelLogin: String?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SystemBackHandler(onBack = onClose)
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProfileEditorPane(
            channelLogin = channelLogin,
            header = { title ->
                Row(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(Lucide.ArrowLeft, title, onClose)
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            },
            bottomInset = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ProfileEditorDialog(channelLogin: String?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 18.dp,
            modifier = Modifier.width(480.dp).heightIn(max = 760.dp)
        ) {
            ProfileEditorPane(
                channelLogin = channelLogin,
                header = { title ->
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        GlassIconButton(Lucide.X, title, onDismiss, size = 36.dp)
                    }
                },
                bottomInset = false
            )
        }
    }
}

@Composable
private fun ProfileEditorPane(
    channelLogin: String?,
    header: @Composable (String) -> Unit,
    bottomInset: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileEditorViewModel = koinViewModel(key = "profile_editor_${channelLogin.orEmpty()}") {
        parametersOf(channelLogin)
    }
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is ProfileEditorEffect.Saved) toast = effect.result.message()
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_800)
            toast = null
        }
    }
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            header(stringResource(Res.string.profile_editor_title))
            AnimatedContent(
                targetState = state.load,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "editorLoad",
                modifier = Modifier.weight(1f)
            ) { load ->
                val twitch = state.twitch
                val draft = state.draft
                when {
                    load == EditorLoad.READY && twitch != null && draft != null -> Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))
                        ColorSection(draft.colorHex) { viewModel.sendEvent(ProfileEditorEvent.PickColor(it)) }
                        TwitchBadgeSection(
                            twitch = twitch,
                            draft = draft,
                            onGlobal = { viewModel.sendEvent(ProfileEditorEvent.PickGlobalBadge(it)) },
                            onChannel = { viewModel.sendEvent(ProfileEditorEvent.PickChannelBadge(it)) }
                        )
                        SevenTvVanitySection(
                            link = state.sevenTv,
                            busy = state.sevenTvBusy,
                            draft = draft,
                            displayName = twitch.displayName,
                            onConnect = { viewModel.sendEvent(ProfileEditorEvent.ConnectSevenTv(it)) },
                            onDisconnect = { viewModel.sendEvent(ProfileEditorEvent.DisconnectSevenTv) },
                            onPaint = { viewModel.sendEvent(ProfileEditorEvent.PickPaint(it)) },
                            onBadge = { viewModel.sendEvent(ProfileEditorEvent.PickSevenTvBadge(it)) }
                        )
                        ProfileFieldsSection(
                            twitch = twitch,
                            draft = draft,
                            onName = { viewModel.sendEvent(ProfileEditorEvent.EditDisplayName(it)) },
                            onBio = { viewModel.sendEvent(ProfileEditorEvent.EditBio(it)) },
                            onOpenSettings = { runCatching { uriHandler.openUri(TWITCH_PROFILE_SETTINGS) } }
                        )
                        AccentSection(draft.accentHex) { viewModel.sendEvent(ProfileEditorEvent.PickAccent(it)) }
                        LinksSection(
                            links = draft.links,
                            onAdd = { title, url -> viewModel.sendEvent(ProfileEditorEvent.AddLink(title, url)) },
                            onRemove = { viewModel.sendEvent(ProfileEditorEvent.RemoveLink(it)) }
                        )
                        draft.stream?.let { stream ->
                            StreamSection(
                                stream = stream,
                                results = state.categoryResults,
                                onTitle = { viewModel.sendEvent(ProfileEditorEvent.EditStreamTitle(it)) },
                                onSearch = { viewModel.sendEvent(ProfileEditorEvent.SearchCategory(it)) },
                                onPick = { viewModel.sendEvent(ProfileEditorEvent.PickCategory(it)) },
                                onAddTag = { viewModel.sendEvent(ProfileEditorEvent.AddTag(it)) },
                                onRemoveTag = { viewModel.sendEvent(ProfileEditorEvent.RemoveTag(it)) },
                                onLanguage = { viewModel.sendEvent(ProfileEditorEvent.SetLanguage(it)) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    load == EditorLoad.LOADING -> Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    }
                    else -> Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (load == EditorLoad.SIGNED_OUT) stringResource(Res.string.profile_editor_signed_out) else stringResource(Res.string.profile_editor_load_failed), style = MaterialTheme.typography.bodyLarge)
                        if (load == EditorLoad.FAILED) TextButton(onClick = { viewModel.sendEvent(ProfileEditorEvent.Retry) }) { Text(stringResource(Res.string.profile_editor_retry)) }
                    }
                }
            }
            val twitch = state.twitch
            val draft = state.draft
            if (state.load == EditorLoad.READY && twitch != null && draft != null) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .then(if (bottomInset) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChatPreviewLine(twitch, state.sevenTvVanity, draft)
                    AnimatedVisibility(
                        visible = state.dirty || state.saving,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.sendEvent(ProfileEditorEvent.Discard) }) { Text(stringResource(Res.string.cancel)) }
                            HeroPillButton(
                                text = stringResource(Res.string.profile_editor_save),
                                onClick = { viewModel.sendEvent(ProfileEditorEvent.ReviewChanges) },
                                loading = state.saving,
                                height = 48.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        state.pendingChanges?.let { changes ->
            ChangesReviewDialog(
                changes = changes,
                onConfirm = { viewModel.sendEvent(ProfileEditorEvent.Save) },
                onDismiss = { viewModel.sendEvent(ProfileEditorEvent.CancelReview) }
            )
        }
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
                .then(if (bottomInset) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                .padding(bottom = 140.dp)
        ) {
            Text(
                toast.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.expressiveGlass(CircleShape).padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

private suspend fun VanitySaveResult.message(): String = when {
    isClean && saved.isEmpty() -> getString(Res.string.profile_editor_nothing_changed)
    isClean -> getString(Res.string.profile_editor_saved)
    blocked.isNotEmpty() && failed.isEmpty() -> getString(Res.string.profile_editor_blocked_by_twitch)
    else -> (failed + blocked).map { part ->
        when (part) {
            VanityPart.COLOR -> Res.string.profile_editor_part_failed_color
            VanityPart.BADGE -> Res.string.profile_editor_part_failed_badge
            VanityPart.SEVEN_TV -> Res.string.profile_editor_part_failed_seven_tv
            VanityPart.PROFILE -> Res.string.profile_editor_part_failed_profile
            VanityPart.ACCENT -> Res.string.profile_editor_part_failed_accent
            VanityPart.LINKS -> Res.string.profile_editor_part_failed_links
            VanityPart.STREAM -> Res.string.profile_editor_part_failed_stream
        }
    }.map { getString(it) }.joinToString("\n")
}
