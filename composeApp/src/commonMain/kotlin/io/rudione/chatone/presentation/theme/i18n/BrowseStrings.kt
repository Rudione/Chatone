package io.rudione.chatone.presentation.theme.i18n

interface BrowseStrings {
    val title: String
    val entryTitle: String
    val entrySubtitle: String
    val searchPlaceholder: String
    val clearSearch: String
    val popularCategories: String
    val tabFollowing: String
    val tabPopular: String
    val tabCategories: String
    val followEmpty: String
    val followSignIn: String
    val followReconnect: String
    val channelsHeader: String
    val categoriesHeader: String
    val nothingFound: String
    val loadFailed: String
    val retry: String
    val live: String
    val offline: String
    val sortLabel: String
    val sortViewersHigh: String
    val sortViewersLow: String
    val sortRecent: String
    val languageLabel: String
    val allLanguages: String
    val tagsLabel: String
    val noStreams: String
    val resetFilters: String
    val recentHeader: String
    val keepChannel: String
    val forgetChannel: String
    val back: String
    val thousandSuffix: String
    val millionSuffix: String
    val decimalSeparator: String
    val askAi: String
    val filters: String
    val minViewersLabel: String
    val anyViewers: String
    val hideMature: String
    val hideMatureDescription: String
    val applyFilters: String
    val customTagHint: String
    fun viewers(count: Int, compact: String): String
    fun aiStreamPrompt(streamer: String, title: String, category: String, tags: String, language: String, viewers: String, uptime: String): String
    fun channels(count: Int, compact: String): String
}

object BrowseStringsEn : BrowseStrings {
    override val title = "Browse"
    override val entryTitle = "Browse"
    override val entrySubtitle = "Top categories and streams"
    override val searchPlaceholder = "Find a category or channel"
    override val clearSearch = "Clear search"
    override val popularCategories = "Popular right now"
    override val tabFollowing = "Following"
    override val tabPopular = "Popular"
    override val tabCategories = "Categories"
    override val followEmpty = "None of the channels you follow are live right now"
    override val followSignIn = "Log in to see channels you follow"
    override val followReconnect = "Reconnect your account to see your follows"
    override val channelsHeader = "Channels"
    override val categoriesHeader = "Categories"
    override val nothingFound = "Nothing found"
    override val loadFailed = "Could not load. Check the connection"
    override val retry = "Retry"
    override val live = "LIVE"
    override val offline = "Offline"
    override val sortLabel = "Sort"
    override val sortViewersHigh = "Most viewers"
    override val sortViewersLow = "Fewest viewers"
    override val sortRecent = "Recently started"
    override val languageLabel = "Language"
    override val allLanguages = "All"
    override val tagsLabel = "Tags"
    override val noStreams = "No streams match these filters"
    override val resetFilters = "Reset filters"
    override val recentHeader = "Recently opened"
    override val keepChannel = "Add to my channels"
    override val forgetChannel = "Remove from recent"
    override val back = "Back"
    override val thousandSuffix = "K"
    override val millionSuffix = "M"
    override val decimalSeparator = "."
    override val askAi = "Ask AI about this stream"
    override val filters = "Filters"
    override val minViewersLabel = "Minimum viewers"
    override val anyViewers = "Any"
    override val hideMature = "Hide 18+"
    override val hideMatureDescription = "Skip streams marked as mature"
    override val applyFilters = "Show streams"
    override val customTagHint = "Your tag, e.g. English"
    override fun viewers(count: Int, compact: String) = if (count == 1) "1 viewer" else "$compact viewers"
    override fun channels(count: Int, compact: String) = if (count == 1) "1 channel" else "$compact channels"
    override fun aiStreamPrompt(streamer: String, title: String, category: String, tags: String, language: String, viewers: String, uptime: String) =
        "In 3-4 sentences, tell me what $streamer's live stream is about right now and whether it's worth joining. " +
            "Title: \"$title\". Category: $category. Tags: $tags. Language: $language. Viewers: $viewers. Live for: $uptime. " +
            "Rely only on these details and say so if they are not enough."
}

object BrowseStringsRu : BrowseStrings {
    override val title = "Обзор"
    override val entryTitle = "Обзор"
    override val entrySubtitle = "Топ категорий и стримов"
    override val searchPlaceholder = "Найти категорию или канал"
    override val clearSearch = "Очистить поиск"
    override val popularCategories = "Популярно сейчас"
    override val tabFollowing = "Фолловнут"
    override val tabPopular = "Популярное"
    override val tabCategories = "Категории"
    override val followEmpty = "Никто из отслеживаемых каналов сейчас не в эфире"
    override val followSignIn = "Войдите, чтобы видеть отслеживаемые каналы"
    override val followReconnect = "Переподключите аккаунт, чтобы видеть отслеживаемые"
    override val channelsHeader = "Каналы"
    override val categoriesHeader = "Категории"
    override val nothingFound = "Ничего не найдено"
    override val loadFailed = "Не удалось загрузить. Проверьте соединение"
    override val retry = "Повторить"
    override val live = "В ЭФИРЕ"
    override val offline = "Не в сети"
    override val sortLabel = "Сортировка"
    override val sortViewersHigh = "Больше зрителей"
    override val sortViewersLow = "Меньше зрителей"
    override val sortRecent = "Недавно начали"
    override val languageLabel = "Язык"
    override val allLanguages = "Все"
    override val tagsLabel = "Теги"
    override val noStreams = "Нет стримов с такими фильтрами"
    override val resetFilters = "Сбросить фильтры"
    override val recentHeader = "Недавно открытые"
    override val keepChannel = "Добавить в мои каналы"
    override val forgetChannel = "Убрать из недавних"
    override val back = "Назад"
    override val thousandSuffix = " тыс."
    override val millionSuffix = " млн"
    override val decimalSeparator = ","
    override val askAi = "Спросить ИИ о стриме"
    override val filters = "Фильтры"
    override val minViewersLabel = "Минимум зрителей"
    override val anyViewers = "Любое"
    override val hideMature = "Скрыть 18+"
    override val hideMatureDescription = "Не показывать стримы с пометкой для взрослых"
    override val applyFilters = "Показать стримы"
    override val customTagHint = "Свой тег, например Русский"
    override fun viewers(count: Int, compact: String) = "$compact ${plural(count, "зритель", "зрителя", "зрителей")}"
    override fun channels(count: Int, compact: String) = "$compact ${plural(count, "канал", "канала", "каналов")}"
    override fun aiStreamPrompt(streamer: String, title: String, category: String, tags: String, language: String, viewers: String, uptime: String) =
        "Кратко, в 3–4 предложениях, расскажи, про что сейчас стрим $streamer и стоит ли заходить. " +
            "Название: «$title». Категория: $category. Теги: $tags. Язык: $language. Зрителей: $viewers. В эфире: $uptime. " +
            "Опирайся только на эти данные и честно скажи, если их мало."

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
