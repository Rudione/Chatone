package io.rudione.chatone.presentation.chat.zoom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.ChatMessageScaleRepository
import io.rudione.chatone.domain.model.ChatMessageScale
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Minus
import io.rudione.chatone.icons.lucide.Plus

enum class ChatZoomCommand { ENLARGE, SHRINK, RESET }

fun chatZoomCommandOf(key: Key, primaryModifier: Boolean, altPressed: Boolean): ChatZoomCommand? {
    if (!primaryModifier || altPressed) return null
    return when (key) {
        Key.Equals, Key.Plus, Key.NumPadAdd -> ChatZoomCommand.ENLARGE
        Key.Minus, Key.NumPadSubtract -> ChatZoomCommand.SHRINK
        Key.Zero, Key.NumPad0 -> ChatZoomCommand.RESET
        else -> null
    }
}

private fun KeyEvent.chatZoomCommand(): ChatZoomCommand? =
    chatZoomCommandOf(key, isCtrlPressed || isMetaPressed, isAltPressed)

fun ChatMessageScaleRepository.apply(command: ChatZoomCommand) = when (command) {
    ChatZoomCommand.ENLARGE -> enlarge()
    ChatZoomCommand.SHRINK -> shrink()
    ChatZoomCommand.RESET -> reset()
}

@Composable
fun rememberChatMessageDensity(repository: ChatMessageScaleRepository = koinInject()): Density {
    val scale by repository.scale.collectAsState()
    val base = LocalDensity.current
    return remember(base, scale) {
        if (scale.isDefault) base else Density(base.density * scale.factor, base.fontScale)
    }
}

@Composable
fun ChatZoomShortcuts(repository: ChatMessageScaleRepository = koinInject()) {
    DisposableEffect(repository) {
        val unregister = GlobalKeyDispatcher.registerShortcut { event ->
            val command = event.chatZoomCommand() ?: return@registerShortcut false
            if (event.type == KeyEventType.KeyDown) repository.apply(command)
            true
        }
        onDispose { unregister() }
    }
}

@Composable
fun ChatZoomIndicator(
    modifier: Modifier = Modifier,
    repository: ChatMessageScaleRepository = koinInject()
) {
    val s = LocalStrings.current
    val scale by repository.scale.collectAsState()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var shownFor by remember { mutableStateOf<ChatMessageScale?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(scale) {
        if (shownFor == null) {
            shownFor = scale
            return@LaunchedEffect
        }
        shownFor = scale
        visible = true
    }
    LaunchedEffect(visible, hovered, scale) {
        if (!visible || hovered) return@LaunchedEffect
        delay(INDICATOR_VISIBLE_MS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)) + scaleIn(
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            initialScale = 0.86f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
        ),
        exit = fadeOut(tween(180)) + scaleOut(
            tween(180),
            targetScale = 0.92f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
        ),
        modifier = modifier
    ) {
        LiquidGlassSurface(
            modifier = Modifier.hoverable(interaction),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            backgroundAlphaHigh = 0.96f
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ChatoneIconButton(
                    onClick = { repository.shrink() },
                    enabled = scale.percent > ChatMessageScale.MIN_PERCENT,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Lucide.Minus, contentDescription = s.chatZoomOut, modifier = Modifier.size(18.dp))
                }
                Column(
                    modifier = Modifier.widthIn(min = 64.dp).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${scale.percent}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        s.chatZoomLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                ChatoneIconButton(
                    onClick = { repository.enlarge() },
                    enabled = scale.percent < ChatMessageScale.MAX_PERCENT,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Lucide.Plus, contentDescription = s.chatZoomIn, modifier = Modifier.size(18.dp))
                }
                if (!scale.isDefault) {
                    TextButton(onClick = { repository.reset() }) {
                        Text(s.chatZoomReset, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageScaleSettingRow(repository: ChatMessageScaleRepository = koinInject()) {
    val s = LocalStrings.current
    val scale by repository.scale.collectAsState()
    Column {
        SliderRow(
            label = s.settingsChatMessageScale,
            value = scale.percent.toFloat(),
            valueRange = ChatMessageScale.MIN_PERCENT.toFloat()..ChatMessageScale.MAX_PERCENT.toFloat(),
            steps = (ChatMessageScale.MAX_PERCENT - ChatMessageScale.MIN_PERCENT) / ChatMessageScale.STEP_PERCENT - 1,
            valueLabel = "${scale.percent}%"
        ) { repository.set(ChatMessageScale.of(it)) }
        if (isDesktopPlatform) {
            HighlightedSettingsText(
                s.settingsChatMessageScaleHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            )
        }
    }
}

private const val INDICATOR_VISIBLE_MS = 1_600L
