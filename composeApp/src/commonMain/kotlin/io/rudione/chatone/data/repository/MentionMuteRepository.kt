package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings

class MentionMuteRepository(private val settings: Settings = Settings()) {

    companion object {
        private const val KEY = "mention_mute_rules"
        private const val SEP = "\n"
    }

    private fun loadRules(): Set<String> {
        return settings.getStringOrNull(KEY)
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    private fun saveRules(rules: Set<String>) {
        settings.putString(KEY, rules.joinToString(SEP))
    }

    fun muteUser(login: String) {
        val rules = loadRules().toMutableSet()
        rules.add("user:${login.lowercase()}")
        saveRules(rules)
    }

    fun unmuteUser(login: String) {
        val rules = loadRules().toMutableSet()
        rules.remove("user:${login.lowercase()}")
        saveRules(rules)
    }

    fun muteChannel(channelLogin: String) {
        val rules = loadRules().toMutableSet()
        rules.add("channel:${channelLogin.lowercase()}")
        saveRules(rules)
    }

    fun unmuteChannel(channelLogin: String) {
        val rules = loadRules().toMutableSet()
        rules.remove("channel:${channelLogin.lowercase()}")
        saveRules(rules)
    }

    fun muteUserInChannel(userLogin: String, channelLogin: String) {
        val rules = loadRules().toMutableSet()
        rules.add("userinchannel:${userLogin.lowercase()}@${channelLogin.lowercase()}")
        saveRules(rules)
    }

    fun unmuteUserInChannel(userLogin: String, channelLogin: String) {
        val rules = loadRules().toMutableSet()
        rules.remove("userinchannel:${userLogin.lowercase()}@${channelLogin.lowercase()}")
        saveRules(rules)
    }

    fun isMuted(userLogin: String, channelLogin: String): Boolean {
        val rules = loadRules()
        val u = userLogin.lowercase()
        val c = channelLogin.lowercase()
        return "user:$u" in rules
                || "channel:$c" in rules
                || "userinchannel:${u}@${c}" in rules
    }

    fun isUserMuted(userLogin: String): Boolean =
        "user:${userLogin.lowercase()}" in loadRules()

    fun isChannelMuted(channelLogin: String): Boolean =
        "channel:${channelLogin.lowercase()}" in loadRules()

    fun isUserMutedInChannel(userLogin: String, channelLogin: String): Boolean =
        "userinchannel:${userLogin.lowercase()}@${channelLogin.lowercase()}" in loadRules()

    fun getAllRules(): List<MuteRule> {
        return loadRules().mapNotNull { raw ->
            when {
                raw.startsWith("user:") -> MuteRule.User(raw.removePrefix("user:"))
                raw.startsWith("channel:") -> MuteRule.Channel(raw.removePrefix("channel:"))
                raw.startsWith("userinchannel:") -> {
                    val parts = raw.removePrefix("userinchannel:").split("@")
                    if (parts.size == 2) MuteRule.UserInChannel(parts[0], parts[1]) else null
                }
                else -> null
            }
        }
    }

    fun removeRule(rule: MuteRule) {
        val key = when (rule) {
            is MuteRule.User -> "user:${rule.login}"
            is MuteRule.Channel -> "channel:${rule.channelLogin}"
            is MuteRule.UserInChannel -> "userinchannel:${rule.userLogin}@${rule.channelLogin}"
        }
        val rules = loadRules().toMutableSet()
        rules.remove(key)
        saveRules(rules)
    }

    sealed class MuteRule {
        data class User(val login: String) : MuteRule()
        data class Channel(val channelLogin: String) : MuteRule()
        data class UserInChannel(val userLogin: String, val channelLogin: String) : MuteRule()
    }
}
