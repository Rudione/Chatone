package io.rudione.chatone.presentation.settings

import androidx.compose.ui.graphics.vector.ImageVector
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.CircleUser
import io.rudione.chatone.icons.lucide.Highlighter
import io.rudione.chatone.icons.lucide.Info
import io.rudione.chatone.icons.lucide.Keyboard
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare
import io.rudione.chatone.icons.lucide.Palette
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.SquareSlash
import io.rudione.chatone.icons.lucide.Swords
import io.rudione.chatone.icons.lucide.Wallpaper
import io.rudione.chatone.presentation.theme.i18n.AppStrings

internal enum class SettingsSection(
    val label: String,
    val icon: ImageVector
) {
    APPEARANCE("Appearance", Lucide.Palette),
    CHAT("Chat", Lucide.MessagesSquare),
    NOTIFICATIONS("Notifications", Lucide.Bell),
    HIGHLIGHTS("Highlights", Lucide.Highlighter),
    BACKGROUND("Background", Lucide.Wallpaper),
    HOTKEYS("Hotkeys", Lucide.Keyboard),
    COMMANDS("Commands", Lucide.SquareSlash),
    ACTIONS("Actions", Lucide.Swords),
    MODERATION("Moderation", Lucide.Shield),
    AI("AI Assistant", Lucide.Sparkles),
    ACCOUNT("Account", Lucide.CircleUser),
    ABOUT("About", Lucide.Info);

    fun localizedLabel(s: AppStrings): String = when (this) {
        APPEARANCE -> s.settingsAppearance
        CHAT -> s.sectionChat
        NOTIFICATIONS -> s.settingsNotifications
        HIGHLIGHTS -> s.sectionHighlights
        BACKGROUND -> s.sectionBackground
        HOTKEYS -> s.sectionHotkeys
        COMMANDS -> s.sectionCommands
        ACTIONS -> s.sectionActions
        MODERATION -> s.settingsModeration
        AI -> s.aiAssistantTitle
        ACCOUNT -> s.settingsAccount
        ABOUT -> s.settingsAbout
    }
}

internal fun SettingsDestination.section(): SettingsSection = when (this) {
    is SettingsDestination.EditMacro -> SettingsSection.MODERATION
}

private val SECTION_KEYWORDS: Map<SettingsSection, List<String>> = mapOf(
    SettingsSection.APPEARANCE to listOf(
        "appearance",
        "theme",
        "color",
        "dark",
        "light",
        "font",
        "size",
        "scale",
        "ui",
        "ui scale",
        "font size",
        "font family",
        "font style",
        "title bar",
        "titlebar",
        "language",
        "locale",
        "compact",
        "density",
        "expressive",
        "palette",
        "accent",
        "corner radius",
        "rounding",
        "elevation",
        "transparency",
        "opacity",
        "glass",
        "blur",
        "sidebar",
        "menu",
        "animation",
        "ripple",
        "high contrast",
        "внешний вид",
        "тема",
        "цвет",
        "темная",
        "светлая",
        "шрифт",
        "размер",
        "масштаб",
        "масштаб интерфейса",
        "размер шрифта",
        "семейство шрифтов",
        "стиль шрифта",
        "заголовок окна",
        "язык",
        "локаль",
        "компактный",
        "плотность",
        "палитра",
        "акцент",
        "скругление",
        "тень",
        "прозрачность",
        "стекло",
        "размытие",
        "боковая панель",
        "боковую панель",
        "скрывать боковую панель",
        "hide sidebar",
        "компактный режим",
        "анимация",
        "контраст"
    ),
    SettingsSection.CHAT to listOf(
        "chat",
        "message",
        "timestamp",
        "badge",
        "emote",
        "scroll",
        "deleted",
        "message density",
        "line spacing",
        "username color",
        "readable colors",
        "compact mode",
        "alternate background",
        "stripes",
        "inline images",
        "link preview",
        "image height",
        "image preview",
        "blur images",
        "clip",
        "clip preview",
        "clip preview size",
        "scrollbar",
        "scrollbar width",
        "emoji",
        "autocomplete",
        "command suggestions",
        "translate",
        "mentions",
        "pause",
        "pause hotkey",
        "auto scroll",
        "channel points",
        "warning",
        "raid",
        "raid countdown",
        "show timestamps",
        "12h",
        "24h",
        "deleted messages",
        "removed messages",
        "moderator actions",
        "show moderator",
        "spoof",
        "send as web",
        "platform badge",
        "чат",
        "сообщение",
        "время",
        "бейдж",
        "эмоут",
        "скролл",
        "удаленные",
        "плотность сообщений",
        "межстрочный интервал",
        "цвет имени",
        "читаемые цвета",
        "компактный режим",
        "чередование фона",
        "встроенные изображения",
        "превью ссылок",
        "высота изображения",
        "размытие изображений",
        "клип",
        "превью клипа",
        "размер превью клипа",
        "полоса прокрутки",
        "ширина полосы прокрутки",
        "эмодзи",
        "автозаполнение",
        "подсказки команд",
        "перевод",
        "упоминания",
        "пауза",
        "автоскролл",
        "поинты канала",
        "предупреждения",
        "рейд",
        "обратный отсчет рейда",
        "показывать время",
        "удаленные сообщения",
        "действия модератора"
    ),
    SettingsSection.NOTIFICATIONS to listOf(
        "notification", "sound", "mention", "alert", "mute", "ping",
        "volume", "notification sound", "custom sound", "mention sound",
        "system notifications", "tray", "tray notifications", "live notifications",
        "follow notifications", "do not disturb", "dnd", "quiet hours",
        "уведомление", "звук", "упоминание", "тишина", "пинг",
        "громкость", "звук уведомлений", "свой звук", "звук упоминаний",
        "системные уведомления", "трей", "уведомления в трее", "лайв уведомления",
        "уведомления подписки", "не беспокоить", "тихие часы"
    ),
    SettingsSection.HIGHLIGHTS to listOf(
        "highlight", "keyword", "rule", "color", "regex",
        "highlight color", "highlight rule", "add highlight", "case sensitive",
        "whole word", "background color", "blink", "sound on highlight",
        "хайлайт", "ключевое слово", "правило", "цвет", "регулярка",
        "цвет хайлайта", "правило хайлайта", "добавить хайлайт", "регистр",
        "целое слово", "фоновый цвет", "мигание", "звук при хайлайте"
    ),
    SettingsSection.BACKGROUND to listOf(
        "background", "wallpaper", "image", "blur", "wallpaper opacity",
        "blur amount", "background color", "tint", "noise", "video wallpaper",
        "gradient", "fit", "cover", "stretch",
        "фон", "обои", "картинка", "размытие", "прозрачность обоев",
        "степень размытия", "цвет фона", "оттенок", "шум", "видео обои",
        "градиент", "вписать", "растянуть"
    ),
    SettingsSection.HOTKEYS to listOf(
        "hotkey", "shortcut", "keyboard", "keybind", "binding",
        "pause hotkey", "send message", "open settings", "switch panel",
        "next panel", "previous panel", "reset hotkey",
        "горячие клавиши", "сочетание", "клавиатура", "клавиша", "привязка",
        "клавиша паузы", "отправка сообщения", "открыть настройки", "переключение панели",
        "следующая панель", "предыдущая панель", "сбросить клавишу"
    ),
    SettingsSection.COMMANDS to listOf(
        "command", "commands", "trigger", "alias", "shortcut", "replace", "expand",
        "auto reply", "phrase", "abbreviation", "expander",
        "команда", "команды", "триггер", "алиас", "сокращение", "подмена", "замена",
        "автоответ", "фраза", "расширение"
    ),
    SettingsSection.ACTIONS to listOf(
        "action", "actions", "automation", "timer", "timed message", "auto reply",
        "keyword", "sound alert", "auto points", "bonus", "claim", "mute", "ignore",
        "действия", "автоматизация", "таймер", "автоответчик", "ключевое слово",
        "звук", "баллы", "бонус", "мьют", "игнор", "фраза", "скрытые фразы", "muted phrases"
    ),
    SettingsSection.MODERATION to listOf(
        "moderation", "mod", "ban", "timeout", "automod", "macro",
        "default timeout", "timeout duration", "ban reason", "custom reason",
        "mod actions", "mod buttons", "moderator buttons", "macros", "custom macros",
        "local automod", "chat rules", "rule trigger", "saved reasons",
        "repeated message", "nuke", "blocked term", "blockterm", "bot badge",
        "модерация", "мод", "бан", "таймаут", "автомод", "макрос",
        "стандартный таймаут", "длительность таймаута", "причина бана", "своя причина",
        "действия модератора", "кнопки мода", "макросы", "локальный автомод",
        "правила чата", "сохраненные причины", "повторные сообщения", "ньюк",
        "блок слова", "бот бейдж"
    ),
    SettingsSection.AI to listOf(
        "ai", "assistant", "gpt", "llm", "model", "neural", "ollama", "g4f",
        "lm studio", "auto mod scan", "ai automod", "endpoint", "base url",
        "ии", "ассистент", "нейросеть", "модель", "нейро", "искусственный интеллект",
        "ии автомод", "сканер"
    ),
    SettingsSection.ACCOUNT to listOf(
        "account", "login", "token", "auth", "profile", "proxy",
        "add account", "remove account", "primary account", "logout", "clear cache",
        "switch account", "multi account", "account proxy", "blocked users",
        "blocked", "block list", "ignore", "ignored", "unblock",
        "first party", "first-party", "device auth", "gql token", "integrity",
        "аккаунт", "логин", "токен", "авторизация", "профиль", "прокси",
        "добавить аккаунт", "удалить аккаунт", "основной аккаунт", "выйти", "очистить кеш",
        "переключить аккаунт", "несколько аккаунтов", "прокси аккаунта",
        "заблокированные", "блок лист", "игнор", "игнорированные", "разблокировать"
    ),
    SettingsSection.ABOUT to listOf(
        "about", "version", "info", "backup", "export", "import", "restore",
        "changelog", "license", "credits", "donate", "support", "github",
        "settings backup", "settings export", "settings import",
        "о программе", "версия", "информация", "бекап", "экспорт", "импорт",
        "восстановить", "история изменений", "лицензия", "поддержка",
        "бекап настроек", "экспорт настроек", "импорт настроек"
    )
)

internal fun settingsSectionsMatching(query: String, strings: AppStrings): List<SettingsSection> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return SettingsSection.entries.toList()
    return SettingsSection.entries.filter { section ->
        section.label.lowercase().contains(q) ||
            section.localizedLabel(strings).lowercase().contains(q) ||
            SECTION_KEYWORDS[section].orEmpty().any { it.contains(q) }
    }
}
