package io.rudione.chatone.presentation.chat.mentions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val FLASH_IN_MS = 180
private const val FLASH_HOLD_MS = 650L
private const val FLASH_OUT_MS = 900
private const val FLASH_PEAK_ALPHA = 0.26f
private val FlashCorner = 8.dp

@Stable
class MessageJumpFlash internal constructor() {
    var messageId by mutableStateOf<String?>(null)
        private set
    private val level = Animatable(0f)

    internal val alpha: Float get() = level.value

    suspend fun flash(id: String) {
        messageId = id
        level.snapTo(0f)
        level.animateTo(1f, tween(FLASH_IN_MS, easing = LinearOutSlowInEasing))
        delay(FLASH_HOLD_MS)
        level.animateTo(0f, tween(FLASH_OUT_MS, easing = FastOutSlowInEasing))
        if (messageId == id) messageId = null
    }
}

@Composable
fun rememberMessageJumpFlash(): MessageJumpFlash = remember { MessageJumpFlash() }

fun Modifier.jumpFlash(flash: MessageJumpFlash, messageId: String, color: Color): Modifier =
    drawBehind {
        if (flash.messageId != messageId) return@drawBehind
        val alpha = flash.alpha * FLASH_PEAK_ALPHA
        if (alpha <= 0f) return@drawBehind
        val radius = FlashCorner.toPx()
        drawRoundRect(color.copy(alpha = alpha), cornerRadius = CornerRadius(radius, radius))
    }
