package io.rudione.chatone.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.util.handleHover


private data class EmojiCategory(val icon: String, val label: String, val emojis: List<String>)

private val EMOJI_CATEGORIES = listOf(
    EmojiCategory(
        "😀", "Smileys", listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "🫠", "😉", "😊", "😇",
            "🥰", "😍", "🤩", "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜", "🤪", "😝", "🤑",
            "🤗", "🤭", "🫢", "🫣", "🤫", "🤔", "🫡", "🤐", "🤨", "😐", "😑", "😶", "🫥", "😏",
            "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮",
            "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕", "🫤",
            "😟", "🙁", "☹️", "😮", "😯", "😲", "😳", "🥺", "🫣", "😦", "😧", "😨", "😰", "😥",
            "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬",
            "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖"
        )
    ),
    EmojiCategory(
        "👋", "People", listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌", "🤌", "🤏", "✌️", "🤞",
            "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "🫵", "👍", "👎", "✊",
            "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪",
            "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🫀", "🫁", "🧠", "🦷", "🦴", "👀", "👁️",
            "👅", "👄", "🫦", "💋", "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩", "🧓",
            "👴", "👵", "🙍", "🙎", "🙅", "🙆", "💁", "🙋", "🧏", "🙇", "🤦", "🤷", "💆", "💇",
            "🚶", "🧍", "🧎", "🏃", "💃", "🕺", "🕴️", "👫", "👬", "👭", "💑", "👨‍👩‍👦", "👨‍👩‍👧"
        )
    ),
    EmojiCategory(
        "🐶", "Animals", listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷",
            "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇",
            "🐺", "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌", "🐞", "🐜", "🪲", "🦟", "🦗",
            "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟",
            "🐬", "🐳", "🐋", "🦈", "🦭", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🦣", "🐘", "🦛",
            "🦏", "🐪", "🐫", "🦒", "🦘", "🦬", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙",
            "🐐", "🦌", "🐕", "🐩", "🦮", "🐕‍🦺", "🐈", "🐈‍⬛", "🪶", "🐓", "🦃", "🦤", "🦚", "🦜"
        )
    ),
    EmojiCategory(
        "🍎", "Food", listOf(
            "🍎", "🍊", "🍋", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅",
            "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🧄", "🧅", "🥔", "🍠", "🫘", "🌰", "🥜",
            "🍞", "🥐", "🥖", "🫓", "🥨", "🥯", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩",
            "🍗", "🍖", "🦴", "🌭", "🍔", "🍟", "🍕", "🫔", "🌮", "🌯", "🥙", "🧆", "🥚", "🍿",
            "🧂", "🥫", "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝", "🍠", "🦪", "🍣", "🍤", "🍙",
            "🥟", "🦑", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬", "🍭",
            "🍮", "🍯", "🍼", "🥛", "☕", "🫖", "🍵", "🧃", "🥤", "🧋", "🍶", "🍺", "🍻", "🥂"
        )
    ),
    EmojiCategory(
        "⚽", "Activities", listOf(
            "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒",
            "🏑", "🥍", "🏏", "🪃", "🥅", "⛳", "🪁", "🎣", "🤿", "🎽", "🎿", "🛷", "🥌", "🎯",
            "🪃", "🎱", "🔮", "🪄", "🎮", "🕹️", "🎲", "♟️", "🧩", "🧸", "🪅", "🎭", "🎨", "🖼️",
            "🎪", "🤹", "🎬", "🎤", "🎧", "🎼", "🎵", "🎶", "🎹", "🥁", "🪘", "🎷", "🎺", "🪗",
            "🎸", "🪕", "🎻", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🏵️", "🎗️", "🎫", "🎟️", "🎪"
        )
    ),
    EmojiCategory(
        "🚗", "Travel", listOf(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛", "🚜",
            "🏍️", "🛵", "🚲", "🛴", "🛹", "🛼", "🚏", "🛣️", "🛤️", "⛽", "🚧", "🚦", "🚥", "🚢",
            "✈️", "🛩️", "🛫", "🛬", "🪂", "💺", "🚁", "🚟", "🚠", "🚡", "🛰️", "🚀", "🛸", "🪐",
            "⭐", "🌟", "💫", "✨", "🌈", "☀️", "🌤️", "⛅", "🌥️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️",
            "🌪️", "🌫️", "🌬️", "🌀", "🌊", "🌁", "🏔️", "⛰️", "🌋", "🗻", "🏕️", "🏖️", "🏜️", "🏝️"
        )
    ),
    EmojiCategory(
        "💡", "Objects", listOf(
            "⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🖲️", "💾", "💿", "📀", "📷", "📸", "📹",
            "🎥", "📽️", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🧭", "⏱️", "⏲️", "⏰", "🕰️",
            "⌛", "⏳", "📡", "🔋", "🔌", "💡", "🔦", "🕯️", "🧱", "💰", "💴", "💵", "💶", "💷",
            "💸", "💳", "🧾", "💹", "📈", "📉", "📊", "📋", "📌", "📍", "📎", "🖇️", "📏", "📐",
            "✂️", "🗃️", "🗄️", "🗑️", "🔒", "🔓", "🔏", "🔐", "🔑", "🗝️", "🔨", "🪓", "⛏️", "⚒️",
            "🛠️", "🗡️", "⚔️", "🛡️", "🪚", "🔧", "🪛", "🔩", "⚙️", "🗜️", "⚖️", "🦯", "🔗", "⛓️",
            "🪝", "🧲", "🪜", "⚗️", "🧪", "🧫", "🧬", "🔭", "🔬", "🩺", "💊", "💉", "🩹", "🩼"
        )
    ),
    EmojiCategory(
        "❤️", "Symbols", listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "💕", "💞",
            "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️",
            "🛐", "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓",
            "🆔", "⚛️", "🉑", "☢️", "☣️", "📴", "📳", "🈶", "🈚", "🈸", "🈺", "🈷️", "✴️", "🆚",
            "💮", "🉐", "㊙️", "㊗️", "🈴", "🈵", "🈹", "🈲", "🅰️", "🅱️", "🆎", "🆑", "🅾️", "🆘",
            "❌", "⭕", "🛑", "⛔", "📛", "🚫", "💯", "💢", "♨️", "🚷", "🚯", "🚳", "🚱", "🔞",
            "📵", "🔕", "🎵", "🎶", "💲", "💱", "™️", "©️", "®️", "〰️", "➰", "➿", "🔚", "🔙"
        )
    ),
    EmojiCategory(
        "🎌", "Flags", listOf(
            "🏳️", "🏴", "🏁", "🚩", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️", "🇦🇫", "🇦🇱", "🇩🇿", "🇦🇩", "🇦🇴", "🇦🇷", "🇦🇲",
            "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪", "🇧🇿", "🇧🇯", "🇧🇹", "🇧🇴", "🇧🇦",
            "🇧🇼", "🇧🇷", "🇧🇳", "🇧🇬", "🇧🇫", "🇧🇮", "🇨🇻", "🇰🇭", "🇨🇲", "🇨🇦", "🇨🇫", "🇹🇩", "🇨🇱", "🇨🇳",
            "🇨🇴", "🇨🇷", "🇭🇷", "🇨🇺", "🇨🇾", "🇨🇿", "🇩🇰", "🇩🇯", "🇩🇴", "🇪🇨", "🇪🇬", "🇸🇻", "🇬🇶", "🇪🇷",
            "🇪🇪", "🇸🇿", "🇪🇹", "🇫🇯", "🇫🇮", "🇫🇷", "🇬🇦", "🇬🇲", "🇬🇪", "🇩🇪", "🇬🇭", "🇬🇷", "🇬🇹", "🇬🇳",
            "🇬🇼", "🇬🇾", "🇭🇹", "🇭🇳", "🇭🇺", "🇮🇸", "🇮🇳", "🇮🇩", "🇮🇷", "🇮🇶", "🇮🇪", "🇮🇱", "🇮🇹", "🇯🇲",
            "🇯🇵", "🇯🇴", "🇰🇿", "🇰🇪", "🇰🇷", "🇰🇼", "🇰🇬", "🇱🇦", "🇱🇻", "🇱🇧", "🇱🇸", "🇱🇷", "🇱🇾", "🇱🇮"
        )
    )
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotePickerSheet(
    channelEmotes: ChannelEmotes,
    onEmoteSelected: (GenericEmote) -> Unit,
    onEmojiSelected: (String) -> Unit = {},
    onDismiss: () -> Unit,
    closeOnMouseLeave: Boolean = false
) {

    val settings = remember { Settings() }
    var favoriteIds by remember {
        mutableStateOf(
            settings.getStringOrNull("favorite_emotes")
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        )
    }

    fun toggleFavorite(emote: GenericEmote) {
        val key = "${emote.provider}_${emote.id}"
        favoriteIds = if (key in favoriteIds) favoriteIds - key else favoriteIds + key
        settings.putString("favorite_emotes", favoriteIds.joinToString(","))
    }

    val favoriteEmotes = remember(favoriteIds, channelEmotes) {
        channelEmotes.all.filter { "${it.provider}_${it.id}" in favoriteIds }
    }

    var searchQuery by remember { mutableStateOf("") }

    var mainTab by remember { mutableIntStateOf(0) }
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible.value = true
    }


    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible.value) 0.35f else 0f,
        animationSpec = tween(220),
        label = "backdropAlpha"
    )

    val backdropInteractionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha))
            .clickable(
                indication = null,
                interactionSource = backdropInteractionSource
            ) { onDismiss() }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible.value,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.3f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .then(
                        if (closeOnMouseLeave)
                            Modifier.handleHover(onEnter = {}, onExit = { onDismiss() })
                        else
                            Modifier
                    )
                    .heightIn(max = 480.dp)

                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume */ }
            ) {

                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )

                TabRow(
                    selectedTabIndex = mainTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = mainTab == 0,
                        onClick = { mainTab = 0; searchQuery = "" },
                        text = {
                            Text(
                                "Emotes",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (mainTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = mainTab == 1,
                        onClick = { mainTab = 1; searchQuery = "" },
                        text = {
                            Text(
                                "Emoji",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (mainTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                when (mainTab) {
                    0 -> EmoteTab(
                        channelEmotes = channelEmotes,
                        favoriteEmotes = favoriteEmotes,
                        favoriteIds = favoriteIds,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onEmoteSelected = onEmoteSelected,
                        onToggleFavorite = { toggleFavorite(it) }
                    )

                    1 -> EmojiTab(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onEmojiSelected = { emoji ->
                            onEmojiSelected(emoji)
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@Composable
private fun EmoteTab(
    channelEmotes: ChannelEmotes,
    favoriteEmotes: List<GenericEmote>,
    favoriteIds: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmoteSelected: (GenericEmote) -> Unit,
    onToggleFavorite: (GenericEmote) -> Unit
) {
    data class ProviderTab(
        val label: String,
        val provider: EmoteProvider?,
        val isFav: Boolean = false,
        val count: Int
    )

    val tabs = remember(channelEmotes, favoriteEmotes) {
        buildList {
            if (favoriteEmotes.isNotEmpty()) {
                add(
                    ProviderTab(
                        "★ (${favoriteEmotes.size})",
                        null,
                        isFav = true,
                        count = favoriteEmotes.size
                    )
                )
            }
            val all = channelEmotes.all.size
            val tw = channelEmotes.byProvider[EmoteProvider.TWITCH]?.size ?: 0
            val s7 = channelEmotes.byProvider[EmoteProvider.SEVEN_TV]?.size ?: 0
            val bt = channelEmotes.byProvider[EmoteProvider.BTTV]?.size ?: 0
            val fz = channelEmotes.byProvider[EmoteProvider.FFZ]?.size ?: 0
            add(ProviderTab("All ($all)", null, count = all))
            if (tw > 0) add(ProviderTab("Twitch", EmoteProvider.TWITCH, count = tw))
            if (s7 > 0) add(ProviderTab("7TV", EmoteProvider.SEVEN_TV, count = s7))
            if (bt > 0) add(ProviderTab("BTTV", EmoteProvider.BTTV, count = bt))
            if (fz > 0) add(ProviderTab("FFZ", EmoteProvider.FFZ, count = fz))
        }
    }

    var selectedProviderTab by remember { mutableIntStateOf(0) }


    val safeTab = selectedProviderTab.coerceAtMost(tabs.lastIndex)

    val filteredEmotes = remember(channelEmotes, favoriteEmotes, searchQuery, safeTab, tabs) {
        val tab = tabs.getOrNull(safeTab)
        var list: List<GenericEmote> = when {
            tab?.isFav == true -> favoriteEmotes
            tab?.provider != null -> channelEmotes.byProvider[tab.provider] ?: emptyList()
            else -> channelEmotes.all
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter { it.code.lowercase().contains(q) }
        }
        list
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Search emotes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )


        ScrollableTabRow(
            selectedTabIndex = safeTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safeTab == index,
                    onClick = { selectedProviderTab = index },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (safeTab == index) FontWeight.SemiBold else FontWeight.Normal,

                            color = if (tab.isFav) Color(0xFFFFD700)
                            else Color.Unspecified
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (filteredEmotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No emotes match \"$searchQuery\""
                        else if (tabs.getOrNull(safeTab)?.isFav == true) "No favorites yet\nLong-press an emote to add"
                        else "No emotes loaded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { onSearchQueryChange("") }) { Text("Clear search") }
                    }
                }
            }
        } else {
            val favKeySet = favoriteIds
            val gridState = rememberLazyGridState()
            LaunchedEffect(safeTab, searchQuery) { gridState.scrollToItem(0) }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp, max = 280.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    filteredEmotes,
                    key = { "${it.provider}_${it.id}" },
                    contentType = { it.provider }
                ) { emote ->
                    val emoteKey =
                        remember(emote.provider, emote.id) { "${emote.provider}_${emote.id}" }
                    val isFav = emoteKey in favKeySet
                    EmoteGridItem(
                        emoteKey = emoteKey,
                        emote = emote,
                        isFavorite = isFav,
                        onClick = { onEmoteSelected(emote) },
                        onToggleFavorite = { onToggleFavorite(emote) }
                    )
                }
            }
        }
    }
}


@Composable
private fun EmojiTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val displayEmojis = remember(searchQuery, selectedCategoryIndex) {
        if (searchQuery.isNotBlank()) {
            EMOJI_CATEGORIES.flatMap { it.emojis }.filter { it.contains(searchQuery) }
        } else {
            EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.emojis ?: emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Search emoji...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        if (searchQuery.isBlank()) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp,
                divider = {}
            ) {
                EMOJI_CATEGORIES.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedCategoryIndex == index)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.icon,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Text(
                text = EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.label ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (displayEmojis.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No emoji found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 100.dp, max = 280.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(displayEmojis) { emoji ->
                    EmojiGridItem(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmoteGridItem(
    emoteKey: String,
    emote: GenericEmote,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember(emoteKey) { MutableInteractionSource() }
    val imageUrl = remember(emoteKey) { emote.url1x.ifEmpty { emote.url2x } }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onToggleFavorite
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AnimatedEmoteImage(
                url = imageUrl,
                contentDescription = emote.code,
                modifier = Modifier.size(36.dp)
            )

            if (isFavorite) {
                Text(
                    "★",
                    fontSize = 10.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        Text(
            text = emote.code,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmojiGridItem(
    emoji: String,
    onClick: () -> Unit
) {
    val interactionSource = remember(emoji) { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}