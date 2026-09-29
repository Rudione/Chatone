package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.domain.model.DisplayMessage

private const val MAX_TRACKED_CHATTERS = 20_000

@Stable
fun interface ChatterColors {
    operator fun get(login: String): Color?

    companion object {
        val None = ChatterColors { null }
    }
}

class ChatterColorIndex : ChatterColors {

    private val byLogin = HashMap<String, Color>()
    private var lastAbsorbedId: String? = null

    override fun get(login: String): Color? = byLogin[login]

    fun absorb(messages: List<DisplayMessage>) {
        if (messages.isEmpty()) return
        if (byLogin.size > MAX_TRACKED_CHATTERS) {
            byLogin.clear()
            lastAbsorbedId = null
        }
        val resumeAt = lastAbsorbedId
            ?.let { id -> messages.indexOfLast { it.id == id } + 1 }
            ?: 0
        for (index in resumeAt until messages.size) {
            val message = messages[index] as? DisplayMessage.PrivMsg ?: continue
            val login = message.username.lowercase()
            if (login.isEmpty()) continue
            parseTwitchHexColor(message.color)?.let { byLogin[login] = it }
        }
        lastAbsorbedId = messages.last().id
    }
}

private fun parseTwitchHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val value = hex.removePrefix("#").toLongOrNull(16) ?: return null
    return Color(
        red = ((value shr 16) and 0xFF) / 255f,
        green = ((value shr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f
    )
}
