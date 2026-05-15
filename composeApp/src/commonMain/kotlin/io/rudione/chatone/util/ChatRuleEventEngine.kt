package io.rudione.chatone.util

import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ChatRuleEventEngine {

    fun fireStreamOnline(
        scope: CoroutineScope,
        channelLogin: String,
        rules: List<ChatRule>,
        send: suspend (text: String) -> Unit
    ): Job? = fireType(scope, channelLogin, rules, ChatRuleType.STREAM_ONLINE, mapOf(), send)

    fun fireStreamOffline(
        scope: CoroutineScope,
        channelLogin: String,
        rules: List<ChatRule>,
        send: suspend (text: String) -> Unit
    ): Job? = fireType(scope, channelLogin, rules, ChatRuleType.STREAM_OFFLINE, mapOf(), send)

    fun fireFirstMessage(
        scope: CoroutineScope,
        channelLogin: String,
        username: String,
        rules: List<ChatRule>,
        send: suspend (text: String) -> Unit
    ): Job? = fireType(
        scope, channelLogin, rules, ChatRuleType.FIRST_MESSAGE_GREETING,
        mapOf("user" to username), send
    )

    fun fireRaid(
        scope: CoroutineScope,
        channelLogin: String,
        raiderLogin: String,
        viewers: Int,
        rules: List<ChatRule>,
        send: suspend (text: String) -> Unit
    ): Job? = fireType(
        scope, channelLogin, rules, ChatRuleType.RAID_WELCOME,
        mapOf("raider" to raiderLogin, "viewers" to viewers.toString()), send
    )

    private fun fireType(
        scope: CoroutineScope,
        channelLogin: String,
        rules: List<ChatRule>,
        type: ChatRuleType,
        substitutions: Map<String, String>,
        send: suspend (text: String) -> Unit
    ): Job? {
        val applicable = rules.firstOrNull { rule ->
            rule.enabled && rule.type == type && rule.eventMessage.isNotBlank() && (
                rule.scope == AutomodScope.GLOBAL ||
                rule.channelLogin.equals(channelLogin, ignoreCase = true)
            )
        } ?: return null
        val text = applyVars(applicable.eventMessage, channelLogin, substitutions)
        val repeat = applicable.eventRepeat.coerceIn(1, 10)
        val delaySec = applicable.eventDelaySeconds.coerceIn(0, 600)
        return scope.launch {
            repeat(repeat) { i ->
                if (i > 0 && delaySec > 0) delay(delaySec * 1000L)
                runCatching { send(text) }
            }
        }
    }

    private fun applyVars(template: String, channelLogin: String, vars: Map<String, String>): String {
        var out = template.replace("{channel}", channelLogin, ignoreCase = true)
        vars.forEach { (k, v) -> out = out.replace("{$k}", v, ignoreCase = true) }
        return out
    }
}
