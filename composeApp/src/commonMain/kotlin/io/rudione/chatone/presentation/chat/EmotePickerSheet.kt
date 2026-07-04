package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import io.rudione.chatone.presentation.chat.components.EmoteGridItemFlyweight
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.rudione.chatone.presentation.chat.models.EmoteUiData
import io.rudione.chatone.util.emote.EmoteSearchIndex

private const val PAGE_SIZE = 100
private const val PRELOAD_THRESHOLD = 20

private data class EmojiCategory(val icon: String, val label: String, val emojis: List<String>)

private val EMOJI_CATEGORIES = listOf(
    EmojiCategory("😀","Smileys", listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","🫠","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🫢","🫣","🤫","🤔","🫡","🤐","🤨","😐","😑","😶","🫥","😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🤧","🥵","🥶","🥴","😵","🤯","🤠","🥳","🥸","😎","🤓","🧐","😕","🫤","😟","🙁","☹️","😮","😯","😲","😳","🥺","🫣","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖")),
    EmojiCategory("👋","People", listOf("👋","🤚","🖐️","✋","🖖","🫱","🫲","🫳","🫴","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","🫵","👍","👎","✊","👊","🤛","🤜","👏","🙌","🫶","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦾","🦿","🦵","🦶","👂","🦻","👃","🫀","🫁","🧠","🦷","🦴","👀","👁️","👅","👄","🫦","💋","👶","🧒","👦","👧","🧑","👱","👨","🧔","👩","🧓","👴","👵","🙍","🙎","🙅","🙆","💁","🙋","🧏","🙇","🤦","🤷","💆","💇","🚶","🧍","🧎","🏃","💃","🕺","👫","👬","👭","💑","👨‍👩‍👦","👨‍👩‍👧")),
    EmojiCategory("🐶","Animals", listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐒","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🪱","🐛","🦋","🐌","🐞","🐜","🦟","🦗","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈","🦭","🐊","🐅","🐆","🦓","🦍","🦧","🦣","🐘","🦛","🦏","🐪","🐫","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖","🐏","🐑","🦙","🐐","🦌","🐕","🐩","🦮","🐈","🐈‍⬛","🪶","🐓","🦃","🦤","🦚","🦜")),
    EmojiCategory("🍎","Food", listOf("🍎","🍊","🍋","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌶️","🫑","🧄","🧅","🥔","🍠","🫘","🌰","🥜","🍞","🥐","🥖","🫓","🥨","🥯","🧀","🥚","🍳","🧈","🥞","🧇","🥓","🥩","🍗","🍖","🦴","🌭","🍔","🍟","🍕","🫔","🌮","🌯","🥙","🧆","🍿","🧂","🍱","🍘","🍙","🍚","🍛","🍜","🍝","🦪","🍣","🍤","🥟","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯","🍼","🥛","☕","🫖","🍵","🧃","🥤","🧋","🍶","🍺","🍻","🥂")),
    EmojiCategory("⚽","Activities", listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍","🏏","🪃","🥅","⛳","🪁","🎣","🤿","🎽","🎿","🛷","🥌","🎯","🎱","🔮","🪄","🎮","🕹️","🎲","♟️","🧩","🧸","🪅","🎭","🎨","🖼️","🎪","🤹","🎬","🎤","🎧","🎼","🎵","🎶","🎹","🥁","🪘","🎷","🎺","🪗","🎸","🪕","🎻","🏆","🥇","🥈","🥉","🏅","🎖️","🏵️","🎗️","🎫","🎟️")),
    EmojiCategory("🚗","Travel", listOf("🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜","🏍️","🛵","🚲","🛴","🛹","🛼","🚢","✈️","🛩️","🛫","🛬","🪂","💺","🚁","🚟","🚠","🚡","🛰️","🚀","🛸","🪐","⭐","🌟","💫","✨","🌈","☀️","🌤️","⛅","🌥️","🌦️","🌧️","⛈️","🌩️","🌨️","🌪️","🌫️","🌬️","🌀","🌊","🌁","🏔️","⛰️","🌋","🗻","🏕️","🏖️","🏜️","🏝️")),
    EmojiCategory("💡","Objects", listOf("⌚","📱","💻","⌨️","🖥️","🖨️","🖱️","💾","💿","📀","📷","📸","📹","🎥","📽️","🎞️","📞","☎️","📟","📠","📺","📻","🧭","⏱️","⏲️","⏰","🕰️","⌛","⏳","📡","🔋","🔌","💡","🔦","🕯️","🧱","💰","💴","💵","💶","💷","💸","💳","🧾","📈","📉","📊","📋","📌","📍","📎","🖇️","📏","📐","✂️","🗃️","🗄️","🗑️","🔒","🔓","🔏","🔐","🔑","🗝️","🔨","🪓","⛏️","⚒️","🛠️","🗡️","⚔️","🛡️","🪚","🔧","🪛","🔩","⚙️","🗜️","⚖️","🦯","🔗","⛓️","🪝","🧲","🪜","⚗️","🧪","🧫","🧬","🔭","🔬","🩺","💊","💉","🩹","🩼")),
    EmojiCategory("❤️","Symbols", listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","💕","💞","💓","💗","💖","💘","💝","💟","☮️","✝️","☪️","🕉️","☸️","✡️","🔯","🕎","☯️","🛐","⛎","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓","🆔","⚛️","☢️","☣️","📴","📳","✴️","🆚","💮","🉐","㊙️","㊗️","🈴","🈵","🈹","🈲","🅰️","🅱️","🆎","🆑","🅾️","🆘","❌","⭕","🛑","⛔","📛","🚫","💯","💢","♨️","™️","©️","®️","〰️","➰","➿","🔚","🔙")),
    EmojiCategory("🎌","Flags", listOf("🏳️","🏴","🏁","🚩","🏳️‍🌈","🏳️‍⚧️","🏴‍☠️","🇦🇫","🇦🇱","🇩🇿","🇦🇩","🇦🇴","🇦🇷","🇦🇲","🇦🇺","🇦🇹","🇦🇿","🇧🇸","🇧🇭","🇧🇩","🇧🇧","🇧🇾","🇧🇪","🇧🇿","🇧🇯","🇧🇹","🇧🇴","🇧🇦","🇧🇼","🇧🇷","🇧🇳","🇧🇬","🇧🇫","🇧🇮","🇨🇻","🇰🇭","🇨🇲","🇨🇦","🇨🇫","🇹🇩","🇨🇱","🇨🇳","🇨🇴","🇨🇷","🇭🇷","🇨🇺","🇨🇾","🇨🇿","🇩🇰","🇩🇯","🇩🇴","🇪🇨","🇪🇬","🇸🇻","🇬🇶","🇪🇷","🇪🇪","🇸🇿","🇪🇹","🇫🇯","🇫🇮","🇫🇷","🇬🇦","🇬🇲","🇬🇪","🇩🇪","🇬🇭","🇬🇷","🇬🇹","🇬🇳","🇬🇼","🇬🇾","🇭🇹","🇭🇳","🇭🇺","🇮🇸","🇮🇳","🇮🇩","🇮🇷","🇮🇶","🇮🇪","🇮🇱","🇮🇹","🇯🇲","🇯🇵","🇯🇴","🇰🇿","🇰🇪","🇰🇷","🇰🇼","🇰🇬","🇱🇦","🇱🇻","🇱🇧","🇱🇸","🇱🇷","🇱🇾","🇱🇮"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotePickerSheet(
    channelEmotes: ChannelEmotes,
    personalEmotes: List<GenericEmote> = emptyList(),
    onEmoteSelected: (GenericEmote) -> Unit,
    onEmojiSelected: (String) -> Unit = {},
    onDismiss: () -> Unit,
    closeOnMouseLeave: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    val settings = remember { Settings() }
    var favoriteIds by remember {
        mutableStateOf(
            settings.getStringOrNull("favorite_emotes")
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        )
    }

    var favoriteEmojis by remember {
        mutableStateOf(
            settings.getStringOrNull("favorite_emojis")
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        )
    }

    fun toggleFavorite(emote: GenericEmote) {
        val key = "${emote.provider}_${emote.id}"
        favoriteIds = if (key in favoriteIds) favoriteIds - key else favoriteIds + key
        settings.putString("favorite_emotes", favoriteIds.joinToString(","))
    }

    fun toggleFavoriteEmoji(emoji: String) {
        favoriteEmojis = if (emoji in favoriteEmojis) favoriteEmojis - emoji else favoriteEmojis + emoji
        settings.putString("favorite_emojis", favoriteEmojis.joinToString(","))
    }

    val allEmotes = remember(channelEmotes, personalEmotes) {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<GenericEmote>()
        for (e in channelEmotes.all) if (seen.add(e.listKey)) result.add(e)
        for (e in personalEmotes) if (seen.add(e.listKey)) result.add(e)
        result
    }

    val favoriteEmotes = remember(favoriteIds, allEmotes) {
        allEmotes.filter { "${it.provider}_${it.id}" in favoriteIds }
    }

    val backdropSource = remember { MutableInteractionSource() }
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(indication = null, interactionSource = backdropSource) { onDismiss() }
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(
                    Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    ))
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .then(
                    if (closeOnMouseLeave) Modifier.pointerInput(Unit) {
                        delay(500)
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                when (event.type) {
                                    PointerEventType.Enter -> { dismissJob?.cancel(); dismissJob = null }
                                    PointerEventType.Exit -> { dismissJob = scope.launch { delay(250); onDismiss() } }
                                }
                            }
                        }
                    } else Modifier
                )
                .heightIn(max = 520.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            EmoteTab(
                channelEmotes = channelEmotes,
                personalEmotes = personalEmotes,
                allEmotes = allEmotes,
                favoriteEmotes = favoriteEmotes,
                favoriteIds = favoriteIds,
                favoriteEmojis = favoriteEmojis,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onEmoteSelected = onEmoteSelected,
                onToggleFavorite = { toggleFavorite(it) },
                onFavoriteAll = { emotes ->
                    val newKeys = emotes.map { "${it.provider}_${it.id}" }.toSet()
                    favoriteIds = favoriteIds + newKeys
                    settings.putString("favorite_emotes", favoriteIds.joinToString(","))
                },
                onEmojiSelected = onEmojiSelected,
                onToggleFavoriteEmoji = { toggleFavoriteEmoji(it) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private sealed class PickerTab {
    object Favorites : PickerTab()
    object All : PickerTab()
    object Twitch : PickerTab()
    object TwitchSubscribed : PickerTab()
    object SevenTv : PickerTab()
    object Bttv : PickerTab()
    object Ffz : PickerTab()
    object Emoji : PickerTab()
}

private data class EmoteSection(val title: String?, val emotes: List<GenericEmote>)

private fun providerName(p: EmoteProvider): String = when (p) {
    EmoteProvider.TWITCH -> "Twitch"
    EmoteProvider.SEVEN_TV -> "7TV"
    EmoteProvider.BTTV -> "BTTV"
    EmoteProvider.FFZ -> "FFZ"
}

@Composable
private fun CompactEmoteSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor =
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
            .border(1.dp, borderColor, RoundedCornerShape(17.dp)),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)
            ) {
                Icon(
                    Icons.Default.Search, null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            LocalStrings.current.emoteSearchPlaceholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Default.Clear, "Clear",
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .clickable { onValueChange("") },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    )
}

@Composable
private fun EmoteSectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
internal fun EmoteTab(
    channelEmotes: ChannelEmotes,
    personalEmotes: List<GenericEmote> = emptyList(),
    allEmotes: List<GenericEmote>,
    favoriteEmotes: List<GenericEmote>,
    favoriteIds: Set<String>,
    favoriteEmojis: Set<String> = emptySet(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmoteSelected: (GenericEmote) -> Unit,
    onToggleFavorite: (GenericEmote) -> Unit,
    onFavoriteAll: (List<GenericEmote>) -> Unit = {},
    onEmojiSelected: (String) -> Unit = {},
    onToggleFavoriteEmoji: (String) -> Unit = {}
) {
    data class TabEntry(val label: String, val tab: PickerTab, val count: Int)

    val twitchChannel = channelEmotes.twitchEmotes
    val twitchGlobal: List<GenericEmote> = channelEmotes.twitchGlobal
    val twitchSubscribed = personalEmotes.filter { it.provider == EmoteProvider.TWITCH }
    val hasTwitch = twitchChannel.isNotEmpty() || twitchGlobal.isNotEmpty()
    val hasSub = twitchSubscribed.isNotEmpty()
    val sevenTv = channelEmotes.byProvider[EmoteProvider.SEVEN_TV] ?: emptyList()
    val bttv = channelEmotes.byProvider[EmoteProvider.BTTV] ?: emptyList()
    val ffz = channelEmotes.byProvider[EmoteProvider.FFZ] ?: emptyList()

    val tabs = remember(allEmotes.size, favoriteEmotes.size, hasTwitch, hasSub, sevenTv.size, bttv.size, ffz.size) {
        buildList<TabEntry> {
            if (favoriteEmotes.isNotEmpty()) add(TabEntry("★ ${favoriteEmotes.size}", PickerTab.Favorites, favoriteEmotes.size))
            add(TabEntry("All (${allEmotes.size})", PickerTab.All, allEmotes.size))
            if (hasTwitch) add(TabEntry("Twitch", PickerTab.Twitch, twitchChannel.size + twitchGlobal.size))
            if (hasSub) add(TabEntry("Sub ★", PickerTab.TwitchSubscribed, twitchSubscribed.size))
            if (sevenTv.isNotEmpty()) add(TabEntry("7TV", PickerTab.SevenTv, sevenTv.size))
            if (bttv.isNotEmpty()) add(TabEntry("BTTV", PickerTab.Bttv, bttv.size))
            if (ffz.isNotEmpty()) add(TabEntry("FFZ", PickerTab.Ffz, ffz.size))
            add(TabEntry("😀 Emoji", PickerTab.Emoji, 0))
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val safeTab = selectedTabIndex.coerceAtMost(tabs.lastIndex.coerceAtLeast(0))
    val currentTab = tabs.getOrNull(safeTab)
    val isEmojiTab = currentTab?.tab == PickerTab.Emoji

    val baseSections: List<EmoteSection> = remember(currentTab, allEmotes, favoriteEmotes, twitchChannel, twitchGlobal, twitchSubscribed, sevenTv, bttv, ffz) {
        val channelName = twitchChannel.firstOrNull()?.authorName?.takeIf { it.isNotBlank() }
        when (currentTab?.tab) {
            PickerTab.Favorites -> listOf(EmoteSection(null, favoriteEmotes))
            PickerTab.All -> buildList {
                if (twitchChannel.isNotEmpty()) add(EmoteSection(channelName?.let { "Twitch · $it" } ?: "Twitch", twitchChannel))
                if (twitchGlobal.isNotEmpty()) add(EmoteSection("Twitch Global", twitchGlobal))
                if (sevenTv.isNotEmpty()) add(EmoteSection("7TV", sevenTv))
                if (bttv.isNotEmpty()) add(EmoteSection("BTTV", bttv))
                if (ffz.isNotEmpty()) add(EmoteSection("FFZ", ffz))
                if (twitchSubscribed.isNotEmpty()) add(EmoteSection("Subscriptions", twitchSubscribed))
            }
            PickerTab.Twitch -> buildList {
                if (twitchChannel.isNotEmpty()) add(EmoteSection(channelName?.let { "Channel · $it" } ?: "Channel", twitchChannel))
                if (twitchGlobal.isNotEmpty()) add(EmoteSection("Global", twitchGlobal))
            }
            PickerTab.TwitchSubscribed -> twitchSubscribed
                .groupBy { it.authorName.ifBlank { "Other" } }
                .toList()
                .sortedBy { it.first.lowercase() }
                .map { (chan, list) -> EmoteSection(chan, list) }
            PickerTab.SevenTv -> listOf(EmoteSection(null, sevenTv))
            PickerTab.Bttv -> listOf(EmoteSection(null, bttv))
            PickerTab.Ffz -> listOf(EmoteSection(null, ffz))
            else -> emptyList()
        }
    }

    val flatSource = remember(baseSections) { baseSections.flatMap { it.emotes } }
    val currentEmoteSource = flatSource

    val searchIndex by produceState<EmoteSearchIndex?>(initialValue = null, flatSource.size, currentTab) {
        if (isEmojiTab) { value = null; return@produceState }
        value = withContext(Dispatchers.Default) { EmoteSearchIndex().also { it.build(flatSource) } }
    }
    val allIndex by produceState<EmoteSearchIndex?>(initialValue = null, allEmotes.size) {
        value = withContext(Dispatchers.Default) { EmoteSearchIndex().also { it.build(allEmotes) } }
    }

    val displaySections: List<EmoteSection> = remember(baseSections, searchQuery, searchIndex, allIndex) {
        if (isEmojiTab) emptyList()
        else if (searchQuery.isBlank() || searchIndex == null) baseSections
        else {
            val matched = searchIndex!!.search(searchQuery, limit = 500).map { it.listKey }.toSet()
            val filtered = baseSections
                .map { sec -> sec.copy(emotes = sec.emotes.filter { it.listKey in matched }) }
                .filter { it.emotes.isNotEmpty() }
            if (filtered.isNotEmpty()) filtered
            else (allIndex?.search(searchQuery, limit = 500) ?: emptyList())
                .groupBy { it.provider }
                .toList()
                .map { (prov, list) -> EmoteSection("${providerName(prov)} · search", list) }
        }
    }

    val isEmpty = displaySections.all { it.emotes.isEmpty() }
    val gridState = rememberLazyGridState()
    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { isScrolling = it }
    }
    LaunchedEffect(safeTab, searchQuery) {
        if (gridState.canScrollForward || gridState.canScrollBackward) gridState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Emoji can only be "searched" by pasting the emoji itself, which is useless —
        // hide the bar there instead of pretending it works.
        if (!isEmojiTab) {
            CompactEmoteSearchBar(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ScrollableTabRow(
                selectedTabIndex = safeTab,
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = safeTab == index,
                        onClick = { selectedTabIndex = index; onSearchQueryChange("") },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (safeTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = when (tab.tab) {
                                is PickerTab.Favorites -> Color(0xFFFFD700)
                                is PickerTab.TwitchSubscribed -> Color(0xFF9147FF)
                                else -> Color.Unspecified
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
            if (currentTab?.tab == PickerTab.Favorites && favoriteEmotes.isNotEmpty()) {
                IconButton(
                    onClick = { onFavoriteAll(currentEmoteSource) },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Favorite all", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (isEmojiTab) {
            EmojiTab(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onEmojiSelected = onEmojiSelected,
                favoriteEmojis = favoriteEmojis,
                onToggleFavoriteEmoji = onToggleFavoriteEmoji
            )
        } else if (currentTab?.tab == PickerTab.Favorites && favoriteEmojis.isNotEmpty() && isEmpty) {
            // Show favorite emojis when there are no favorite emotes but there are fav emoji
            EmojiTab(
                searchQuery = "",
                onSearchQueryChange = {},
                onEmojiSelected = onEmojiSelected,
                favoriteEmojis = favoriteEmojis,
                showOnlyFavorites = true,
                onToggleFavoriteEmoji = onToggleFavoriteEmoji
            )
        } else if (isEmpty) {
            EmptyEmoteState(
                searchQuery = searchQuery,
                isFavoritesTab = currentTab?.tab == PickerTab.Favorites,
                onClearSearch = { onSearchQueryChange("") }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                state = gridState,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 120.dp, max = 280.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                displaySections.forEach { section ->
                    if (section.title != null && section.emotes.isNotEmpty()) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "header_${section.title}",
                            contentType = "header"
                        ) {
                            EmoteSectionHeader(section.title)
                        }
                    }
                    items(
                        items = section.emotes,
                        key = { it.listKey },
                        contentType = { it.provider }
                    ) { emote ->
                        val uiData = remember(emote.listKey, favoriteIds) {
                            EmoteUiData.fromEmote(emote, "${emote.provider}_${emote.id}" in favoriteIds)
                        }
                        EmoteGridItemFlyweight(
                            uiData = uiData,
                            isScrolling = isScrolling,
                            onClick = { onEmoteSelected(emote) },
                            onToggleFavorite = { onToggleFavorite(emote) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyEmoteState(
    searchQuery: String,
    isFavoritesTab: Boolean,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val sl = LocalStrings.current
            Text(
                text = when {
                    searchQuery.isNotBlank() -> sl.emoteNoMatch.replace("{0}", searchQuery)
                    isFavoritesTab -> sl.emoteNoFavorites
                    else -> sl.emoteNoLoaded
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            if (searchQuery.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onClearSearch) {
                    Text(LocalStrings.current.emoteClearSearch, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LoadMoreIndicator(isLoading: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmojiTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmojiSelected: (String) -> Unit,
    favoriteEmojis: Set<String> = emptySet(),
    showOnlyFavorites: Boolean = false,
    onToggleFavoriteEmoji: (String) -> Unit = {}
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val displayEmojis = remember(searchQuery, selectedCategoryIndex, showOnlyFavorites, favoriteEmojis) {
        when {
            showOnlyFavorites -> favoriteEmojis.toList()
            searchQuery.isNotBlank() -> EMOJI_CATEGORIES.flatMap { it.emojis }.filter { it.contains(searchQuery) }
            else -> EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.emojis ?: emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Favorites row — shown above categories when not in showOnlyFavorites mode
        if (!showOnlyFavorites && favoriteEmojis.isNotEmpty() && searchQuery.isBlank()) {
            Text(
                "★ ${LocalStrings.current.emotePickerFavoritesSection}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 88.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(favoriteEmojis.toList(), key = { "fav_$it" }) { emoji ->
                    EmojiGridItem(
                        emoji = emoji,
                        isFavorite = true,
                        onClick = { onEmojiSelected(emoji) },
                        onToggleFavorite = { onToggleFavoriteEmoji(emoji) }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        }
        if (searchQuery.isBlank()) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex, modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp, divider = {}
            ) {
                EMOJI_CATEGORIES.forEachIndexed { index, category ->
                    Tab(selected = selectedCategoryIndex == index, onClick = { selectedCategoryIndex = index },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedCategoryIndex == index)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(category.icon, fontSize = 18.sp, textAlign = TextAlign.Center) }
                    }
                }
            }
            Text(
                EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.label ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (displayEmojis.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(LocalStrings.current.emoteNoEmojiFound, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 100.dp, max = 280.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(
                    items = displayEmojis,
                    key = { index, emoji -> "$index-$emoji" }
                ) { _, emoji ->
                    EmojiGridItem(
                        emoji = emoji,
                        isFavorite = emoji in favoriteEmojis,
                        onClick = { onEmojiSelected(emoji) },
                        onToggleFavorite = { onToggleFavoriteEmoji(emoji) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiGridItem(
    emoji: String,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val interactionSource = remember(emoji) { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isFavorite) Color(0xFFFFD700).copy(alpha = 0.12f) else Color.Transparent
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 22.sp, textAlign = TextAlign.Center)

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) strings.emotePickerRemoveFavorite
                        else strings.emotePickerAddFavorite,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Star, null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isFavorite) Color(0xFFFFD700)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { onToggleFavorite(); showMenu = false }
            )
        }
    }
}