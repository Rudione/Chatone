package io.rudione.chatone.presentation.legal

import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReportTarget { MESSAGE, USER, STREAM }

data class ContentReport(
    val target: ReportTarget,
    val login: String,
    val channel: String = "",
    val messageId: String = "",
    val message: String = ""
) {
    fun body(reason: String, details: String): String = buildString {
        appendLine("Chatone content report: ${target.name}")
        appendLine("User: ${login.take(50)}")
        if (channel.isNotBlank()) appendLine("Channel: ${channel.take(50)}")
        if (messageId.isNotBlank()) appendLine("Message ID: ${messageId.take(100)}")
        appendLine("Reason: ${reason.take(150)}")
        if (details.isNotBlank()) appendLine("Details: ${details.take(2000)}")
        if (message.isNotBlank()) appendLine("Message: ${message.take(2000)}")
    }

    fun mailto(reason: String, details: String): String =
        "mailto:${AppLinks.ReportEmail}?subject=${"Chatone content report".encodeURLParameter()}&body=${body(reason, details).encodeURLParameter()}"
}

object ContentReports {
    private val _pending = MutableStateFlow<ContentReport?>(null)
    val pending = _pending.asStateFlow()
    fun open(report: ContentReport) { _pending.value = report }
    fun dismiss(report: ContentReport) { _pending.compareAndSet(report, null) }
}
