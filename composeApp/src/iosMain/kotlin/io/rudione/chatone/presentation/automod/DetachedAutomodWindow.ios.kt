package io.rudione.chatone.presentation.automod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.rudione.chatone.data.repository.AutomodRepository
import io.rudione.chatone.util.AutomodImportExport
import io.rudione.chatone.util.readAutomodText
import io.rudione.chatone.util.saveAutomodText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun DetachedAutomodWindow(
    currentChannelLogin: String?,
    onClose: () -> Unit
) {
    val repository: AutomodRepository = koinInject()
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    val parsed = AutomodImportExport.fromJson(text)
                    if (parsed.isNotEmpty()) repository.importMerge(parsed)
                }
            },
            repository = repository
        )
    }
}
