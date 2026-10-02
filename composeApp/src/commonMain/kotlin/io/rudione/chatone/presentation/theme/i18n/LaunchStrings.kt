package io.rudione.chatone.presentation.theme.i18n

import io.rudione.chatone.presentation.startup.LaunchStage

interface LaunchStrings {
    fun stage(stage: LaunchStage): String
}

object LaunchStringsEn : LaunchStrings {
    override fun stage(stage: LaunchStage): String = when (stage) {
        LaunchStage.Starting -> "Starting up"
        LaunchStage.Connecting -> "Connecting to chat"
        LaunchStage.Emotes -> "Loading emotes and badges"
        LaunchStage.History -> "Fetching chat history"
        LaunchStage.Rendering, LaunchStage.Ready -> "Almost there"
    }
}

object LaunchStringsRu : LaunchStrings {
    override fun stage(stage: LaunchStage): String = when (stage) {
        LaunchStage.Starting -> "Запускаемся"
        LaunchStage.Connecting -> "Подключаемся к чату"
        LaunchStage.Emotes -> "Загружаем эмоуты и значки"
        LaunchStage.History -> "Подтягиваем историю чата"
        LaunchStage.Rendering, LaunchStage.Ready -> "Почти готово"
    }
}
