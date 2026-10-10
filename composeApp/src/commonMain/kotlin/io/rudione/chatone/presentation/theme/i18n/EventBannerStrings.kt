package io.rudione.chatone.presentation.theme.i18n

interface EventBannerStrings {
    val predictionOpen: String
    val predictionOpenShort: String
    val predictionLocked: String
    val predictionLockedShort: String
    val predictionResolved: String
    val predictionCanceled: String
    fun predictors(count: Int): String
    fun points(count: Int, compact: String): String
    fun predictionWon(outcome: String): String
    fun pollVotes(count: Int): String
    fun pollWon(choice: String, percent: Int, votes: Int): String
}

object EventBannerStringsEn : EventBannerStrings {
    override val predictionOpen = "Open"
    override val predictionOpenShort = "Open"
    override val predictionLocked = "Locked"
    override val predictionLockedShort = "Locked"
    override val predictionResolved = "Resolved"
    override val predictionCanceled = "Canceled"
    override fun predictors(count: Int) = if (count == 1) "1 predictor" else "$count predictors"
    override fun points(count: Int, compact: String) = "$compact pts"
    override fun predictionWon(outcome: String) = "“$outcome” won"
    override fun pollVotes(count: Int) = if (count == 1) "1 vote" else "$count votes"
    override fun pollWon(choice: String, percent: Int, votes: Int) = "“$choice” won: $percent%, ${pollVotes(votes)}"
}

object EventBannerStringsRu : EventBannerStrings {
    override val predictionOpen = "Ставки открыты"
    override val predictionOpenShort = "Открыто"
    override val predictionLocked = "Ставки закрыты"
    override val predictionLockedShort = "Закрыто"
    override val predictionResolved = "Итоги"
    override val predictionCanceled = "Отменён"
    override fun predictors(count: Int) = "$count ${plural(count, "участник", "участника", "участников")}"
    override fun points(count: Int, compact: String) = "$compact ${plural(count, "балл", "балла", "баллов")}"
    override fun predictionWon(outcome: String) = "Победил «$outcome»"
    override fun pollVotes(count: Int) = "$count ${plural(count, "голос", "голоса", "голосов")}"
    override fun pollWon(choice: String, percent: Int, votes: Int) = "Победил «$choice»: $percent%, ${pollVotes(votes)}"

    private fun plural(count: Int, one: String, few: String, many: String): String {
        if (count >= 1_000) return many
        val mod100 = count % 100
        val mod10 = count % 10
        return when {
            mod100 in 11..14 -> many
            mod10 == 1 -> one
            mod10 in 2..4 -> few
            else -> many
        }
    }
}
