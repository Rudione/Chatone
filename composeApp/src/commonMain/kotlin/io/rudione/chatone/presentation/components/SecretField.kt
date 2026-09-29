package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.delay
import io.rudione.chatone.icons.lucide.Eye
import io.rudione.chatone.icons.lucide.EyeOff
import io.rudione.chatone.icons.lucide.Lucide

private const val SECRET_AUTO_HIDE_MS = 20_000L
private val SecretMask = PasswordVisualTransformation('•')

@Stable
class SecretVisibility {
    var revealed by mutableStateOf(false)
        private set

    val transformation: VisualTransformation
        get() = if (revealed) VisualTransformation.None else SecretMask

    fun toggle() {
        revealed = !revealed
    }

    fun hide() {
        revealed = false
    }
}

@Composable
fun rememberSecretVisibility(autoHideMillis: Long = SECRET_AUTO_HIDE_MS): SecretVisibility {
    val visibility = remember { SecretVisibility() }
    LaunchedEffect(visibility.revealed) {
        if (visibility.revealed) {
            delay(autoHideMillis)
            visibility.hide()
        }
    }
    return visibility
}

@Composable
fun SecretRevealButton(visibility: SecretVisibility, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val label = if (visibility.revealed) s.secretHide else s.secretReveal
    ChatoneIconButton(onClick = visibility::toggle, modifier = modifier.size(26.dp)) {
        Icon(
            if (visibility.revealed) Lucide.EyeOff else Lucide.Eye,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatoneSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    trailing: (@Composable () -> Unit)? = null
) {
    val visibility = rememberSecretVisibility()
    ChatoneTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        hint = hint,
        enabled = enabled,
        isError = isError,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = visibility.transformation,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (value.isNotEmpty()) SecretRevealButton(visibility)
                trailing?.invoke()
            }
        }
    )
}
