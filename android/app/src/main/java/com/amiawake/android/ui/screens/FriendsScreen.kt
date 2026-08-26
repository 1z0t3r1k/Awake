package com.amiawake.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amiawake.android.ui.MainUiState
import com.amiawake.android.ui.MainViewModel
import com.amiawake.android.ui.components.ConfirmationDialog
import com.amiawake.android.ui.components.EmptyState
import com.amiawake.android.ui.components.FriendCard
import com.amiawake.android.ui.components.UserAvatar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FriendsScreen(
    state: MainUiState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onCancel: (String) -> Unit,
    onFriend: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var username by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<PendingConfirmation?>(null) }
    val focus = LocalFocusManager.current
    val requestLoading = state.isRunning(MainViewModel.FRIEND_REQUEST_ACTION)

    confirmation?.let { pending ->
        ConfirmationDialog(
            title = pending.title,
            message = pending.message,
            confirmText = pending.confirm,
            onConfirm = { if (pending.decline) onDecline(pending.username) else onCancel(pending.username); confirmation = null },
            onDismiss = { confirmation = null },
        )
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Друзья", style = MaterialTheme.typography.headlineSmall)
            Text("Добавляйте близких по имени пользователя", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = username,
                onValueChange = { if (it.length <= 32) username = it },
                label = { Text("Имя пользователя") },
                leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (username.isNotBlank() && !requestLoading) { onSend(username); username = "" } }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(
                onClick = { focus.clearFocus(); onSend(username); username = "" },
                enabled = username.isNotBlank() && !requestLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                if (requestLoading) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("Отправить заявку")
            }
        }
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selectedTab == 0, { selectedTab = 0 }, text = { Text("Друзья") })
            Tab(selectedTab == 1, { selectedTab = 1 }, text = { Text(if (state.friends.incoming.isEmpty()) "Заявки" else "Заявки · ${state.friends.incoming.size}") })
        }
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedTab == 0) {
                    if (state.friends.friends.isEmpty()) item {
                        EmptyState("Здесь пока никого нет", "Добавьте друга, чтобы видеть, когда ему удобно написать или позвонить.")
                    }
                    items(state.friends.friends, key = { it.username }) { friend -> FriendCard(friend.username, friend.status, { onFriend(friend.username) }) }
                } else {
                    item { Text("Входящие", style = MaterialTheme.typography.titleLarge) }
                    if (state.friends.incoming.isEmpty()) item { EmptyState("Новых заявок пока нет", "Когда кто-то захочет добавить вас, заявка появится здесь.") }
                    items(state.friends.incoming, key = { "in:${it.username}" }) { request ->
                        RequestCard(
                            request.username,
                            "Хочет добавить вас",
                            primary = "Принять",
                            primaryLoading = state.isRunning("accept:${request.username}"),
                            onPrimary = { onAccept(request.username) },
                            secondary = "Отклонить",
                            onSecondary = { confirmation = PendingConfirmation.decline(request.username) },
                        )
                    }
                    item { Text("Исходящие", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp)) }
                    if (state.friends.outgoing.isEmpty()) item { Text("Нет заявок, ожидающих ответа", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(state.friends.outgoing, key = { "out:${it.username}" }) { request ->
                        RequestCard(
                            request.username,
                            "Заявка отправлена",
                            primary = "Отменить заявку",
                            primaryLoading = state.isRunning("cancel:${request.username}"),
                            onPrimary = { confirmation = PendingConfirmation.cancel(request.username) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(username: String, subtitle: String, primary: String, primaryLoading: Boolean, onPrimary: () -> Unit, secondary: String? = null, onSecondary: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAvatar(username)
            Column(Modifier.weight(1f)) {
                Text("@$username", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onPrimary, enabled = !primaryLoading) { Text(primary) }
                if (secondary != null && onSecondary != null) TextButton(onClick = onSecondary) { Text(secondary) }
            }
        }
    }
}

private data class PendingConfirmation(val username: String, val title: String, val message: String, val confirm: String, val decline: Boolean) {
    companion object {
        fun decline(username: String) = PendingConfirmation(username, "Отклонить заявку?", "@$username не будет добавлен в друзья.", "Отклонить", true)
        fun cancel(username: String) = PendingConfirmation(username, "Отменить заявку?", "@$username больше не увидит вашу заявку в друзья.", "Отменить заявку", false)
    }
}
