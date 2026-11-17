package io.rudione.chatone.presentation.chat.guard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.forbidden_category_ableist
import chatone.composeapp.generated.resources.forbidden_category_ethnic
import chatone.composeapp.generated.resources.forbidden_category_homophobic
import chatone.composeapp.generated.resources.forbidden_category_racial
import chatone.composeapp.generated.resources.forbidden_category_transphobic
import chatone.composeapp.generated.resources.forbidden_word_edit
import chatone.composeapp.generated.resources.forbidden_word_hold
import chatone.composeapp.generated.resources.forbidden_word_holding
import chatone.composeapp.generated.resources.forbidden_word_text
import chatone.composeapp.generated.resources.forbidden_word_title
import io.rudione.chatone.domain.moderation.ForbiddenCategory
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.SendHorizontal
import io.rudione.chatone.icons.lucide.TriangleAlert
import io.rudione.chatone.presentation.chat.rememberNickColors
import io.rudione.chatone.presentation.components.expressive.HoldToConfirmButton
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ForbiddenSendDialog(
    guard: ForbiddenSendGuard,
    authorName: String,
    authorLogin: String,
    authorColor: String?
) {
    val prompt = guard.prompt ?: return
    Dialog(
        onDismissRequest = guard::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ForbiddenSendCard(
            prompt = prompt,
            authorName = authorName,
            authorLogin = authorLogin,
            authorColor = authorColor,
            onSend = guard::confirm,
            onEdit = guard::dismiss
        )
    }
}

@Composable
private fun ForbiddenSendCard(
    prompt: ForbiddenSendPrompt,
    authorName: String,
    authorLogin: String,
    authorColor: String?,
    onSend: () -> Unit,
    onEdit: () -> Unit
) {
    val appear = remember(prompt) { Animatable(0f) }
    val shake = remember(prompt) { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val shakePx = with(LocalDensity.current) { SHAKE_DISTANCE.toPx() }
    LaunchedEffect(prompt) {
        launch { appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
        if (!isDesktopPlatform) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(DOUBLE_BUZZ_GAP_MS)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        delay(SHAKE_DELAY_MS)
        shake.animateTo(1f, tween(SHAKE_MS, easing = FastOutSlowInEasing))
    }
    val error = MaterialTheme.colorScheme.error
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardShape = RoundedCornerShape(30.dp)

    Column(
        modifier = Modifier
            .widthIn(max = CARD_MAX_WIDTH)
            .fillMaxWidth(CARD_WIDTH_FRACTION)
            .graphicsLayer {
                val value = appear.value
                alpha = value.coerceIn(0f, 1f)
                val scale = 0.9f + 0.1f * value
                scaleX = scale
                scaleY = scale
            }
            .expressiveGlass(cardShape, elevation = 24.dp)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(error.copy(alpha = 0.16f), Color.Transparent),
                        endY = size.height * 0.5f
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScallopBadge(
            icon = Lucide.TriangleAlert,
            size = 68.dp,
            container = error.copy(alpha = 0.22f).compositeOver(surface),
            content = error
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(Res.string.forbidden_word_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.forbidden_word_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        MessagePreview(
            prompt = prompt,
            authorName = authorName,
            authorLogin = authorLogin,
            authorColor = authorColor,
            modifier = Modifier.graphicsLayer {
                val progress = shake.value
                translationX = sin(progress * SHAKE_WAVES * 2f * PI.toFloat()) * (1f - progress) * shakePx
            }
        )
        Spacer(Modifier.height(10.dp))
        CategoryChips(prompt)
        Spacer(Modifier.height(18.dp))
        HoldToConfirmButton(
            text = stringResource(Res.string.forbidden_word_hold),
            holdingText = stringResource(Res.string.forbidden_word_holding),
            onConfirmed = onSend,
            icon = Lucide.SendHorizontal,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onEdit) {
            Text(
                stringResource(Res.string.forbidden_word_edit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MessagePreview(
    prompt: ForbiddenSendPrompt,
    authorName: String,
    authorLogin: String,
    authorColor: String?,
    modifier: Modifier = Modifier
) {
    val error = MaterialTheme.colorScheme.error
    val separator = MaterialTheme.colorScheme.onSurfaceVariant
    val nameColor = rememberNickColors().of(authorColor, authorLogin.ifBlank { authorName })
    val glow by rememberInfiniteTransition(label = "forbiddenGlow").animateFloat(
        initialValue = 0.14f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(tween(GLOW_MS), RepeatMode.Reverse),
        label = "forbiddenGlowAlpha"
    )
    val text = prompt.text
    val line = buildAnnotatedString {
        if (authorName.isNotBlank()) {
            withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) { append(authorName) }
            withStyle(SpanStyle(color = separator)) { append(": ") }
        }
        var cursor = 0
        prompt.hits.forEach { hit ->
            if (hit.start < cursor) return@forEach
            append(text.substring(cursor, hit.start))
            withStyle(SpanStyle(color = error, background = error.copy(alpha = glow), fontWeight = FontWeight.Bold)) {
                append(text.substring(hit.start, hit.end))
            }
            cursor = hit.end
        }
        append(text.substring(cursor))
    }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f))
            .border(1.dp, error.copy(alpha = 0.24f), shape)
            .heightIn(max = PREVIEW_MAX_HEIGHT)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(prompt: ForbiddenSendPrompt) {
    val categories = remember(prompt) { prompt.hits.map { it.category }.distinct() }
    val error = MaterialTheme.colorScheme.error
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(error.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(error))
                Text(category.label(), style = MaterialTheme.typography.labelMedium, color = error, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ForbiddenCategory.label(): String = stringResource(
    when (this) {
        ForbiddenCategory.HOMOPHOBIC -> Res.string.forbidden_category_homophobic
        ForbiddenCategory.TRANSPHOBIC -> Res.string.forbidden_category_transphobic
        ForbiddenCategory.RACIAL -> Res.string.forbidden_category_racial
        ForbiddenCategory.ETHNIC -> Res.string.forbidden_category_ethnic
        ForbiddenCategory.ABLEIST -> Res.string.forbidden_category_ableist
    }
)

private val CARD_MAX_WIDTH = 380.dp
private const val CARD_WIDTH_FRACTION = 0.92f
private val PREVIEW_MAX_HEIGHT = 150.dp
private val SHAKE_DISTANCE = 12.dp
private const val SHAKE_WAVES = 3f
private const val SHAKE_MS = 520
private const val SHAKE_DELAY_MS = 120L
private const val DOUBLE_BUZZ_GAP_MS = 160L
private const val GLOW_MS = 900
