package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.theme.ChatoneTheme

// ── Native emoji data ────────────────────────────────────────────────────────

private data class EmojiCategory(val icon: String, val label: String, val emojis: List<String>)

private val EMOJI_CATEGORIES = listOf(
    EmojiCategory("😀", "Smileys", listOf(
        "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","🫠","😉","😊","😇",
        "🥰","😍","🤩","😘","😗","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑",
        "🤗","🤭","🫢","🫣","🤫","🤔","🫡","🤐","🤨","😐","😑","😶","🫥","😏",
        "😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮",
        "🤧","🥵","🥶","🥴","😵","🤯","🤠","🥳","🥸","😎","🤓","🧐","😕","🫤",
        "😟","🙁","☹️","😮","😯","😲","😳","🥺","🫣","😦","😧","😨","😰","😥",
        "😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬",
        "😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖"
    )),
    EmojiCategory("👋", "People", listOf(
        "👋","🤚","🖐️","✋","🖖","🫱","🫲","🫳","🫴","👌","🤌","🤏","✌️","🤞",
        "🫰","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","🫵","👍","👎","✊",
        "👊","🤛","🤜","👏","🙌","🫶","👐","🤲","🤝","🙏","✍️","💅","🤳","💪",
        "🦾","🦿","🦵","🦶","👂","🦻","👃","🫀","🫁","🧠","🦷","🦴","👀","👁️",
        "👅","👄","🫦","💋","👶","🧒","👦","👧","🧑","👱","👨","🧔","👩","🧓",
        "👴","👵","🙍","🙎","🙅","🙆","💁","🙋","🧏","🙇","🤦","🤷","💆","💇",
        "🚶","🧍","🧎","🏃","💃","🕺","🕴️","👫","👬","👭","💑","👨‍👩‍👦","👨‍👩‍👧"
    )),
    EmojiCategory("🐶", "Animals", listOf(
        "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐻‍❄️","🐨","🐯","🦁","🐮","🐷",
        "🐸","🐵","🙈","🙉","🙊","🐒","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇",
        "🐺","🐗","🐴","🦄","🐝","🪱","🐛","🦋","🐌","🐞","🐜","🪲","🦟","🦗",
        "🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟",
        "🐬","🐳","🐋","🦈","🦭","🐊","🐅","🐆","🦓","🦍","🦧","🦣","🐘","🦛",
        "🦏","🐪","🐫","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖","🐏","🐑","🦙",
        "🐐","🦌","🐕","🐩","🦮","🐕‍🦺","🐈","🐈‍⬛","🪶","🐓","🦃","🦤","🦚","🦜"
    )),
    EmojiCategory("🍎", "Food", listOf(
        "🍎","🍊","🍋","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅",
        "🍆","🥑","🥦","🥬","🥒","🌶️","🫑","🧄","🧅","🥔","🍠","🫘","🌰","🥜",
        "🍞","🥐","🥖","🫓","🥨","🥯","🧀","🥚","🍳","🧈","🥞","🧇","🥓","🥩",
        "🍗","🍖","🦴","🌭","🍔","🍟","🍕","🫔","🌮","🌯","🥙","🧆","🥚","🍿",
        "🧂","🥫","🍱","🍘","🍙","🍚","🍛","🍜","🍝","🍠","🦪","🍣","🍤","🍙",
        "🥟","🦑","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭",
        "🍮","🍯","🍼","🥛","☕","🫖","🍵","🧃","🥤","🧋","🍶","🍺","🍻","🥂"
    )),
    EmojiCategory("⚽", "Activities", listOf(
        "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒",
        "🏑","🥍","🏏","🪃","🥅","⛳","🪁","🎣","🤿","🎽","🎿","🛷","🥌","🎯",
        "🪃","🎱","🔮","🪄","🎮","🕹️","🎲","♟️","🧩","🧸","🪅","🎭","🎨","🖼️",
        "🎪","🤹","🎬","🎤","🎧","🎼","🎵","🎶","🎹","🥁","🪘","🎷","🎺","🪗",
        "🎸","🪕","🎻","🏆","🥇","🥈","🥉","🏅","🎖️","🏵️","🎗️","🎫","🎟️","🎪"
    )),
    EmojiCategory("🚗", "Travel", listOf(
        "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜",
        "🏍️","🛵","🚲","🛴","🛹","🛼","🚏","🛣️","🛤️","⛽","🚧","🚦","🚥","🚢",
        "✈️","🛩️","🛫","🛬","🪂","💺","🚁","🚟","🚠","🚡","🛰️","🚀","🛸","🪐",
        "⭐","🌟","💫","✨","🌈","☀️","🌤️","⛅","🌥️","🌦️","🌧️","⛈️","🌩️","🌨️",
        "🌪️","🌫️","🌬️","🌀","🌊","🌁","🏔️","⛰️","🌋","🗻","🏕️","🏖️","🏜️","🏝️"
    )),
    EmojiCategory("💡", "Objects", listOf(
        "⌚","📱","💻","⌨️","🖥️","🖨️","🖱️","🖲️","💾","💿","📀","📷","📸","📹",
        "🎥","📽️","🎞️","📞","☎️","📟","📠","📺","📻","🧭","⏱️","⏲️","⏰","🕰️",
        "⌛","⏳","📡","🔋","🔌","💡","🔦","🕯️","🧱","💰","💴","💵","💶","💷",
        "💸","💳","🧾","💹","📈","📉","📊","📋","📌","📍","📎","🖇️","📏","📐",
        "✂️","🗃️","🗄️","🗑️","🔒","🔓","🔏","🔐","🔑","🗝️","🔨","🪓","⛏️","⚒️",
        "🛠️","🗡️","⚔️","🛡️","🪚","🔧","🪛","🔩","⚙️","🗜️","⚖️","🦯","🔗","⛓️",
        "🪝","🧲","🪜","⚗️","🧪","🧫","🧬","🔭","🔬","🩺","💊","💉","🩹","🩼"
    )),
    EmojiCategory("❤️", "Symbols", listOf(
        "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","💕","💞",
        "💓","💗","💖","💘","💝","💟","☮️","✝️","☪️","🕉️","☸️","✡️","🔯","🕎","☯️",
        "🛐","⛎","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓",
        "🆔","⚛️","🉑","☢️","☣️","📴","📳","🈶","🈚","🈸","🈺","🈷️","✴️","🆚",
        "💮","🉐","㊙️","㊗️","🈴","🈵","🈹","🈲","🅰️","🅱️","🆎","🆑","🅾️","🆘",
        "❌","⭕","🛑","⛔","📛","🚫","💯","💢","♨️","🚷","🚯","🚳","🚱","🔞",
        "📵","🔕","🎵","🎶","💲","💱","™️","©️","®️","〰️","➰","➿","🔚","🔙"
    )),
    EmojiCategory("🎌", "Flags", listOf(
        "🏳️","🏴","🏁","🚩","🏳️‍🌈","🏳️‍⚧️","🏴‍☠️","🇦🇫","🇦🇱","🇩🇿","🇦🇩","🇦🇴","🇦🇷","🇦🇲",
        "🇦🇺","🇦🇹","🇦🇿","🇧🇸","🇧🇭","🇧🇩","🇧🇧","🇧🇾","🇧🇪","🇧🇿","🇧🇯","🇧🇹","🇧🇴","🇧🇦",
        "🇧🇼","🇧🇷","🇧🇳","🇧🇬","🇧🇫","🇧🇮","🇨🇻","🇰🇭","🇨🇲","🇨🇦","🇨🇫","🇹🇩","🇨🇱","🇨🇳",
        "🇨🇴","🇨🇷","🇭🇷","🇨🇺","🇨🇾","🇨🇿","🇩🇰","🇩🇯","🇩🇴","🇪🇨","🇪🇬","🇸🇻","🇬🇶","🇪🇷",
        "🇪🇪","🇸🇿","🇪🇹","🇫🇯","🇫🇮","🇫🇷","🇬🇦","🇬🇲","🇬🇪","🇩🇪","🇬🇭","🇬🇷","🇬🇹","🇬🇳",
        "🇬🇼","🇬🇾","🇭🇹","🇭🇳","🇭🇺","🇮🇸","🇮🇳","🇮🇩","🇮🇷","🇮🇶","🇮🇪","🇮🇱","🇮🇹","🇯🇲",
        "🇯🇵","🇯🇴","🇰🇿","🇰🇪","🇰🇷","🇰🇼","🇰🇬","🇱🇦","🇱🇻","🇱🇧","🇱🇸","🇱🇷","🇱🇾","🇱🇮"
    ))
)

// ── Sheet ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotePickerSheet(
    emotes: List<GenericEmote>,
    onEmoteSelected: (GenericEmote) -> Unit,
    onEmojiSelected: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    // Tab 0 = Emotes, Tab 1 = Emoji
    var mainTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        ) {
            // ── Main tabs: Emotes / Emoji ────────────────────────
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
                    emotes = emotes,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onEmoteSelected = onEmoteSelected
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

// ── Emote tab (unchanged logic, just extracted) ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmoteTab(
    emotes: List<GenericEmote>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmoteSelected: (GenericEmote) -> Unit
) {
    data class ProviderTab(val label: String, val provider: EmoteProvider?, val count: Int)

    val tabs = remember(emotes) {
        val all = emotes.size
        val s7 = emotes.count { it.provider == EmoteProvider.SEVEN_TV }
        val bt = emotes.count { it.provider == EmoteProvider.BTTV }
        val fz = emotes.count { it.provider == EmoteProvider.FFZ }
        val tw = emotes.count { it.provider == EmoteProvider.TWITCH }
        buildList {
            add(ProviderTab("All ($all)", null, all))
            if (tw > 0) add(ProviderTab("Twitch", EmoteProvider.TWITCH, tw))
            if (s7 > 0) add(ProviderTab("7TV", EmoteProvider.SEVEN_TV, s7))
            if (bt > 0) add(ProviderTab("BTTV", EmoteProvider.BTTV, bt))
            if (fz > 0) add(ProviderTab("FFZ", EmoteProvider.FFZ, fz))
        }
    }

    var selectedProviderTab by remember { mutableIntStateOf(0) }

    val filteredEmotes = remember(emotes, searchQuery, selectedProviderTab, tabs) {
        var filtered = emotes
        val tab = tabs.getOrNull(selectedProviderTab)
        if (tab?.provider != null) filtered = filtered.filter { it.provider == tab.provider }
        if (searchQuery.isNotBlank()) filtered = filtered.filter { it.code.contains(searchQuery, ignoreCase = true) }
        filtered
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Search bar
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
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
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

        // Provider sub-tabs
        ScrollableTabRow(
            selectedTabIndex = selectedProviderTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedProviderTab == index,
                    onClick = { selectedProviderTab = index },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedProviderTab == index) FontWeight.SemiBold else FontWeight.Normal
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
                        else "No emotes loaded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { onSearchQueryChange("") }) { Text("Clear search") }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp, max = 280.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredEmotes, key = { "${it.provider}_${it.id}" }) { emote ->
                    EmoteGridItem(emote = emote, onClick = { onEmoteSelected(emote) })
                }
            }
        }
    }
}

// ── Emoji tab ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // When there's a search query show results across all categories
    val displayEmojis = remember(searchQuery, selectedCategoryIndex) {
        if (searchQuery.isNotBlank()) {
            // simple substring match on codepoint name not available in KMP,
            // so just match the emoji character itself or show all
            EMOJI_CATEGORIES.flatMap { it.emojis }
                .filter { it.contains(searchQuery) }
        } else {
            EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.emojis ?: emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Search bar
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
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
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

        // Category icon tabs — only visible when not searching
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

            // Category label
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
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

// ── Grid items ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmoteGridItem(
    emote: GenericEmote,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedEmoteImage(
            url = emote.url2x,
            contentDescription = emote.code,
            modifier = Modifier.size(36.dp)
        )
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
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}