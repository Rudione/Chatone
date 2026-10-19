package io.rudione.chatone.presentation.theme.i18n

interface AiAssistStrings {
    val errorOverloadedTitle: String
    val errorOverloadedHint: String
    val errorRateLimitedTitle: String
    val errorRateLimitedHint: String
    val errorKeyTitle: String
    val errorKeyHint: String
    val errorModelTitle: String
    val errorModelHint: String
    val errorModelLocalHint: String
    val errorRequestTitle: String
    val errorRequestHint: String
    val errorServerTitle: String
    val errorServerHint: String
    val errorNetworkTitle: String
    val errorNetworkHint: String
    val errorEmptyTitle: String
    val errorEmptyHint: String
    val retry: String
    val highlightActive: String
    val highlightStop: String
    val highlightResume: String
    val highlightStopped: String
}

object AiAssistStringsEn : AiAssistStrings {
    override val errorOverloadedTitle = "The model is overloaded"
    override val errorOverloadedHint = "{0} is getting too many requests right now, it usually passes in a minute. Try again or pick another model."
    override val errorRateLimitedTitle = "Too many requests"
    override val errorRateLimitedHint = "{0} has paused your key for a while. Wait a minute and try again."
    override val errorKeyTitle = "The key didn't work"
    override val errorKeyHint = "{0} rejected the API key. Check it in the assistant settings."
    override val errorModelTitle = "Model not found"
    override val errorModelHint = "{0} has no model {1}. Pick another one in the assistant settings."
    override val errorModelLocalHint = "Model {1} isn't installed. Run: ollama pull {1}"
    override val errorRequestTitle = "Request rejected"
    override val errorRequestHint = "{0} didn't accept the request. Try a shorter question or another model."
    override val errorServerTitle = "Service error"
    override val errorServerHint = "{0} answered with error {1}. Try again later."
    override val errorNetworkTitle = "No connection to {0}"
    override val errorNetworkHint = "Check your internet or VPN and try again."
    override val errorEmptyTitle = "Empty answer"
    override val errorEmptyHint = "The model returned nothing. Try again."
    override val retry = "Try again"
    override val highlightActive = "Highlighting in chat: {0}"
    override val highlightStop = "Stop"
    override val highlightResume = "Highlight again"
    override val highlightStopped = "Highlighting stopped"
}

object AiAssistStringsRu : AiAssistStrings {
    override val errorOverloadedTitle = "Модель перегружена"
    override val errorOverloadedHint = "У {0} сейчас слишком много запросов, обычно это проходит за минуту. Повторите или выберите другую модель."
    override val errorRateLimitedTitle = "Слишком много запросов"
    override val errorRateLimitedHint = "{0} временно ограничил ваш ключ. Подождите минуту и повторите."
    override val errorKeyTitle = "Ключ не подошёл"
    override val errorKeyHint = "{0} не принял API-ключ. Проверьте его в настройках ассистента."
    override val errorModelTitle = "Модель не найдена"
    override val errorModelHint = "У {0} нет модели {1}. Выберите другую в настройках ассистента."
    override val errorModelLocalHint = "Модель {1} не установлена. Выполните: ollama pull {1}"
    override val errorRequestTitle = "Запрос отклонён"
    override val errorRequestHint = "{0} не принял запрос. Спросите короче или выберите другую модель."
    override val errorServerTitle = "Сбой на стороне сервиса"
    override val errorServerHint = "{0} ответил ошибкой {1}. Повторите позже."
    override val errorNetworkTitle = "Нет связи с {0}"
    override val errorNetworkHint = "Проверьте интернет или VPN и повторите."
    override val errorEmptyTitle = "Пустой ответ"
    override val errorEmptyHint = "Модель ничего не ответила. Повторите запрос."
    override val retry = "Повторить"
    override val highlightActive = "Подсвечиваю в чате: {0}"
    override val highlightStop = "Остановить"
    override val highlightResume = "Подсвечивать снова"
    override val highlightStopped = "Подсветка остановлена"
}
