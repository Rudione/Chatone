package io.rudione.chatone.presentation.chat.components

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.event_points
import chatone.composeapp.generated.resources.event_poll_votes
import chatone.composeapp.generated.resources.event_poll_won
import io.rudione.chatone.presentation.browse.compactQuantity
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun eventPoints(count: Int, compact: String): String =
    pluralStringResource(Res.plurals.event_points, compactQuantity(count), compact)

@Composable
internal fun pollWonText(choice: String, percent: Int, votes: Int): String =
    stringResource(Res.string.event_poll_won, choice, percent, pluralStringResource(Res.plurals.event_poll_votes, votes, votes))
