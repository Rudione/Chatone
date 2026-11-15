package io.rudione.chatone.presentation.laboratory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.lab_title
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.components.SystemBackHandler
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LaboratoryScreen(onClose: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settings by viewModel.state.collectAsState()
    SystemBackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChatoneIconButton(onClick = onClose) {
                    Icon(Lucide.ArrowLeft, stringResource(Res.string.lab_title), tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(Res.string.lab_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
            LaboratoryContent(settings, viewModel::sendEvent)
        }
    }
}
