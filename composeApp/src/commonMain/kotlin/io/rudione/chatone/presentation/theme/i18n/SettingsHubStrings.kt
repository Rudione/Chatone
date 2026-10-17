package io.rudione.chatone.presentation.theme.i18n

interface SettingsHubStrings {
    val sectionMedia: String
    val groupInterface: String
    val groupMessageLook: String
    val groupLinksMedia: String
    val groupChatPanel: String
    val groupPrivacy: String
    val groupMouse: String
    val groupRowDecor: String
    val uploaderServiceGroup: String
    val uploaderBehaviourGroup: String
    val uploaderLinkHelp: String
    val uploaderFieldsTitle: String
    val uploaderFieldsHelp: String
    val uploaderFieldName: String
    val uploaderFieldValue: String
    val uploaderAddField: String
    val uploaderRemoveField: String
    val uploaderFieldsEmpty: String
    val highlightBuiltIn: String
    val highlightCustom: String
    val highlightCustomEmpty: String
    val highlightSound: String
    val highlightRemove: String
    val searchFound: String
    val searchBackToResults: String
    val searchTryAnother: String
    val searchKeyboardHint: String
    val searchOpen: String
}

object SettingsHubStringsEn : SettingsHubStrings {
    override val sectionMedia = "Images"
    override val groupInterface = "Interface"
    override val groupMessageLook = "Message look"
    override val groupLinksMedia = "Links and media"
    override val groupChatPanel = "Chat panel"
    override val groupPrivacy = "Privacy"
    override val groupMouse = "Mouse"
    override val groupRowDecor = "Row details"
    override val uploaderServiceGroup = "Upload service"
    override val uploaderBehaviourGroup = "Image upload"
    override val uploaderLinkHelp = "{url} is the whole response. For JSON use a path, for example {data.link} or {files.0.url}."
    override val uploaderFieldsTitle = "Custom form fields"
    override val uploaderFieldsHelp = "Sent together with the file. For example key = your API key, or reqtype = fileupload for catbox.moe. Values are stored encrypted."
    override val uploaderFieldName = "Name"
    override val uploaderFieldValue = "Value"
    override val uploaderAddField = "Add field"
    override val uploaderRemoveField = "Remove field"
    override val uploaderFieldsEmpty = "No custom fields"
    override val highlightBuiltIn = "Built-in"
    override val highlightCustom = "Your rules"
    override val highlightCustomEmpty = "No rules yet. Add a word or a regex below."
    override val highlightSound = "Sound"
    override val highlightRemove = "Remove rule"
    override val searchFound = "Found: {0}"
    override val searchBackToResults = "Back to results"
    override val searchTryAnother = "Try another word — typos and the wrong keyboard layout are fine"
    override val searchKeyboardHint = "↑ ↓ to choose, Enter to open"
    override val searchOpen = "Open"
}

object SettingsHubStringsRu : SettingsHubStrings {
    override val sectionMedia = "Изображения"
    override val groupInterface = "Интерфейс"
    override val groupMessageLook = "Вид сообщений"
    override val groupLinksMedia = "Ссылки и медиа"
    override val groupChatPanel = "Панель чата"
    override val groupPrivacy = "Приватность"
    override val groupMouse = "Мышь"
    override val groupRowDecor = "Оформление строк"
    override val uploaderServiceGroup = "Сервис загрузки"
    override val uploaderBehaviourGroup = "Загрузка картинок"
    override val uploaderLinkHelp = "{url} — весь ответ целиком. Для JSON укажите путь, например {data.link} или {files.0.url}."
    override val uploaderFieldsTitle = "Свои поля формы"
    override val uploaderFieldsHelp = "Отправляются вместе с файлом. Например key = ваш API-ключ или reqtype = fileupload для catbox.moe. Значения хранятся в зашифрованном виде."
    override val uploaderFieldName = "Имя"
    override val uploaderFieldValue = "Значение"
    override val uploaderAddField = "Добавить поле"
    override val uploaderRemoveField = "Удалить поле"
    override val uploaderFieldsEmpty = "Своих полей нет"
    override val highlightBuiltIn = "Встроенные"
    override val highlightCustom = "Свои правила"
    override val highlightCustomEmpty = "Своих правил пока нет. Добавьте слово или регулярку ниже."
    override val highlightSound = "Звук"
    override val highlightRemove = "Удалить правило"
    override val searchFound = "Найдено: {0}"
    override val searchBackToResults = "К результатам"
    override val searchTryAnother = "Попробуйте другое слово — опечатки и другая раскладка не страшны"
    override val searchKeyboardHint = "↑ ↓ — выбрать, Enter — открыть"
    override val searchOpen = "Открыть"
}
