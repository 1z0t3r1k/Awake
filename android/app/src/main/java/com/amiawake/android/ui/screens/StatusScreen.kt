package com.amiawake.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.amiawake.android.data.AvailabilityStatus
import com.amiawake.android.data.SleepState
import com.amiawake.android.ui.MainUiState
import com.amiawake.android.ui.MainViewModel
import com.amiawake.android.ui.components.LoadingContent
import com.amiawake.android.ui.model.confidenceLabel
import com.amiawake.android.ui.model.copy
import com.amiawake.android.ui.model.description
import com.amiawake.android.ui.model.freshnessLabel
import com.amiawake.android.ui.model.title

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatusScreen(state: MainUiState, padding: PaddingValues, onSetStatus: (AvailabilityStatus) -> Unit) {
    var sheetVisible by remember { mutableStateOf(false) }
    val dashboard = state.dashboard
    if (dashboard == null && state.initialLoading) {
        LoadingContent(Modifier.fillMaxSize().padding(padding))
        return
    }
    if (sheetVisible && dashboard != null) {
        StatusPicker(dashboard.status, state.isRunning(MainViewModel.STATUS_ACTION), { sheetVisible = false }, onSetStatus)
    }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Статус", style = MaterialTheme.typography.headlineSmall)
        Text("Управляйте тем, как друзья видят вашу доступность", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Ваш статус", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.padding(5.dp))
                Text(dashboard?.status?.copy()?.title ?: "Не удалось загрузить", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(dashboard?.status?.copy()?.description.orEmpty(), color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.padding(8.dp))
                Button(onClick = { sheetVisible = true }, enabled = dashboard != null && !state.isRunning(MainViewModel.STATUS_ACTION)) {
                    if (state.isRunning(MainViewModel.STATUS_ACTION)) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else { Icon(Icons.Outlined.Edit, null); Spacer(Modifier.padding(4.dp)); Text("Изменить статус") }
                }
            }
        }
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                    Column {
                        Text("Определение сна", style = MaterialTheme.typography.titleMedium)
                        Text("Автоматически по сигналам устройства", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.padding(10.dp))
                val sleep = dashboard?.userState?.state ?: SleepState.UNKNOWN
                Text(sleep.title(), style = MaterialTheme.typography.titleLarge)
                Text(sleep.description(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(6.dp))
                Text(freshnessLabel(dashboard?.userState?.calculatedAt), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (sleep != SleepState.UNKNOWN) Text(confidenceLabel(dashboard?.userState?.confidence ?: 0.0), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Выбранный вами статус и автоматическое определение сна — разные вещи. Друзья видят ваш статус доступности; определение сна помогает приложению понимать контекст.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StatusPicker(selected: AvailabilityStatus, loading: Boolean, onDismiss: () -> Unit, onSelect: (AvailabilityStatus) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Как с вами связаться?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            AvailabilityStatus.entries.forEachIndexed { index, status ->
                val icon = statusIcon(status)
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !loading) { onSelect(status); onDismiss() }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(status.copy().title, style = MaterialTheme.typography.titleMedium)
                        Text(status.copy().description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    RadioButton(selected = selected == status, onClick = null)
                }
                if (index < AvailabilityStatus.entries.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

private fun statusIcon(status: AvailabilityStatus): ImageVector = when (status) {
    AvailabilityStatus.AVAILABLE -> Icons.Outlined.Call
    AvailabilityStatus.TEXT_ONLY -> Icons.Outlined.ChatBubbleOutline
    AvailabilityStatus.DO_NOT_DISTURB -> Icons.Outlined.DoNotDisturbOn
}
