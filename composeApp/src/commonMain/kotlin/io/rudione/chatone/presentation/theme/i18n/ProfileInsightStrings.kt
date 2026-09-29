package io.rudione.chatone.presentation.theme.i18n

interface ProfileInsightStrings {
    val tabRoles: String
    val roleModerators: String
    val roleVips: String
    val roleFounders: String
    val roleArtists: String
    val rolesFailed: String
    val rolesRetry: String
    val rolesEmpty: String
    val rolesPartner: String
    val rolesAffiliate: String
    val rolesOpenWeb: String
    val akaTitle: String
    val nameColor: String
    val archiveLabel: String
    val archiveSearch: String
    val monthsShort: List<String>
    val ageUnits: List<String>
    val thousandsSeparator: String
    fun rolesSince(date: String): String
    fun rolesGranted(date: String): String
    fun rolesAccountCreated(date: String): String
    fun rolesFollowers(count: Int, formatted: String): String
    fun rolesChannels(count: Int): String
    fun akaSeen(from: String, to: String): String
}

object ProfileInsightStringsEn : ProfileInsightStrings {
    override val tabRoles = "Roles"
    override val roleModerators = "Mods"
    override val roleVips = "VIPs"
    override val roleFounders = "Founders"
    override val roleArtists = "Artists"
    override val rolesFailed = "Couldn't load roles"
    override val rolesRetry = "Retry"
    override val rolesEmpty = "No roles on other channels"
    override val rolesPartner = "Partner"
    override val rolesAffiliate = "Affiliate"
    override val rolesOpenWeb = "Open on roles.tv"
    override val akaTitle = "Previous names"
    override val nameColor = "Name color, click to copy"
    override val archiveLabel = "logs"
    override val archiveSearch = "Search messages"
    override val thousandsSeparator = ","
    override val monthsShort = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    override val ageUnits = listOf("y", "mo", "d", "h", "m")
    override fun rolesSince(date: String) = "since $date"
    override fun rolesGranted(date: String) = "Role since $date"
    override fun rolesAccountCreated(date: String) = "Account created $date"
    override fun rolesFollowers(count: Int, formatted: String) = "$formatted ${if (count == 1) "follower" else "followers"}"
    override fun rolesChannels(count: Int) = "$count ${if (count == 1) "channel" else "channels"}"
    override fun akaSeen(from: String, to: String) = "$from – $to"
}

object ProfileInsightStringsRu : ProfileInsightStrings {
    override val tabRoles = "Роли"
    override val roleModerators = "Модеры"
    override val roleVips = "VIP"
    override val roleFounders = "Фаундеры"
    override val roleArtists = "Артисты"
    override val rolesFailed = "Не удалось загрузить роли"
    override val rolesRetry = "Повторить"
    override val rolesEmpty = "Ролей на других каналах нет"
    override val rolesPartner = "Партнёр"
    override val rolesAffiliate = "Компаньон"
    override val rolesOpenWeb = "Открыть на roles.tv"
    override val akaTitle = "Прошлые ники"
    override val nameColor = "Цвет ника, нажми чтобы скопировать"
    override val archiveLabel = "архив"
    override val archiveSearch = "Поиск по сообщениям"
    override val thousandsSeparator = "\u00A0"
    override val monthsShort = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    override val ageUnits = listOf("г", "мес", "д", "ч", "м")
    override fun rolesSince(date: String) = "с $date"
    override fun rolesGranted(date: String) = "Роль с $date"
    override fun rolesAccountCreated(date: String) = "Аккаунт создан $date"
    override fun rolesFollowers(count: Int, formatted: String) =
        "$formatted ${plural(count, "фолловер", "фолловера", "фолловеров")}"
    override fun rolesChannels(count: Int) = "$count ${plural(count, "канал", "канала", "каналов")}"
    override fun akaSeen(from: String, to: String) = "$from – $to"

    private fun plural(count: Int, one: String, few: String, many: String): String {
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
