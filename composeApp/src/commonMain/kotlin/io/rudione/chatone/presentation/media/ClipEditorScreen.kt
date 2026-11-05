package io.rudione.chatone.presentation.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.clip_preparing_hint
import chatone.composeapp.generated.resources.media_blocked
import chatone.composeapp.generated.resources.media_clip_banned
import chatone.composeapp.generated.resources.media_close
import chatone.composeapp.generated.resources.media_create_failed
import chatone.composeapp.generated.resources.media_create_title
import chatone.composeapp.generated.resources.media_forbidden
import chatone.composeapp.generated.resources.media_needs_rights
import chatone.composeapp.generated.resources.media_not_live
import chatone.composeapp.generated.resources.media_preparing
import chatone.composeapp.generated.resources.media_restricted
import chatone.composeapp.generated.resources.media_retry
import chatone.composeapp.generated.resources.media_signed_out
import chatone.composeapp.generated.resources.media_throttled
import chatone.composeapp.generated.resources.media_twitch_error
import io.rudione.chatone.domain.media.ClipOrigin
import io.rudione.chatone.domain.media.ClipFailure
import io.rudione.chatone.icons.lucide.Clapperboard
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.TriangleAlert
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.FitText
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.MorphingLoadingIndicator
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.WavyLinearProgress
import io.rudione.chatone.presentation.components.rememberScopedViewModelStoreOwner
import io.rudione.chatone.presentation.stream.ambient.AmbienceFromImage
import io.rudione.chatone.presentation.stream.ambient.StreamAmbience
import io.rudione.chatone.util.platform.currentFormFactor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Immutable
class ClipEditorNavigation(val onClose: () -> Unit, val onWatch: (String) -> Unit)

private val CardShape = RoundedCornerShape(30.dp)
private const val CARD_ALPHA = 0.94f
private const val SCRIM_ALPHA = 0.5f

@Composable
fun ClipEditorScreen(
    login: String,
    origin: ClipOrigin,
    navigation: ClipEditorNavigation,
    modifier: Modifier = Modifier
) {
    val session = rememberScopedViewModelStoreOwner()
    val viewModel: ClipEditorViewModel = koinViewModel(viewModelStoreOwner = session) {
        parametersOf(login, origin)
    }
    val state by viewModel.state.collectAsState()
    val ambience = remember { StreamAmbience() }
    LaunchedEffect(ambience) { ambience.animate() }
    state.draft?.thumbnailUrl?.let { AmbienceFromImage(it, ambience) }
    SystemBackHandler(onBack = navigation.onClose)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            1f,
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        )
    }
    val closable = state.phase != ClipEditorPhase.PUBLISHING
    val remoteControl = remember { currentFormFactor().isLeanBack }
    val cardFocus = remember { FocusRequester() }
    LaunchedEffect(state.phase, remoteControl) {
        if (remoteControl) {
            withFrameNanos { }
            runCatching { cardFocus.requestFocus() }
        }
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = closable
                ) {
                    navigation.onClose()
                }
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val progress = appear.value
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = 0.92f + 0.08f * progress
                    scaleY = 0.92f + 0.08f * progress
                    translationY = (1f - progress) * 48.dp.toPx()
                }
                .shadow(28.dp, CardShape)
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = CARD_ALPHA))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    ),
                    CardShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !remoteControl
                ) {}
                .focusRequester(cardFocus)
                .focusGroup()
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                ClipHeader(
                    title = stringResource(Res.string.media_create_title),
                    channel = state.channelName,
                    closeLabel = stringResource(Res.string.media_close),
                    onClose = navigation.onClose
                )
                AnimatedContent(
                    targetState = state.phase,
                    contentKey = { if (it == ClipEditorPhase.PUBLISHING) ClipEditorPhase.EDITING else it },
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(140)) },
                    label = "clipEditor"
                ) { phase ->
                    when (phase) {
                        ClipEditorPhase.PREPARING -> Centered {
                            MorphingLoadingIndicator(size = 76.dp)
                            Text(
                                stringResource(Res.string.media_preparing),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(Res.string.clip_preparing_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            WavyLinearProgress(Modifier.widthIn(max = 260.dp).padding(top = 6.dp))
                        }

                        ClipEditorPhase.FAILED -> Centered {
                            ScallopBadge(
                                Lucide.TriangleAlert,
                                size = 84.dp,
                                animated = false,
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                failureText(state.failure),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            ClipActions {
                                PillButton(
                                    stringResource(Res.string.media_retry),
                                    onClick = { viewModel.sendEvent(ClipEditorEvent.Retry) },
                                    height = 46.dp
                                )
                                PillButton(
                                    stringResource(Res.string.media_close),
                                    onClick = navigation.onClose,
                                    tone = PillTone.Tonal,
                                    height = 46.dp
                                )
                            }
                        }

                        ClipEditorPhase.DONE -> PublishedPane(
                            state.publishedSlug.orEmpty(),
                            state.title,
                            state.draft?.thumbnailUrl.orEmpty(),
                            ambience,
                            navigation
                        )

                        else -> EditingPane(state, viewModel, ambience)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipHeader(title: String, channel: String, closeLabel: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScallopBadge(Lucide.Clapperboard, size = 40.dp, animated = false, iconFraction = 0.46f)
        Column(Modifier.weight(1f)) {
            FitText(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (channel.isNotBlank()) {
                Text(
                    channel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        GlassIconButton(Lucide.X, closeLabel, onClose, size = 40.dp)
    }
}

@Composable
internal fun Centered(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 320.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClipActions(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
internal fun failureText(failure: ClipFailure?): String = when (failure) {
    ClipFailure.SIGNED_OUT -> stringResource(Res.string.media_signed_out)
    ClipFailure.NOT_LIVE -> stringResource(Res.string.media_not_live)
    ClipFailure.BLOCKED -> stringResource(Res.string.media_blocked)
    ClipFailure.FORBIDDEN -> stringResource(Res.string.media_forbidden)
    ClipFailure.BANNED -> stringResource(Res.string.media_clip_banned)
    ClipFailure.RESTRICTED -> stringResource(Res.string.media_restricted)
    ClipFailure.THROTTLED -> stringResource(Res.string.media_throttled)
    ClipFailure.NEEDS_RIGHTS -> stringResource(Res.string.media_needs_rights)
    ClipFailure.TWITCH_ERROR -> stringResource(Res.string.media_twitch_error)
    ClipFailure.FAILED, null -> stringResource(Res.string.media_create_failed)
}
