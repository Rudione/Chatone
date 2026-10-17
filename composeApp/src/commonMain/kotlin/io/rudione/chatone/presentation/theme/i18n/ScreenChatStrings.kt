package io.rudione.chatone.presentation.theme.i18n

interface ScreenChatStrings {
    val menuOpen: String
    val menuClose: String
    val menuPopoutChat: String
    val menuCopyLink: String
    val windowTitle: String
    val lock: String
    val unlock: String
    val close: String
    val dragHint: String
    val waiting: String
    val settingsTitle: String
    val settingsHelp: String
    val layerTitle: String
    val layerHelp: String
    val layerAll: String
    val layerApp: String
    val layerNormal: String
    val backgroundTitle: String
    val backdropTitle: String
    val textTitle: String
    val scaleTitle: String
    val hideDelayTitle: String
    val hideDelayValue: String
    val lockedTitle: String
    val lockedHelp: String
    val settings: String
    val settingsWindowTitle: String
    val groupWindow: String
    val groupLook: String
    val groupMessages: String
    val overFullScreenTitle: String
    val overFullScreenHelp: String
    val backdropHelp: String
    val backdropOpacityTitle: String
    val fontSizeTitle: String
    val moderationTitle: String
    val moderationHelp: String
    val replyAboveTitle: String
    val replyAboveHelp: String
    val keepDockTitle: String
    val keepDockHelp: String
    val openApp: String
}

object ScreenChatStringsEn : ScreenChatStrings {
    override val menuOpen = "Screen chat"
    override val menuClose = "Close screen chat"
    override val menuPopoutChat = "Open chat in browser"
    override val menuCopyLink = "Copy channel link"
    override val windowTitle = "Screen chat · #{0}"
    override val lock = "Pin in place"
    override val unlock = "Unpin"
    override val close = "Close"
    override val dragHint = "Drag to move"
    override val waiting = "Waiting for messages…"
    override val settingsTitle = "Screen chat"
    override val settingsHelp = "A separate see-through window with the chat that you can put over a game, a browser or your stream. Open it from the channel menu: ⋮ in the sidebar or a right click on the chat top bar. A few seconds after the mouse stops, only the messages stay. Ctrl + mouse wheel over the window changes the scale, the gear in its panel opens these settings."
    override val layerTitle = "Window layer"
    override val layerHelp = "Over Chatone only: the window stays on top while you are in Chatone (the player included) and steps back when you switch to another app."
    override val layerAll = "Over all windows"
    override val layerApp = "Over Chatone only"
    override val layerNormal = "Like a normal window"
    override val backgroundTitle = "Background on hover"
    override val backdropTitle = "Backdrop under messages"
    override val textTitle = "Message opacity"
    override val scaleTitle = "Scale"
    override val hideDelayTitle = "Hide the panel after"
    override val hideDelayValue = "{0} s"
    override val lockedTitle = "Pin windows in place"
    override val lockedHelp = "The windows can't be moved or resized by accident. The panel still shows on hover so you can unpin."
    override val settings = "Screen chat settings"
    override val settingsWindowTitle = "Screen chat settings"
    override val groupWindow = "Window"
    override val groupLook = "Look"
    override val groupMessages = "Messages"
    override val overFullScreenTitle = "Over full-screen apps"
    override val overFullScreenHelp = "Keeps the chat visible over a game or a browser in full screen. macOS allows this only for apps without a Dock icon, so by default the Chatone icon leaves the Dock and Cmd+Tab while a screen chat is open. You can always get back through the button on the chat panel or the Chatone icon in the menu bar."
    override val backdropHelp = "A dark plate under every message. Without it only highlights stay: mentions, first messages, rewards."
    override val backdropOpacityTitle = "Backdrop density"
    override val fontSizeTitle = "Font size"
    override val moderationTitle = "Moderator actions"
    override val moderationHelp = "Who timed out or banned whom and which messages were deleted."
    override val replyAboveTitle = "Reply on its own line"
    override val replyAboveHelp = "Shows the message that was answered above the reply. When off, the reply starts with @nickname instead, which saves space."
    override val keepDockTitle = "Keep the Dock icon"
    override val keepDockHelp = "The chat window stops activating Chatone when clicked, like the floating windows of call apps, so macOS can show it over full screen without hiding the Dock icon. If the chat disappears in full screen with this on, turn it off."
    override val openApp = "Open Chatone"
}

object ScreenChatStringsRu : ScreenChatStrings {
    override val menuOpen = "Экранный чат"
    override val menuClose = "Закрыть экранный чат"
    override val menuPopoutChat = "Открыть чат в браузере"
    override val menuCopyLink = "Скопировать ссылку на канал"
    override val windowTitle = "Экранный чат · #{0}"
    override val lock = "Закрепить"
    override val unlock = "Открепить"
    override val close = "Закрыть"
    override val dragHint = "Потяните, чтобы переместить"
    override val waiting = "Ждём сообщения…"
    override val settingsTitle = "Экранный чат"
    override val settingsHelp = "Отдельное прозрачное окно с чатом, которое можно поставить поверх игры, браузера или стрима. Открывается из меню канала: ⋮ в боковой панели или правый клик по верхней панели чата. Через пару секунд без движения мыши остаются только сообщения. Ctrl + колёсико над окном меняет масштаб, шестерёнка на его панели открывает эти настройки."
    override val layerTitle = "Слой окна"
    override val layerHelp = "Только поверх Chatone: окно поверх, пока вы в Chatone (включая плеер), и уходит назад, когда переключаетесь в другое приложение."
    override val layerAll = "Поверх всех окон"
    override val layerApp = "Только поверх Chatone"
    override val layerNormal = "Как обычное окно"
    override val backgroundTitle = "Фон при наведении"
    override val backdropTitle = "Подложка под сообщениями"
    override val textTitle = "Непрозрачность сообщений"
    override val scaleTitle = "Масштаб"
    override val hideDelayTitle = "Скрывать панель через"
    override val hideDelayValue = "{0} с"
    override val lockedTitle = "Закрепить окна"
    override val lockedHelp = "Окна нельзя случайно сдвинуть или растянуть. Панель при наведении остаётся, чтобы можно было открепить."
    override val settings = "Настройки экранного чата"
    override val settingsWindowTitle = "Настройки экранного чата"
    override val groupWindow = "Окно"
    override val groupLook = "Вид"
    override val groupMessages = "Сообщения"
    override val overFullScreenTitle = "Поверх полноэкранных окон"
    override val overFullScreenHelp = "Чат остаётся виден над игрой или браузером на весь экран. macOS разрешает это только приложениям без значка в Dock, поэтому по умолчанию, пока открыт экранный чат, значка Chatone нет в Dock и в Cmd+Tab. Вернуться в приложение можно кнопкой на панели чата или через значок Chatone в строке меню."
    override val backdropHelp = "Тёмная плашка под каждым сообщением. Без неё остаётся только подсветка: упоминания, первые сообщения, награды."
    override val backdropOpacityTitle = "Плотность подложки"
    override val fontSizeTitle = "Размер шрифта"
    override val moderationTitle = "Действия модераторов"
    override val moderationHelp = "Кто кого замутил или забанил и чьи сообщения удалили."
    override val replyAboveTitle = "Ответ отдельной строкой"
    override val replyAboveHelp = "Над ответом показывается сообщение, на которое ответили. Если выключить, вместо этой строки ответ начинается с @ника — так чат занимает меньше места."
    override val keepDockTitle = "Оставлять значок в Dock"
    override val keepDockHelp = "Клик по окну чата перестаёт переключать на Chatone, как у плавающих окон в приложениях для звонков, и macOS может показывать его поверх полноэкранных окон, не убирая значок из Dock. Если с этим режимом чат пропал в полноэкранном режиме, выключите его."
    override val openApp = "Открыть Chatone"
}
