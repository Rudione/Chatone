package io.rudione.chatone.presentation.automod

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneNumberField
import io.rudione.chatone.presentation.components.ChatoneTextField
import io.rudione.chatone.presentation.components.ExpressiveCheckRow
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.RotateCcw

@Composable
internal fun ChatRuleReplySection(
    rule: ChatRule,
    onChange: (ChatRule) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val defaultTemplate = s.defaultChatRuleReply(rule.type)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveCheckRow(
            title = s.chatRuleReplyEnabled,
            description = s.chatRuleReplyEnabledDesc,
            checked = rule.replyEnabled,
            onCheckedChange = { onChange(rule.copy(replyEnabled = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(visible = rule.replyEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatoneTextField(
                    value = rule.replyTemplate.ifBlank { defaultTemplate },
                    onValueChange = { text ->
                        onChange(rule.copy(replyTemplate = text.takeUnless { it == defaultTemplate }.orEmpty()))
                    },
                    label = s.chatRuleReplyTemplate,
                    hint = s.chatRuleReplyTemplateHint,
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailing = if (rule.replyTemplate.isNotBlank()) {
                        {
                            LiquidGlassTooltipBox(tooltip = s.chatRuleReplyReset) {
                                ChatoneIconButton(
                                    onClick = { onChange(rule.copy(replyTemplate = "")) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Lucide.RotateCcw,
                                        contentDescription = s.chatRuleReplyReset,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                ChatoneNumberField(
                    value = rule.replyCooldownSeconds,
                    onValueChange = { onChange(rule.copy(replyCooldownSeconds = it)) },
                    label = s.chatRuleReplyCooldown,
                    hint = s.chatRuleReplyCooldownDesc,
                    range = 1..ChatRule.MAX_REPLY_COOLDOWN_SECONDS,
                    suffix = s.unitSecondsLabel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
