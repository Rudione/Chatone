package io.rudione.chatone.presentation.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ModerationTab { GENERAL, BUTTONS, SLIDER, MACROS }

@Stable
class SettingsPageState {
    var moderationTab by mutableStateOf(ModerationTab.GENERAL)

    var focusTarget: String? by mutableStateOf(null)
        private set

    var focusRequest by mutableIntStateOf(0)
        private set

    var highlighted: String? by mutableStateOf(null)
        private set

    fun focus(anchor: String, highlight: Boolean = true) {
        focusTarget = anchor
        highlighted = if (highlight) anchor else null
        focusRequest++
    }

    fun consume(anchor: String) {
        if (focusTarget == anchor) focusTarget = null
    }
}

val LocalSettingsPage = compositionLocalOf<SettingsPageState?> { null }

@Composable
fun Modifier.settingsAnchor(key: String): Modifier {
    val page = LocalSettingsPage.current ?: return this
    val requester = remember { BringIntoViewRequester() }
    val pulse = remember { Animatable(0f) }
    val accent = MaterialTheme.colorScheme.primary
    if (page.focusTarget == key) {
        LaunchedEffect(page.focusRequest) {
            delay(ANCHOR_SETTLE_MS)
            requester.bringIntoView()
            page.consume(key)
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(ANCHOR_FADE_MS))
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .drawBehind {
            val amount = if (page.highlighted == key) pulse.value else 0f
            if (amount > 0f) {
                val corner = CornerRadius(ANCHOR_RADIUS.toPx())
                drawRoundRect(accent.copy(alpha = 0.14f * amount), cornerRadius = corner)
                drawRoundRect(accent.copy(alpha = 0.6f * amount), cornerRadius = corner, style = Stroke(1.5.dp.toPx()))
            }
        }
}

private const val ANCHOR_SETTLE_MS = 120L
private const val ANCHOR_FADE_MS = 1_800
private val ANCHOR_RADIUS = 12.dp
