package io.rudione.chatone.presentation.theme.i18n

interface ChatTimelineStrings {
    val today: String
    val yesterday: String
    fun dayLabel(day: Int, month: Int, year: Int?): String
}

object ChatTimelineStringsEn : ChatTimelineStrings {
    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    override val today = "Today"
    override val yesterday = "Yesterday"
    override fun dayLabel(day: Int, month: Int, year: Int?): String =
        "${months.getOrElse(month - 1) { "$month" }} $day" + (year?.let { ", $it" } ?: "")
}

object ChatTimelineStringsRu : ChatTimelineStrings {
    private val months = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    override val today = "Сегодня"
    override val yesterday = "Вчера"
    override fun dayLabel(day: Int, month: Int, year: Int?): String =
        "$day ${months.getOrElse(month - 1) { "$month" }}" + (year?.let { " $it" } ?: "")
}
