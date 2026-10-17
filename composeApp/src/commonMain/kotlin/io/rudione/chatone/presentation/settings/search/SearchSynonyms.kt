package io.rudione.chatone.presentation.settings.search

internal object SearchSynonyms {

    private val groups: List<List<String>> = listOf(
        listOf("фон", "фоновый", "задний", "обои", "подложка", "background", "wallpaper", "backdrop", "bg"),
        listOf("цвет", "окраска", "оттенок", "расцветка", "палитра", "color", "colour", "palette", "tint"),
        listOf("тема", "оформление", "внешний", "дизайн", "стиль", "theme", "appearance", "style", "look"),
        listOf("шрифт", "буквы", "текст", "font", "typeface", "text"),
        listOf("размер", "величина", "масштаб", "крупнее", "мельче", "увеличить", "уменьшить", "size", "scale", "zoom", "bigger", "smaller"),
        listOf("звук", "аудио", "громкость", "sound", "audio", "volume"),
        listOf("уведомление", "оповещение", "пуш", "notification", "notify", "alert", "push"),
        listOf("упоминание", "пинг", "меншн", "mention", "ping"),
        listOf("подсветка", "выделение", "хайлайт", "highlight"),
        listOf("ник", "никнейм", "имя", "логин", "username", "nickname", "nick", "name"),
        listOf("скрыть", "спрятать", "убрать", "отключить", "выключить", "hide", "disable", "off"),
        listOf("показать", "отображать", "включить", "show", "display", "enable", "on"),
        listOf("язык", "локализация", "language", "locale", "lang"),
        listOf("перевод", "переводчик", "translate", "translation", "translator"),
        listOf("горячие", "клавиши", "хоткеи", "хоткей", "сочетание", "шорткат", "hotkey", "shortcut", "keybind", "keyboard"),
        listOf("бан", "блокировка", "ban"),
        listOf("таймаут", "мут", "заглушить", "timeout", "mute"),
        listOf("удалить", "удаление", "стереть", "delete", "remove"),
        listOf("эмоут", "смайл", "смайлик", "эмодзи", "emote", "emoji", "emoticon", "smiley"),
        listOf("картинка", "изображение", "фото", "скриншот", "image", "picture", "photo", "img", "screenshot"),
        listOf("гиф", "гифка", "gif", "giphy"),
        listOf("ссылка", "линк", "url", "link"),
        listOf("аккаунт", "учетка", "профиль", "account", "profile"),
        listOf("выход", "выйти", "разлогин", "logout", "signout"),
        listOf("прозрачность", "непрозрачность", "opacity", "transparency", "alpha"),
        listOf("время", "часы", "таймстамп", "time", "timestamp", "clock"),
        listOf("сайдбар", "боковая", "sidebar"),
        listOf("сообщение", "месседж", "message", "msg"),
        listOf("история", "лог", "history", "log", "scrollback"),
        listOf("модерация", "модер", "moderation", "mod", "moderator"),
        listOf("ии", "нейросеть", "ассистент", "ai", "assistant", "llm", "gpt"),
        listOf("стример", "streamer"),
        listOf("бекап", "бэкап", "резервная", "экспорт", "импорт", "backup", "export", "import"),
        listOf("кеш", "кэш", "cache"),
        listOf("версия", "обновление", "version", "update"),
        listOf("макрос", "macro"),
        listOf("команда", "алиас", "command", "alias"),
        listOf("значок", "бейдж", "иконка", "badge", "icon"),
        listOf("анимация", "плавный", "animation", "smooth"),
        listOf("прокрутка", "скролл", "скроллбар", "scroll", "scrollbar"),
        listOf("свечение", "сияние", "glow"),
        listOf("затемнение", "темнее", "dim", "darken"),
        listOf("оверлей", "поверх", "экранный", "overlay"),
        listOf("ввод", "поле", "инпут", "input", "field"),
        listOf("строка", "ряд", "row", "line"),
        listOf("интерфейс", "ui", "interface")
    )

    private val byKey: Map<String, List<String>> = buildMap<String, MutableSet<String>> {
        for (group in groups) {
            val words = group.map(SearchText::normalize).filter { it.isNotEmpty() && ' ' !in it }
            for (word in words) {
                getOrPut(word) { linkedSetOf() } += words
                getOrPut(SearchText.stem(word)) { linkedSetOf() } += words
            }
        }
    }.mapValues { (key, words) -> words.filter { it != key } }

    fun of(token: String): List<String> {
        val exact = byKey[token]
        if (exact != null) return exact
        return byKey[SearchText.stem(token)].orEmpty().filter { it != token }
    }
}
