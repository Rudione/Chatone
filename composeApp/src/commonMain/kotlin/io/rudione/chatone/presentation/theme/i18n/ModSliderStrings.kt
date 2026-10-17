package io.rudione.chatone.presentation.theme.i18n

interface ModSliderStrings {
    val enableTitle: String
    val enableHelp: String
    val statusOn: String
    val statusOff: String
    val tabGeneral: String
    val tabButtons: String
    val tabSlider: String
    val tabMacros: String
    val scaleTitle: String
    val twitchScale: String
    val twitchScaleHelp: String
    val stepsTitle: String
    val stepsHelp: String
    val stepsOrder: String
    val addStep: String
    val quickSteps: String
    val ownStep: String
    val resetSteps: String
    val deleteAtStart: String
    val deleteAtStartHelp: String
    val banAtEnd: String
    val banAtEndHelp: String
    val travelTitle: String
    val travelHelp: String
    val travelValue: String
    val previewTitle: String
    val previewHint: String
    val previewUser: String
    val previewMessage: String
    val previewResult: String
    val grip: String
    val actionDelete: String
    val actionTimeout: String
    val actionBan: String
    val actionUnban: String
    val buttonsInSliderMode: String
    val buttonsInChat: String
    val buttonsInChatHelp: String
    val buttonsInChatOff: String
}

object ModSliderStringsEn : ModSliderStrings {
    override val enableTitle = "Moderation slider"
    override val enableHelp = "Drag the handle on the left of a message to the right: a little deletes, further times out, all the way bans. Drag left to unban. You can also grab the time. Esc or right click cancels. Mod mode turns on in every channel; to turn it off in one channel, click the sword."
    override val statusOn = "On — the handle shows next to messages in channels where you are a moderator"
    override val statusOff = "Off — moderation buttons are used"
    override val tabGeneral = "General"
    override val tabButtons = "Buttons"
    override val tabSlider = "Slider"
    override val tabMacros = "Macros"
    override val scaleTitle = "Timeout scale"
    override val twitchScale = "Like on Twitch"
    override val twitchScaleHelp = "Seconds 1–59, minutes 1–59, hours 1–23, days 1–14. Turn it off to set your own steps."
    override val stepsTitle = "Your timeout steps"
    override val stepsHelp = "The slider goes through them from left to right. At first they copy the timeout buttons."
    override val stepsOrder = "On the slider"
    override val addStep = "Add"
    override val quickSteps = "Quick steps"
    override val ownStep = "Own step"
    override val resetSteps = "Copy from buttons"
    override val deleteAtStart = "Delete at the start"
    override val deleteAtStartHelp = "A short pull deletes only this message."
    override val banAtEnd = "Ban at the end"
    override val banAtEndHelp = "The very end of the slider bans the user."
    override val travelTitle = "Slider travel"
    override val travelHelp = "Which part of the message width you pull to reach the end."
    override val travelValue = "{0} %"
    override val previewTitle = "Try it"
    override val previewHint = "Nothing is sent from here"
    override val previewUser = "viewer"
    override val previewMessage = "pull my handle to the right"
    override val previewResult = "Would do: {0}"
    override val grip = "Pull to moderate"
    override val actionDelete = "Delete"
    override val actionTimeout = "Timeout {0}"
    override val actionBan = "Ban"
    override val actionUnban = "Unban"
    override val buttonsInSliderMode = "The slider handle replaces delete, timeout and ban. Your own buttons stay before the nickname."
    override val buttonsInChat = "Buttons in chat"
    override val buttonsInChatHelp = "Delete, timeout, ban and your own buttons next to messages. The user card keeps its moderation actions either way."
    override val buttonsInChatOff = "Hidden in chat — still available in the user card"
}

object ModSliderStringsRu : ModSliderStrings {
    override val enableTitle = "Слайдер модерации"
    override val enableHelp = "Потяните ручку слева у сообщения вправо: чуть-чуть — удалить, дальше — мут, до конца — бан. Влево — разбан. Хвататься можно и за время. Отмена — Esc или правая кнопка мыши. Режим модератора включится на всех каналах, выключить на канале — нажмите на меч."
    override val statusOn = "Включён — ручка видна у сообщений на каналах, где вы модератор"
    override val statusOff = "Выключен — работают кнопки модерации"
    override val tabGeneral = "Общее"
    override val tabButtons = "Кнопки"
    override val tabSlider = "Слайдер"
    override val tabMacros = "Макросы"
    override val scaleTitle = "Шкала мута"
    override val twitchScale = "Как на Twitch"
    override val twitchScaleHelp = "Секунды 1–59, минуты 1–59, часы 1–23, дни 1–14. Выключите, чтобы задать свои сроки."
    override val stepsTitle = "Свои сроки мута"
    override val stepsHelp = "Слайдер проходит их слева направо. Сначала они берутся из кнопок таймаута."
    override val stepsOrder = "На слайдере"
    override val addStep = "Добавить"
    override val quickSteps = "Быстрые сроки"
    override val ownStep = "Свой срок"
    override val resetSteps = "Как в кнопках"
    override val deleteAtStart = "Удаление в начале"
    override val deleteAtStartHelp = "Если потянуть чуть-чуть, удалится только это сообщение."
    override val banAtEnd = "Бан в конце"
    override val banAtEndHelp = "Самый конец слайдера банит пользователя."
    override val travelTitle = "Ход слайдера"
    override val travelHelp = "Какую часть ширины сообщения нужно протянуть до конца."
    override val travelValue = "{0} %"
    override val previewTitle = "Попробуйте"
    override val previewHint = "Отсюда ничего не отправится"
    override val previewUser = "зритель"
    override val previewMessage = "потяните мою ручку вправо"
    override val previewResult = "Сработало бы: {0}"
    override val grip = "Потяните, чтобы модерировать"
    override val actionDelete = "Удалить"
    override val actionTimeout = "Мут {0}"
    override val actionBan = "Бан"
    override val actionUnban = "Разбан"
    override val buttonsInSliderMode = "Удаление, мут и бан заменяет ручка слайдера. Свои кнопки остаются перед ником."
    override val buttonsInChat = "Кнопки в чате"
    override val buttonsInChatHelp = "Удаление, мут, бан и свои кнопки у сообщений. В мини-профиле действия модерации остаются в любом случае."
    override val buttonsInChatOff = "Скрыты в чате — остаются в мини-профиле"
}
