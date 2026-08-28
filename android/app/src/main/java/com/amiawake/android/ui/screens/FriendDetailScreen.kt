package com.amiawake.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amiawake.android.data.FriendResponse
import com.amiawake.android.ui.components.ConfirmationDialog
import com.amiawake.android.ui.components.StatusBadge
import com.amiawake.android.ui.components.UserAvatar
import com.amiawake.android.ui.model.copy
import com.amiawake.android.ui.model.confidenceLabel
import com.amiawake.android.ui.model.description
import com.amiawake.android.ui.model.freshnessLabel
import com.amiawake.android.ui.model.title

@Composable
fun FriendDetailScreen(friend: FriendResponse?, padding: PaddingValues, onDelete: (String) -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (friend == null) {
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Этот друг больше не в вашем списке", style = MaterialTheme.typography.titleLarge)
        }
        return
    }
    if (confirmDelete) ConfirmationDialog(
        title = "Удалить @${friend.username} из друзей?",
        message = "Вы больше не сможете видеть статус друг друга, пока снова не добавитесь.",
        confirmText = "Удалить",
        onConfirm = { confirmDelete = false; onDelete(friend.username) },
        onDismiss = { confirmDelete = false },
    )
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserAvatar(friend.displayName, large = true)
        Spacer(Modifier.height(16.dp))
        Text(friend.displayName, style = MaterialTheme.typography.headlineSmall)
        Text("@${friend.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        StatusBadge(friend.status)
        Spacer(Modifier.height(28.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Доступность", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(friend.status.copy().title, style = MaterialTheme.typography.headlineSmall)
                Text(friend.status.copy().description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Определение сна", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                Text(friend.sleepState.title(forSelf = false), style = MaterialTheme.typography.headlineSmall)
                Text(friend.sleepState.description(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(freshnessLabel(friend.sleepStateCalculatedAt), style = MaterialTheme.typography.bodyMedium)
                        if (friend.sleepStateCalculatedAt != null) Text(confidenceLabel(friend.sleepConfidence), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(36.dp))
        OutlinedButton(
            onClick = { confirmDelete = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.DeleteOutline, null)
            Spacer(Modifier.padding(4.dp))
            Text("Удалить из друзей")
        }
    }
}
