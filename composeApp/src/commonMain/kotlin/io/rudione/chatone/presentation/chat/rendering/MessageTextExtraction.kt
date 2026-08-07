package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.TranslationLanguages
import io.rudione.chatone.presentation.chat.TranslationUiState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

internal fun DisplayMessage.PrivMsg.rawTokenText(): String =
    tokens.joinToString("") { token ->
        when (token) {
            is MessageToken.Text -> token.text
            is MessageToken.TwitchEmoteToken -> token.name
            is MessageToken.ThirdPartyEmoteToken -> token.emote.code
            is MessageToken.Link -> token.displayText
            is MessageToken.Mention -> token.username
            is MessageToken.Cheer -> "${token.prefix}${token.amount}"
        }
    }

@Composable
internal fun MessageTranslationLine(state: TranslationUiState?, onPickLanguage: (String) -> Unit) {
    if (state == null) return
    val s = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, end = 8.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box {
            Icon(
                Icons.Outlined.Translate, null,
                modifier = Modifier
                    .size(15.dp)
                    .padding(top = 1.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { menuOpen = true },
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            ChatoneDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                TranslationLanguages.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name, style = MaterialTheme.typography.bodySmall) },
                        onClick = { menuOpen = false; onPickLanguage(code) }
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        when (state) {
            TranslationUiState.Loading -> Text(
                s.chatTranslating,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            is TranslationUiState.Done -> Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)) {
                        append("${state.sourceLang} → ${state.targetLang}  ")
                    }
                    append(state.text)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            is TranslationUiState.Error -> Text(
                s.chatTranslationError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
