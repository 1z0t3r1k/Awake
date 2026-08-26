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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.amiawake.android.data.SleepState
import com.amiawake.android.ui.MainUiState
import com.amiawake.android.ui.components.EmptyState
import com.amiawake.android.ui.components.ErrorState
import com.amiawake.android.ui.components.FriendCard
import com.amiawake.android.ui.components.LoadingContent
import com.amiawake.android.ui.components.SectionHeader
import com.amiawake.android.ui.model.confidenceLabel
import com.amiawake.android.ui.model.description
import com.amiawake.android.ui.model.freshnessLabel
import com.amiawake.android.ui.model.title

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(state: MainUiState, padding: PaddingValues, onRefresh: () -> Unit, onFriends: () -> Unit, onFriend: (String) -> Unit) {
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
        when {
            state.initialLoading && state.dashboard == null -> LoadingContent(Modifier.fillMaxSize())
            state.dashboard == null && state.loadError != null -> ErrorState(state.loadError, onRefresh, Modifier.fillMaxSize())
            else -> {
                val dashboard = state.dashboard
                val sleep = dashboard?.userState?.state ?: SleepState.UNKNOWN
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        Text("Здравствуйте, ${dashboard?.user?.displayName ?: "друг"}", style = MaterialTheme.typography.headlineSmall)
                        Text("Коротко о том, что происходит сейчас", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item { SleepHero(sleep, dashboard?.userState?.confidence ?: 0.0, dashboard?.userState?.calculatedAt) }
                    if (state.friends.incoming.isNotEmpty()) {
                        item {
                            Surface(onClick = onFriends, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Column(Modifier.weight(1f)) {
                                        Text("Новые заявки в друзья", style = MaterialTheme.typography.titleMedium)
                                        Text("Откройте, чтобы ответить", color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Badge { Text(state.friends.incoming.size.toString()) }
                                }
                            }
                        }
                    }
                    item { SectionHeader("Друзья сейчас", "Все", onFriends) }
                    if (state.friends.friends.isEmpty()) {
                        item { EmptyState("Здесь пока никого нет", "Добавьте друга, чтобы видеть, когда ему удобно написать или позвонить.", action = "Добавить друга", onAction = onFriends) }
                    } else {
                        items(state.friends.friends.take(4), key = { it.username }) { friend ->
                            FriendCard(friend.username, friend.status, { onFriend(friend.username) })
                        }
                    }
                    if (state.loadError != null && dashboard != null) {
                        item { ErrorState(state.loadError, onRefresh) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepHero(state: SleepState, confidence: Double, calculatedAt: String?) {
    val icon: ImageVector = if (state == SleepState.SLEEPING) Icons.Outlined.Bedtime else Icons.Outlined.WbSunny
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) {
                Icon(icon, null, Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(22.dp))
            Text(state.title(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(state.description(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
                InfoPill(freshnessLabel(calculatedAt))
                if (state != SleepState.UNKNOWN) InfoPill(confidenceLabel(confidence))
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge)
    }
}
