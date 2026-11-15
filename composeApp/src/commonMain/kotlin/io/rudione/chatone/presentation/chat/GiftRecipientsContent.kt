package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_notice_gift_available
import chatone.composeapp.generated.resources.chat_notice_gift_pending
import io.rudione.chatone.domain.model.ChatNoticeUser
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GiftRecipientsContent(
    recipients: List<ChatNoticeUser>,
    total: Int,
    onOpenUser: (ChatNoticeUser) -> Unit,
    onUserVisible: (String) -> Unit
) {
    var limit by remember { mutableIntStateOf(12) }
    val visible = recipients.take(limit)
    LaunchedEffect(visible) { visible.forEach { if (it.userId.isNotBlank()) onUserVisible(it.userId) } }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 5.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (recipients.isEmpty()) stringResource(Res.string.chat_notice_gift_pending)
            else stringResource(Res.string.chat_notice_gift_available, recipients.size, total),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            visible.forEach { user ->
                ChatNickname(
                    user,
                    fontSizeSp = 12f,
                    onClick = { onOpenUser(user) },
                    modifier = Modifier.clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
        if (recipients.size > limit) TextButton(onClick = { limit += 12 }) {
            Text("+${recipients.size - limit}", color = MaterialTheme.colorScheme.primary)
        }
    }
}
