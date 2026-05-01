package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

    LaunchedEffect(Unit) { visible.value = true }

    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible.value) 0.35f else 0f,
        animationSpec = tween(220), label = "backdrop"
    )
    val backdropSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha))
            .clickable(indication = null, interactionSource = backdropSource) { onDismiss() }
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible.value,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(tween(150))
        ) {
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
                    .heightIn(max = 480.dp)
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

                TabRow(
                    selectedTabIndex = mainTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(selected = mainTab == 0, onClick = { mainTab = 0; searchQuery = "" },
                        text = { Text(LocalStrings.current.emoteTabEmotes, style = MaterialTheme.typography.labelLarge, fontWeight = if (mainTab == 0) FontWeight.Bold else FontWeight.Normal) })
                    Tab(selected = mainTab == 1, onClick = { mainTab = 1; searchQuery = "" },
                        text = { Text(LocalStrings.current.emoteTabEmoji, style = MaterialTheme.typography.labelLarge, fontWeight = if (mainTab == 1) FontWeight.Bold else FontWeight.Normal) })
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
                        onEmojiSelected = { emoji -> onEmojiSelected(emoji); onDismiss() }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun EmoteTab(
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
        val count: Int,
        val cacheKey: String
    )

    val tabs = remember(channelEmotes.all.size, favoriteEmotes.size) {
        buildList<ProviderTab>(8) {
            if (favoriteEmotes.isNotEmpty()) {
                add(ProviderTab("★ (${favoriteEmotes.size})", null, true, favoriteEmotes.size, "fav"))
            }
            val all = channelEmotes.all.size
            val tw = channelEmotes.byProvider[EmoteProvider.TWITCH]?.size ?: 0
            val s7 = channelEmotes.byProvider[EmoteProvider.SEVEN_TV]?.size ?: 0
            val bt = channelEmotes.byProvider[EmoteProvider.BTTV]?.size ?: 0
            val fz = channelEmotes.byProvider[EmoteProvider.FFZ]?.size ?: 0
            add(ProviderTab("All ($all)", null, false, all, "all"))
            if (tw > 0) add(ProviderTab("Twitch", EmoteProvider.TWITCH, false, tw, "tw"))
            if (s7 > 0) add(ProviderTab("7TV", EmoteProvider.SEVEN_TV, false, s7, "s7"))
            if (bt > 0) add(ProviderTab("BTTV", EmoteProvider.BTTV, false, bt, "bt"))
            if (fz > 0) add(ProviderTab("FFZ", EmoteProvider.FFZ, false, fz, "fz"))
        }
    }

    var selectedProviderTab by remember { mutableIntStateOf(0) }
    val safeTab = selectedProviderTab.coerceAtMost(tabs.lastIndex.coerceAtLeast(0))
    val currentTab = tabs.getOrNull(safeTab)


    val debouncedQuery by remember(searchQuery) {
        derivedStateOf { searchQuery }
    }


    val searchIndex by produceState<EmoteSearchIndex?>(
        initialValue = null,
        channelEmotes.all, favoriteEmotes, currentTab?.provider, currentTab?.isFav
    ) {
        value = withContext(Dispatchers.Default) {
            val source = when {
                currentTab?.isFav == true -> favoriteEmotes
                currentTab?.provider != null -> channelEmotes.byProvider[currentTab.provider] ?: emptyList()
                else -> channelEmotes.all
            }
            EmoteSearchIndex().also { it.build(source) }
        }
    }


    val allFilteredEmotes = remember(searchIndex, debouncedQuery, currentTab?.isFav, currentTab?.provider) {
        if (debouncedQuery.isBlank() || searchIndex == null) {
            when {
                currentTab?.isFav == true -> favoriteEmotes
                currentTab?.provider != null -> channelEmotes.byProvider[currentTab.provider] ?: emptyList()
                else -> channelEmotes.all
            }
        } else {
            searchIndex!!.search(debouncedQuery, limit = 500)
        }
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
        if (gridState.canScrollForward || gridState.canScrollBackward) {
            gridState.scrollToItem(0)
        }
        isLoadingMore = false
    }


    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .collect { scrolling ->
                isScrolling = scrolling
            }
    }


    val shouldLoadMore = remember(hasMore, visibleEmotes.size, gridState, isLoadingMore) {
        derivedStateOf {
            if (!hasMore || isLoadingMore) return@derivedStateOf false
            val layoutInfo = gridState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) return@derivedStateOf false
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (lastVisible < 0) return@derivedStateOf false
            val threshold = (visibleEmotes.size - PRELOAD_THRESHOLD).coerceAtLeast(0)
            lastVisible >= threshold
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
        gridState.layoutInfo.visibleItemsInfo
            .mapNotNull { it.key as? String }
            .toSet()
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


        ScrollableTabRow(
            selectedTabIndex = safeTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            edgePadding = 12.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safeTab == index,
                    onClick = { selectedProviderTab = index },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (safeTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (tab.isFav) Color(0xFFFFD700) else Color.Unspecified
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))


        if (allFilteredEmotes.isEmpty()) {
            EmptyEmoteState(
                searchQuery = debouncedQuery,
                isFavoritesTab = currentTab?.isFav == true,
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
    onEmojiSelected: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val displayEmojis = remember(searchQuery, selectedCategoryIndex) {
        if (searchQuery.isNotBlank()) EMOJI_CATEGORIES.flatMap { it.emojis }.filter { it.contains(searchQuery) }
        else EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.emojis ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    EmojiGridItem(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                }
            }
        }
    }
}

@Composable
private fun EmojiGridItem(emoji: String, onClick: () -> Unit) {
    val interactionSource = remember(emoji) { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}
