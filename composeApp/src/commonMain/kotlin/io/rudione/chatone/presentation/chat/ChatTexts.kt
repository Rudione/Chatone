package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.allStringResources
import chatone.composeapp.generated.resources.timeline_day_label
import chatone.composeapp.generated.resources.timeline_day_label_year
import chatone.composeapp.generated.resources.timeline_months
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

private const val SLASH_COMMAND_PREFIX = "slash_command_"

private fun slashCommandResource(name: String): StringResource? = Res.allStringResources[SLASH_COMMAND_PREFIX + name]

@Composable
fun slashCommandDescription(name: String): String =
    slashCommandResource(name)?.let { stringResource(it) } ?: name

suspend fun slashCommandDescriptionText(name: String): String =
    slashCommandResource(name)?.let { getString(it) } ?: name

@Composable
fun chatDayLabel(day: Int, month: Int, year: Int?): String {
    val monthName = stringArrayResource(Res.array.timeline_months).getOrElse(month - 1) { month.toString() }
    return if (year == null) {
        stringResource(Res.string.timeline_day_label, monthName, day)
    } else {
        stringResource(Res.string.timeline_day_label_year, monthName, day, year)
    }
}
