package io.rudione.chatone.presentation.main.follows

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.follow_picker_confirm
import chatone.composeapp.generated.resources.follow_picker_confirm_count
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun followPickerConfirm(count: Int): String =
    if (count > 0) stringResource(Res.string.follow_picker_confirm_count, count) else stringResource(Res.string.follow_picker_confirm)
