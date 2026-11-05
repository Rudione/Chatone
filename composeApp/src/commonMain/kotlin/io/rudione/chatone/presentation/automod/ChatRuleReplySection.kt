package io.rudione.chatone.presentation.automod

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_rule_reply_cooldown
import chatone.composeapp.generated.resources.chat_rule_reply_cooldown_desc
import chatone.composeapp.generated.resources.chat_rule_reply_enabled
import chatone.composeapp.generated.resources.chat_rule_reply_enabled_desc
import chatone.composeapp.generated.resources.chat_rule_reply_reset
import chatone.composeapp.generated.resources.chat_rule_reply_template
import chatone.composeapp.generated.resources.chat_rule_reply_template_hint
import chatone.composeapp.generated.resources.unit_seconds_label
import org.jetbrains.compose.resources.stringResource
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
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.RotateCcw

@Composable
internal fun ChatRuleReplySection(
    rule: ChatRule,
    onChange: (ChatRule) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultTemplate = defaultChatRuleReplyRes(rule.type)?.let { stringResource(it) }.orEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveCheckRow(
            title = stringResource(Res.string.chat_rule_reply_enabled),
            description = stringResource(Res.string.chat_rule_reply_enabled_desc),
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
                    label = stringResource(Res.string.chat_rule_reply_template),
                    hint = stringResource(Res.string.chat_rule_reply_template_hint),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailing = if (rule.replyTemplate.isNotBlank()) {
                        {
                            LiquidGlassTooltipBox(tooltip = stringResource(Res.string.chat_rule_reply_reset)) {
                                ChatoneIconButton(
                                    onClick = { onChange(rule.copy(replyTemplate = "")) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Lucide.RotateCcw,
                                        contentDescription = stringResource(Res.string.chat_rule_reply_reset),
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
                    label = stringResource(Res.string.chat_rule_reply_cooldown),
                    hint = stringResource(Res.string.chat_rule_reply_cooldown_desc),
                    range = 1..ChatRule.MAX_REPLY_COOLDOWN_SECONDS,
                    suffix = stringResource(Res.string.unit_seconds_label),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
