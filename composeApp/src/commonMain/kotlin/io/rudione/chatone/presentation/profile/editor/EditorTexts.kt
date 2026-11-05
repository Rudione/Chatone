package io.rudione.chatone.presentation.profile.editor

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_editor_field_accent
import chatone.composeapp.generated.resources.profile_editor_field_bio
import chatone.composeapp.generated.resources.profile_editor_field_category
import chatone.composeapp.generated.resources.profile_editor_field_channel_badge
import chatone.composeapp.generated.resources.profile_editor_field_display_name
import chatone.composeapp.generated.resources.profile_editor_field_global_badge
import chatone.composeapp.generated.resources.profile_editor_field_language
import chatone.composeapp.generated.resources.profile_editor_field_link_added
import chatone.composeapp.generated.resources.profile_editor_field_link_removed
import chatone.composeapp.generated.resources.profile_editor_field_name_color
import chatone.composeapp.generated.resources.profile_editor_field_paint
import chatone.composeapp.generated.resources.profile_editor_field_seven_tv_badge
import chatone.composeapp.generated.resources.profile_editor_field_stream_title
import chatone.composeapp.generated.resources.profile_editor_field_tags
import io.rudione.chatone.domain.profile.VanityField
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun vanityFieldLabel(field: VanityField): String = stringResource(
    when (field) {
        VanityField.NAME_COLOR -> Res.string.profile_editor_field_name_color
        VanityField.GLOBAL_BADGE -> Res.string.profile_editor_field_global_badge
        VanityField.CHANNEL_BADGE -> Res.string.profile_editor_field_channel_badge
        VanityField.PAINT -> Res.string.profile_editor_field_paint
        VanityField.SEVEN_TV_BADGE -> Res.string.profile_editor_field_seven_tv_badge
        VanityField.DISPLAY_NAME -> Res.string.profile_editor_field_display_name
        VanityField.BIO -> Res.string.profile_editor_field_bio
        VanityField.ACCENT -> Res.string.profile_editor_field_accent
        VanityField.LINK_ADDED -> Res.string.profile_editor_field_link_added
        VanityField.LINK_REMOVED -> Res.string.profile_editor_field_link_removed
        VanityField.STREAM_TITLE -> Res.string.profile_editor_field_stream_title
        VanityField.CATEGORY -> Res.string.profile_editor_field_category
        VanityField.TAGS -> Res.string.profile_editor_field_tags
        VanityField.LANGUAGE -> Res.string.profile_editor_field_language
    }
)
