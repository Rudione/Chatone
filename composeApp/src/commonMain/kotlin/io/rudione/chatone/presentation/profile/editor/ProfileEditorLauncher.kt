package io.rudione.chatone.presentation.profile.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.koinInject

@Immutable
data class ProfileEditorRequest(val channelLogin: String?)

class ProfileEditorLauncher {
    private val _request = MutableStateFlow<ProfileEditorRequest?>(null)
    val request: StateFlow<ProfileEditorRequest?> = _request.asStateFlow()

    fun open(channelLogin: String?) {
        _request.value =
            ProfileEditorRequest(channelLogin?.trim()?.lowercase()?.takeIf { it.isNotEmpty() })
    }

    fun close() {
        _request.value = null
    }
}

@Composable
fun ProfileEditorHost() {
    val launcher: ProfileEditorLauncher = koinInject()
    val request by launcher.request.collectAsState()
    request?.let {
        ProfileEditorDialog(
            channelLogin = it.channelLogin,
            onDismiss = launcher::close
        )
    }
}
