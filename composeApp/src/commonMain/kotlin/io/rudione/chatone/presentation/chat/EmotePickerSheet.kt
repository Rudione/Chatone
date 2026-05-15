package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import io.rudione.chatone.presentation.chat.components.EmoteGridItemFlyweight
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.snapshotFlow
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
                onEmojiSelected = { emoji -> onEmojiSelected(emoji); onDismiss() },
                onToggleFavoriteEmoji = { toggleFavoriteEmoji(it) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private sealed class PickerTab {
    object Favorites : PickerTab()
    object All : PickerTab()
    object TwitchChannel : PickerTab()
    object TwitchGlobal : PickerTab()
    object TwitchSubscribed : PickerTab()
    object SevenTv : PickerTab()
    object Bttv : PickerTab()
    object Ffz : PickerTab()
    object Emoji : PickerTab()
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
    val twitchGlobal: List<GenericEmote> = emptyList()
    val twitchSubscribed = personalEmotes.filter { it.provider == EmoteProvider.TWITCH }
    val hasTwitchChannel = twitchChannel.isNotEmpty()
    val hasTwitchSubscribed = twitchSubscribed.isNotEmpty()
    val s7 = channelEmotes.byProvider[EmoteProvider.SEVEN_TV]?.size ?: 0
    val bt = channelEmotes.byProvider[EmoteProvider.BTTV]?.size ?: 0
    val fz = channelEmotes.byProvider[EmoteProvider.FFZ]?.size ?: 0

    val tabs = remember(allEmotes.size, favoriteEmotes.size, hasTwitchChannel, hasTwitchSubscribed, s7, bt, fz) {
        buildList<TabEntry> {
            if (favoriteEmotes.isNotEmpty()) add(TabEntry("★ ${favoriteEmotes.size}", PickerTab.Favorites, favoriteEmotes.size))
            add(TabEntry("All (${allEmotes.size})", PickerTab.All, allEmotes.size))
            if (hasTwitchChannel) add(TabEntry("Twitch Ch.", PickerTab.TwitchChannel, twitchChannel.size))
            if (twitchGlobal.isNotEmpty()) add(TabEntry("Twitch Global", PickerTab.TwitchGlobal, twitchGlobal.size))
            if (hasTwitchSubscribed) add(TabEntry("Sub ★", PickerTab.TwitchSubscribed, twitchSubscribed.size))
            if (s7 > 0) add(TabEntry("7TV", PickerTab.SevenTv, s7))
            if (bt > 0) add(TabEntry("BTTV", PickerTab.Bttv, bt))
            if (fz > 0) add(TabEntry("FFZ", PickerTab.Ffz, fz))
            add(TabEntry("😀 Emoji", PickerTab.Emoji, 0))
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val safeTab = selectedTabIndex.coerceAtMost(tabs.lastIndex.coerceAtLeast(0))
    val currentTab = tabs.getOrNull(safeTab)
    val isEmojiTab = currentTab?.tab == PickerTab.Emoji

    val currentEmoteSource = remember(currentTab, allEmotes, favoriteEmotes, twitchChannel, twitchSubscribed) {
        when (currentTab?.tab) {
            PickerTab.Favorites -> favoriteEmotes
            PickerTab.All -> allEmotes
            PickerTab.TwitchChannel -> twitchChannel
            PickerTab.TwitchGlobal -> twitchGlobal
            PickerTab.TwitchSubscribed -> twitchSubscribed
            PickerTab.SevenTv -> channelEmotes.byProvider[EmoteProvider.SEVEN_TV] ?: emptyList()
            PickerTab.Bttv -> channelEmotes.byProvider[EmoteProvider.BTTV] ?: emptyList()
            PickerTab.Ffz -> channelEmotes.byProvider[EmoteProvider.FFZ] ?: emptyList()
            else -> emptyList()
        }
    }

    val debouncedQuery by remember(searchQuery) { derivedStateOf { searchQuery } }

    val searchIndex by produceState<EmoteSearchIndex?>(
        initialValue = null,
        currentEmoteSource.size, currentTab
    ) {
        if (isEmojiTab) { value = null; return@produceState }
        value = withContext(Dispatchers.Default) {
            EmoteSearchIndex().also { it.build(currentEmoteSource) }
        }
    }

    val allFilteredEmotes = remember(searchIndex, debouncedQuery, currentEmoteSource) {
        if (isEmojiTab) return@remember emptyList()
        if (debouncedQuery.isBlank() || searchIndex == null) currentEmoteSource
        else searchIndex!!.search(debouncedQuery, limit = 500)
    }

    var loadedCount by remember(allFilteredEmotes.size) { mutableIntStateOf(PAGE_SIZE) }

    LaunchedEffect(allFilteredEmotes.size, safeTab, debouncedQuery) {
        loadedCount = PAGE_SIZE.coerceAtMost(allFilteredEmotes.size)
    }

    val visibleEmotes = remember(allFilteredEmotes, loadedCount) {
        if (loadedCount >= allFilteredEmotes.size) allFilteredEmotes
        else allFilteredEmotes.subList(0, loadedCount)
    }

    val hasMore = loadedCount < allFilteredEmotes.size
    var isLoadingMore by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(safeTab, debouncedQuery) {
        if (gridState.canScrollForward || gridState.canScrollBackward) gridState.scrollToItem(0)
        isLoadingMore = false
    }

    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { isScrolling = it }
    }

    val shouldLoadMore = remember(hasMore, visibleEmotes.size, gridState, isLoadingMore) {
        derivedStateOf {
            if (!hasMore || isLoadingMore) return@derivedStateOf false
            val layoutInfo = gridState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) return@derivedStateOf false
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (lastVisible < 0) return@derivedStateOf false
            lastVisible >= (visibleEmotes.size - PRELOAD_THRESHOLD).coerceAtLeast(0)
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMore && hasMore) {
            isLoadingMore = true
            try {
                delay(8)
                loadedCount = (loadedCount + PAGE_SIZE).coerceAtMost(allFilteredEmotes.size)
            } finally {
                isLoadingMore = false
            }
        }
    }

    val visibleEmoteKeys = remember(gridState.layoutInfo) {
        gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(LocalStrings.current.emoteSearchPlaceholder, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            )
        )

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
        } else if (currentTab?.tab == PickerTab.Favorites && favoriteEmojis.isNotEmpty() && allFilteredEmotes.isEmpty()) {
            // Show favorite emojis when there are no favorite emotes but there are fav emoji
            EmojiTab(
                searchQuery = "",
                onSearchQueryChange = {},
                onEmojiSelected = onEmojiSelected,
                favoriteEmojis = favoriteEmojis,
                showOnlyFavorites = true,
                onToggleFavoriteEmoji = onToggleFavoriteEmoji
            )
        } else if (allFilteredEmotes.isEmpty()) {
            EmptyEmoteState(
                searchQuery = debouncedQuery,
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
                items(
                    items = visibleEmotes,
                    key = { it.listKey },
                    contentType = { it.provider }
                ) { emote ->
                    val uiData = remember(emote.listKey, favoriteIds) {
                        EmoteUiData.fromEmote(emote, "${emote.provider}_${emote.id}" in favoriteIds)
                    }
                    EmoteGridItemFlyweight(
                        uiData = uiData,
                        isScrolling = isScrolling,
                        isVisible = emote.listKey in visibleEmoteKeys,
                        onClick = { onEmoteSelected(emote) },
                        onToggleFavorite = { onToggleFavorite(emote) }
                    )
                }
                if (hasMore) {
                    item(key = "loading_indicator", contentType = "loading") {
                        LoadMoreIndicator(isLoading = isLoadingMore)
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