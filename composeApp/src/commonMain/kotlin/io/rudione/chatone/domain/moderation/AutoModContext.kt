package io.rudione.chatone.domain.moderation

import androidx.compose.runtime.Immutable

@Immutable
data class AutoModContext(
    val accountCreatedAtMs: Long? = null,
    val followedAtMs: Long? = null,
    val followKnown: Boolean = false,
    val subscriptionTier: Int? = null,
    val timeouts: Int? = null,
    val bans: Int? = null,
    val messages: Int? = null
)

enum class AutoModInsightKind { BANS, TIMEOUTS, NEW_ACCOUNT, ACCOUNT_AGE, FOLLOWING, NOT_FOLLOWING, SUBSCRIBER, FIRST_MESSAGE, MESSAGES }

enum class InsightTone { NEUTRAL, POSITIVE, WARNING, DANGER }

@Immutable
data class AutoModInsight(
    val kind: AutoModInsightKind,
    val tone: InsightTone,
    val sinceMs: Long? = null,
    val count: Int? = null
)

object AutoModInsights {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    const val NEW_ACCOUNT_DAYS = 7
    const val LOYAL_FOLLOW_DAYS = 30

    fun of(context: AutoModContext, nowMs: Long): List<AutoModInsight> = buildList {
        context.bans?.takeIf { it > 0 }?.let { add(AutoModInsight(AutoModInsightKind.BANS, InsightTone.DANGER, count = it)) }
        context.timeouts?.takeIf { it > 0 }?.let { add(AutoModInsight(AutoModInsightKind.TIMEOUTS, InsightTone.WARNING, count = it)) }
        context.accountCreatedAtMs?.let { created ->
            val fresh = nowMs - created < NEW_ACCOUNT_DAYS * DAY_MS
            add(
                if (fresh) AutoModInsight(AutoModInsightKind.NEW_ACCOUNT, InsightTone.WARNING, sinceMs = created)
                else AutoModInsight(AutoModInsightKind.ACCOUNT_AGE, InsightTone.NEUTRAL, sinceMs = created)
            )
        }
        when {
            context.followedAtMs != null -> {
                val loyal = nowMs - context.followedAtMs >= LOYAL_FOLLOW_DAYS * DAY_MS
                add(
                    AutoModInsight(
                        AutoModInsightKind.FOLLOWING,
                        if (loyal) InsightTone.POSITIVE else InsightTone.NEUTRAL,
                        sinceMs = context.followedAtMs
                    )
                )
            }
            context.followKnown -> add(AutoModInsight(AutoModInsightKind.NOT_FOLLOWING, InsightTone.WARNING))
        }
        context.subscriptionTier?.let { add(AutoModInsight(AutoModInsightKind.SUBSCRIBER, InsightTone.POSITIVE, count = it)) }
        context.messages?.let { count ->
            add(
                if (count == 0) AutoModInsight(AutoModInsightKind.FIRST_MESSAGE, InsightTone.WARNING)
                else AutoModInsight(AutoModInsightKind.MESSAGES, InsightTone.NEUTRAL, count = count)
            )
        }
    }

    fun tierOf(raw: String?): Int? = when (raw?.uppercase()) {
        null -> null
        "TIER_1", "1000", "PRIME" -> 1
        "TIER_2", "2000" -> 2
        "TIER_3", "3000" -> 3
        else -> raw.filter(Char::isDigit).take(1).toIntOrNull()
    }
}
