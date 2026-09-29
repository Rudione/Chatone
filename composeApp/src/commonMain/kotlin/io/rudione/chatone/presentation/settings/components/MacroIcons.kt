package io.rudione.chatone.presentation.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.outlined.AcUnit
import io.rudione.chatone.icons.material.outlined.AutoAwesome
import io.rudione.chatone.icons.material.outlined.Block
import io.rudione.chatone.icons.material.outlined.Bolt
import io.rudione.chatone.icons.material.outlined.Campaign
import io.rudione.chatone.icons.material.outlined.Casino
import io.rudione.chatone.icons.material.outlined.Celebration
import io.rudione.chatone.icons.material.outlined.Chat
import io.rudione.chatone.icons.material.outlined.CleaningServices
import io.rudione.chatone.icons.material.outlined.Delete
import io.rudione.chatone.icons.material.outlined.EmojiEvents
import io.rudione.chatone.icons.material.outlined.Favorite
import io.rudione.chatone.icons.material.outlined.Flag
import io.rudione.chatone.icons.material.outlined.Gavel
import io.rudione.chatone.icons.material.outlined.Groups
import io.rudione.chatone.icons.material.outlined.Lock
import io.rudione.chatone.icons.material.outlined.LockOpen
import io.rudione.chatone.icons.material.outlined.Notifications
import io.rudione.chatone.icons.material.outlined.NotificationsOff
import io.rudione.chatone.icons.material.outlined.PersonAdd
import io.rudione.chatone.icons.material.outlined.PersonRemove
import io.rudione.chatone.icons.material.outlined.PlayArrow
import io.rudione.chatone.icons.material.outlined.PushPin
import io.rudione.chatone.icons.material.outlined.RocketLaunch
import io.rudione.chatone.icons.material.outlined.Schedule
import io.rudione.chatone.icons.material.outlined.Send
import io.rudione.chatone.icons.material.outlined.Settings
import io.rudione.chatone.icons.material.outlined.Shield
import io.rudione.chatone.icons.material.outlined.Speed
import io.rudione.chatone.icons.material.outlined.SportsEsports
import io.rudione.chatone.icons.material.outlined.Star
import io.rudione.chatone.icons.material.outlined.Timer
import io.rudione.chatone.icons.material.outlined.Verified
import io.rudione.chatone.icons.material.outlined.VisibilityOff
import io.rudione.chatone.icons.material.outlined.VolumeOff
import io.rudione.chatone.icons.material.outlined.Warning
import io.rudione.chatone.icons.material.outlined.Waves
import io.rudione.chatone.icons.material.outlined.Whatshot
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
            Text(
                icon.ifEmpty { "⚡" },
                style = TextStyle(
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    textAlign = TextAlign.Center,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.wrapContentSize(unbounded = true)
            )
        }
    }
}
