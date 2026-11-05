package io.rudione.chatone.presentation.browse

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.browse_channels
import chatone.composeapp.generated.resources.browse_viewers
import org.jetbrains.compose.resources.pluralStringResource

private const val COMPACT_PLURAL_THRESHOLD = 1_000

internal fun compactQuantity(count: Int): Int = if (count >= COMPACT_PLURAL_THRESHOLD) COMPACT_PLURAL_THRESHOLD else count

@Composable
fun browseViewers(count: Int, compact: String): String =
    pluralStringResource(Res.plurals.browse_viewers, compactQuantity(count), compact)

@Composable
fun browseChannels(count: Int, compact: String): String =
    pluralStringResource(Res.plurals.browse_channels, compactQuantity(count), compact)
