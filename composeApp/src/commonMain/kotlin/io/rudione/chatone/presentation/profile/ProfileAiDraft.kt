package io.rudione.chatone.presentation.profile

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_ai_about
import chatone.composeapp.generated.resources.profile_ai_followers
import chatone.composeapp.generated.resources.profile_ai_live
import chatone.composeapp.generated.resources.profile_ai_prompt
import io.rudione.chatone.domain.profile.ChannelProfile
import io.rudione.chatone.util.format.groupedNumber
import org.jetbrains.compose.resources.stringResource

private const val MAX_ABOUT = 400

@Composable
internal fun profileAiDraft(profile: ChannelProfile): String {
    val lines = buildList {
        add(
            stringResource(
                Res.string.profile_ai_prompt,
                profile.displayName.ifBlank { profile.login },
                profile.login
            )
        )
        profile.description.replace('\n', ' ').trim().take(MAX_ABOUT).takeIf { it.isNotEmpty() }
            ?.let {
                add(stringResource(Res.string.profile_ai_about, it))
            }
        profile.live?.let {
            add(
                stringResource(
                    Res.string.profile_ai_live,
                    it.title.trim(),
                    it.category
                )
            )
        }
        profile.followers?.let {
            add(
                stringResource(
                    Res.string.profile_ai_followers,
                    groupedNumber(it.toLong())
                )
            )
        }
    }
    return lines.joinToString("\n")
}
