package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Languages
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.chat.TranslationLanguages
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.expressive.ClosingTap
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.components.PortalPopup

private val ChipShape = RoundedCornerShape(10.dp)
private val MenuShape = RoundedCornerShape(12.dp)
private val MenuGap = 8.dp

@Composable
internal fun MessageInputTranslationChip(
    targetLang: String,
    enabled: Boolean,
    onSelectLanguage: (String) -> Unit,
    onTurnOff: () -> Unit
) {
    val s = LocalStrings.current
    val accent = MaterialTheme.colorScheme.primary
    var menuOpen by remember { mutableStateOf(false) }
    val closing = remember { ClosingTap() }
    Box(Modifier.padding(end = 4.dp)) {
        Row(
            modifier = Modifier
                .height(28.dp)
                .clip(ChipShape)
                .background(accent.copy(alpha = if (enabled) 0.14f else 0.06f))
                .pointerInput(closing) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        closing.pressed()
                    }
                }
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = s.inputTranslationLanguage) {
                    val wasOpen = menuOpen || closing.consume()
                    menuOpen = !wasOpen
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Lucide.Languages, contentDescription = s.inputTranslationLanguage, tint = accent, modifier = Modifier.size(14.dp))
            Text(
                languageBadge(targetLang),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1
            )
        }
        if (menuOpen) {
            TranslationLanguageMenu(
                targetLang = targetLang,
                onSelect = { code ->
                    menuOpen = false
                    if (code != targetLang) onSelectLanguage(code)
                },
                onTurnOff = {
                    menuOpen = false
                    onTurnOff()
                },
                onDismiss = {
                    if (menuOpen) {
                        menuOpen = false
                        closing.closed()
                    }
                }
            )
        }
    }
}

@Composable
private fun TranslationLanguageMenu(
    targetLang: String,
    onSelect: (String) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val gapPx = with(LocalDensity.current) { MenuGap.roundToPx() }
    val positionProvider = remember(gapPx) { AboveAnchorEnd(gapPx) }
    PortalPopup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
    ) {
        LiquidGlassSurface(
            modifier = Modifier.widthIn(min = 180.dp, max = 240.dp),
            shape = MenuShape,
            contentPadding = PaddingValues(vertical = 4.dp),
            backgroundAlphaHigh = 0.94f,
            backgroundAlphaLow = 0.85f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            Column {
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    TranslationLanguages.forEach { (code, name) ->
                        LanguageRow(name = name, selected = code == targetLang) { onSelect(code) }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                LiquidGlassDropdownItem(
                    text = s.inputTranslationOff,
                    icon = Lucide.X,
                    onClick = onTurnOff
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Lucide.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

private class AboveAnchorEnd(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val above = anchorBounds.top - popupContentSize.height - gapPx
        val y = if (above >= 0) above else anchorBounds.bottom + gapPx
        return IntOffset(x, y)
    }
}

internal fun languageBadge(code: String): String = code.substringBefore('-').uppercase()
