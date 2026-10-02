package io.rudione.chatone.presentation.main.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Compass
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare
import io.rudione.chatone.icons.lucide.Plus
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.presentation.components.SwipeDirection
import io.rudione.chatone.presentation.components.directionalSwipe
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.expressive.touchHaze
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun EmptyState(
    isGuest: Boolean,
    onAddChannel: () -> Unit,
    onLogin: () -> Unit,
    onBrowse: (() -> Unit)? = null,
    onOpenChannels: (() -> Unit)? = null
) {
    val s = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .ambientHaze(base = Color.Transparent, strength = HAZE_STRENGTH, animated = !isDesktopPlatform)
            .touchHaze(tint = MaterialTheme.colorScheme.primary, strength = HAZE_STRENGTH)
            .then(
                if (onOpenChannels != null) Modifier.directionalSwipe(SwipeDirection.Right, onOpenChannels)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScallopBadge(icon = Lucide.MessagesSquare, size = 136.dp, animated = !isDesktopPlatform)
            Spacer(Modifier.height(28.dp))
            Text(
                s.noChannelsOpen,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                s.mainNoChannelsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(28.dp))
            PillButton(
                text = s.mainAddChannelTitle,
                onClick = onAddChannel,
                icon = Lucide.Plus,
                modifier = Modifier.fillMaxWidth()
            )
            if (onBrowse != null) {
                PillButton(
                    text = s.explore.entryTitle,
                    onClick = onBrowse,
                    icon = Lucide.Compass,
                    tone = PillTone.Tonal,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
            if (isGuest) {
                PillButton(
                    text = s.mainLoginToTwitch,
                    onClick = onLogin,
                    icon = Lucide.User,
                    tone = PillTone.Ghost,
                    height = 48.dp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        if (onOpenChannels != null) {
            SwipeHint(
                onClick = onOpenChannels,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun SwipeHint(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val nudge by rememberInfiniteTransition(label = "swipeHint").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(NUDGE_MILLIS), RepeatMode.Reverse),
        label = "swipeNudge"
    )
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            LocalStrings.current.mainSwipeForChannels,
            style = MaterialTheme.typography.labelMedium,
            color = tint.copy(alpha = 0.8f)
        )
        Box(Modifier.graphicsLayer { translationX = nudge * 6.dp.toPx() }) {
            Icon(Lucide.ChevronRight, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

private const val NUDGE_MILLIS = 900
private const val HAZE_STRENGTH = 0.75f
