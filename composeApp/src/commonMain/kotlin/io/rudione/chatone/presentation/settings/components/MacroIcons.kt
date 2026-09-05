package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MacroIcons {

    const val PREFIX = "md:"

    val DEFAULT: String = PREFIX + "Bolt"

    val EMOJI: List<String> = listOf(
        "⚡", "🔥", "❄️", "🎯", "🚀", "🛡️", "⚔️", "🎲", "💫", "🌊", "🎮", "📢",
        "🔨", "⏱️", "🚫", "✅", "❌", "⭐", "💜", "🤖", "👑", "🧹", "🔔", "🔕",
        "📌", "💬", "🎬", "🏆", "🎉", "😴", "👀", "🧊", "🩹", "📈", "🕹️", "🧠"
    )

    val CATALOG: List<Pair<String, ImageVector>> = listOf(
        "Bolt" to Icons.Outlined.Bolt,
        "Whatshot" to Icons.Outlined.Whatshot,
        "AcUnit" to Icons.Outlined.AcUnit,
        "RocketLaunch" to Icons.Outlined.RocketLaunch,
        "Shield" to Icons.Outlined.Shield,
        "Gavel" to Icons.Outlined.Gavel,
        "Casino" to Icons.Outlined.Casino,
        "AutoAwesome" to Icons.Outlined.AutoAwesome,
        "Waves" to Icons.Outlined.Waves,
        "SportsEsports" to Icons.Outlined.SportsEsports,
        "Campaign" to Icons.Outlined.Campaign,
        "Chat" to Icons.Outlined.Chat,
        "Send" to Icons.Outlined.Send,
        "PushPin" to Icons.Outlined.PushPin,
        "Timer" to Icons.Outlined.Timer,
        "Schedule" to Icons.Outlined.Schedule,
        "Speed" to Icons.Outlined.Speed,
        "Star" to Icons.Outlined.Star,
        "Favorite" to Icons.Outlined.Favorite,
        "Lock" to Icons.Outlined.Lock,
        "LockOpen" to Icons.Outlined.LockOpen,
        "Block" to Icons.Outlined.Block,
        "Delete" to Icons.Outlined.Delete,
        "CleaningServices" to Icons.Outlined.CleaningServices,
        "VolumeOff" to Icons.Outlined.VolumeOff,
        "VisibilityOff" to Icons.Outlined.VisibilityOff,
        "Notifications" to Icons.Outlined.Notifications,
        "NotificationsOff" to Icons.Outlined.NotificationsOff,
        "PersonAdd" to Icons.Outlined.PersonAdd,
        "PersonRemove" to Icons.Outlined.PersonRemove,
        "Groups" to Icons.Outlined.Groups,
        "Verified" to Icons.Outlined.Verified,
        "EmojiEvents" to Icons.Outlined.EmojiEvents,
        "Celebration" to Icons.Outlined.Celebration,
        "Flag" to Icons.Outlined.Flag,
        "Warning" to Icons.Outlined.Warning,
        "PlayArrow" to Icons.Outlined.PlayArrow,
        "Settings" to Icons.Outlined.Settings
    )

    private val byName: Map<String, ImageVector> = CATALOG.toMap()

    fun token(name: String): String = PREFIX + name

    fun isVector(icon: String): Boolean = icon.startsWith(PREFIX)

    fun isEmojiChoice(icon: String): Boolean = !isVector(icon) && icon in EMOJI

    fun vectorOf(icon: String): ImageVector? =
        if (isVector(icon)) byName[icon.removePrefix(PREFIX)] else null
}

@Composable
fun MacroIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    fontSize: TextUnit = 16.sp,
    tint: Color = LocalContentColor.current
) {
    val vector = MacroIcons.vectorOf(icon)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (vector != null) {
            Icon(vector, contentDescription = null, modifier = Modifier.size(size), tint = tint)
        } else {
            Text(icon.ifEmpty { "⚡" }, fontSize = fontSize)
        }
    }
}
