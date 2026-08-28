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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amiawake.android.data.UserSearchResponse
import com.amiawake.android.ui.MainUiState
import com.amiawake.android.ui.MainViewModel
import com.amiawake.android.ui.components.ConfirmationDialog
import com.amiawake.android.ui.components.EmptyState
import com.amiawake.android.ui.components.ErrorState
import com.amiawake.android.ui.components.FriendCard
import com.amiawake.android.ui.components.UserAvatar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FriendsScreen(
    state: MainUiState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onSend: (String) -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onCancel: (String) -> Unit,
    onFriend: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var confirmation by remember { mutableStateOf<PendingConfirmation?>(null) }
    val searching = state.searchQuery.trim().length >= 2

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
            Text("Найдите человека по имени или username", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                label = { Text("Поиск людей") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) IconButton(onClick = { onSearch("") }) { Icon(Icons.Outlined.Close, "Очистить поиск") }
                },
                supportingText = { if (state.searchQuery.isNotBlank() && !searching) Text("Введите хотя бы 2 символа") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }

        if (searching) {
            SearchResults(state, onSearch, onSend, onAccept, onFriend)
        } else {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selectedTab == 0, { selectedTab = 0 }, text = { Text("Друзья") })
                Tab(selectedTab == 1, { selectedTab = 1 }, text = { Text(if (state.friends.incoming.isEmpty()) "Заявки" else "Заявки · ${state.friends.incoming.size}") })
            }
            PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selectedTab == 0) {
                        if (state.friends.friends.isEmpty()) item {
                            EmptyState("Здесь пока никого нет", "Найдите друга, чтобы видеть, когда ему удобно написать или позвонить.")
                        }
                        items(state.friends.friends, key = { it.username }) { friend -> FriendCard(friend, { onFriend(friend.username) }) }
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
}

@Composable
private fun SearchResults(state: MainUiState, onSearch: (String) -> Unit, onSend: (String) -> Unit, onAccept: (String) -> Unit, onFriend: (String) -> Unit) {
    when {
        state.searchLoading -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
        state.searchError != null -> ErrorState(state.searchError, { onSearch(state.searchQuery) })
        state.searchResults.isEmpty() -> EmptyState("Никого не нашли", "Попробуйте проверить написание имени или username.", icon = Icons.Outlined.Search)
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Результаты", style = MaterialTheme.typography.titleLarge) }
            items(state.searchResults, key = { it.userId }) { user ->
                val friend = state.friends.friends.any { it.username == user.username }
                val incoming = state.friends.incoming.any { it.username == user.username }
                val outgoing = state.friends.outgoing.any { it.username == user.username }
                SearchUserCard(
                    user = user,
                    action = when { friend -> "Открыть"; incoming -> "Принять"; outgoing -> "Заявка отправлена"; else -> "Добавить" },
                    loading = state.isRunning(MainViewModel.FRIEND_REQUEST_ACTION) || state.isRunning("accept:${user.username}"),
                    enabled = !outgoing,
                    onClick = when { friend -> ({ onFriend(user.username) }); incoming -> ({ onAccept(user.username) }); else -> ({ onSend(user.username) }) },
                )
            }
        }
    }
}

@Composable
private fun SearchUserCard(user: UserSearchResponse, action: String, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAvatar(user.displayName)
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onClick, enabled = enabled && !loading) {
                if (loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text(action)
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
