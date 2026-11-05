package io.rudione.chatone.presentation.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Immutable
sealed interface UiText {
    data class Raw(val value: String) : UiText

    data class Resource(val resource: StringResource, val args: List<Any?>) : UiText
}

fun uiText(resource: StringResource, vararg args: Any?): UiText = UiText.Resource(resource, args.toList())

suspend fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> getString(resource, *args.map { (it as? UiText)?.resolve() ?: it ?: "" }.toTypedArray())
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(resource, *args.map { (it as? UiText)?.asString() ?: it ?: "" }.toTypedArray())
}
