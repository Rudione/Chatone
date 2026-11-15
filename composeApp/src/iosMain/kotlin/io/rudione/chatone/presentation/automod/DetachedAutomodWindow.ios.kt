package io.rudione.chatone.presentation.automod

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.data.repository.AutomodRepository
import io.rudione.chatone.presentation.nativecontext.SuspendNativeMessageMenus
import io.rudione.chatone.util.automod.AutomodImportExport
import io.rudione.chatone.util.automod.readAutomodText
import io.rudione.chatone.util.automod.saveAutomodText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun DetachedAutomodWindow(
    currentChannelLogin: String?,
    onClose: () -> Unit
) {
    val repository: AutomodRepository = koinInject()
    val scope = rememberCoroutineScope()
    SuspendNativeMessageMenus()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AutomodScreen(
                currentChannelLogin = currentChannelLogin,
                onClose = onClose,
                onExport = { fileName, content ->
                    scope.launch { saveAutomodText(fileName, content) }
                },
                onImport = {
                    scope.launch {
                        val text = readAutomodText() ?: return@launch
                        val parsed = AutomodImportExport.fromJson(text)
                        if (parsed.wordRules.isNotEmpty()) repository.importMerge(parsed.wordRules)
                        if (parsed.chatRules.isNotEmpty()) repository.importMergeChatRules(parsed.chatRules)
                    }
                },
                repository = repository,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}
