package io.rudione.chatone.presentation.automod

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.readAutomodText
import io.rudione.chatone.util.saveAutomodText
import io.rudione.chatone.util.AutomodImportExport
import io.rudione.chatone.data.repository.AutomodRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun DetachedAutomodWindow(
    currentChannelLogin: String?,
    onClose: () -> Unit
) {
    val repository: AutomodRepository = koinInject()
    val scope = rememberCoroutineScope()
    val windowState = rememberWindowState(
        width = 960.dp,
        height = 680.dp,
        position = WindowPosition.PlatformDefault
    )
    Window(
        onCloseRequest = onClose,
        title = "Chatone — Local Automod",
        state = windowState,
        resizable = true
    ) {
        ChatoneTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AutomodScreen(
                    currentChannelLogin = currentChannelLogin,
                    onClose = onClose,
                    onExport = { fileName, content ->
                        scope.launch { saveAutomodText(fileName, content) }
                    },
                    onImport = {
                        scope.launch {
                            val text = readAutomodText() ?: return@launch
                            val result = AutomodImportExport.fromJson(text)
                            if (result.wordRules.isNotEmpty()) repository.importMerge(result.wordRules)
                            if (result.chatRules.isNotEmpty()) repository.importMergeChatRules(result.chatRules)
                        }
                    },
                    repository = repository
                )
            }
        }
    }
}
