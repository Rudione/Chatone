package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
fun ChatSearchBar(
    query: String,
    matchCount: Int,
    currentMatchIndex: Int,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier
                    .width(180.dp)
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when {
                                event.key == Key.Enter && event.isShiftPressed -> { onPrevious(); true }
                                event.key == Key.Enter -> { onNext(); true }
                                event.key == Key.Escape -> { onClose(); true }
                                else -> false
                            }
                        } else false
                    },
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search in chat…",
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        inner()
                    }
                }
            )

            if (query.isNotEmpty()) {
                Text(
                    text = if (matchCount == 0) "No results" else "${currentMatchIndex + 1} / $matchCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (matchCount == 0)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.widthIn(min = 48.dp)
                )
            }

            val btnColors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ChatoneIconButton(
                onClick = onPrevious,
                enabled = matchCount > 0,
                modifier = Modifier.size(28.dp),
                colors = btnColors
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Previous match",
                    modifier = Modifier.size(18.dp)
                )
            }
            ChatoneIconButton(
                onClick = onNext,
                enabled = matchCount > 0,
                modifier = Modifier.size(28.dp),
                colors = btnColors
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Next match",
                    modifier = Modifier.size(18.dp)
                )
            }

            ChatoneIconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp),
                colors = btnColors
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close search",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
