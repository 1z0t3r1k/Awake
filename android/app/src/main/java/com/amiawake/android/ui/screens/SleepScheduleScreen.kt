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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amiawake.android.ui.MainUiState
import com.amiawake.android.ui.MainViewModel
import com.amiawake.android.ui.components.ConfirmationDialog
import com.amiawake.android.ui.components.PrimaryActionButton
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScheduleScreen(
    state: MainUiState,
    padding: PaddingValues,
    onSave: (LocalTime, LocalTime) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val initialSleep = remember(state.schedule?.sleepTime) { parseTime(state.schedule?.sleepTime, LocalTime.of(23, 0)) }
    val initialWake = remember(state.schedule?.wakeTime) { parseTime(state.schedule?.wakeTime, LocalTime.of(7, 0)) }
    var sleepTime by remember(state.schedule?.sleepTime) { mutableStateOf(initialSleep) }
    var wakeTime by remember(state.schedule?.wakeTime) { mutableStateOf(initialWake) }
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val loading = state.isRunning(MainViewModel.SCHEDULE_ACTION)

    picker?.let { target ->
        TimePickerDialog(
            title = if (target == PickerTarget.SLEEP) "Когда вы обычно ложитесь?" else "Когда вы обычно просыпаетесь?",
            initial = if (target == PickerTarget.SLEEP) sleepTime else wakeTime,
            onDismiss = { picker = null },
            onConfirm = { value -> if (target == PickerTarget.SLEEP) sleepTime = value else wakeTime = value; picker = null },
        )
    }
    if (confirmDelete) ConfirmationDialog(
        title = "Удалить расписание сна?",
        message = "Приложение больше не будет учитывать ваше обычное время сна.",
        confirmText = "Удалить",
        onConfirm = { confirmDelete = false; onDelete() },
        onDismiss = { confirmDelete = false },
    )

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Обычное время сна", style = MaterialTheme.typography.headlineSmall)
        Text("Расписание помогает приложению точнее определять, когда вы можете спать.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                TimeRow(Icons.Outlined.Bedtime, "Ложусь", sleepTime, { picker = PickerTarget.SLEEP })
                TimeRow(Icons.Outlined.LightMode, "Просыпаюсь", wakeTime, { picker = PickerTarget.WAKE })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Учитывать расписание сна", style = MaterialTheme.typography.titleMedium)
                Text(if (state.schedule?.enabled == true) "Расписание помогает определению сна" else "Сейчас расписание не учитывается", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = state.schedule?.enabled == true, onCheckedChange = onEnabled, enabled = !loading)
        }
        PrimaryActionButton("Сохранить расписание", { onSave(sleepTime, wakeTime) }, loading, modifier = Modifier.fillMaxWidth())
        if (state.schedule != null) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Удалить расписание") }
        }
    }
}

@Composable
private fun TimeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: LocalTime, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(value.format(TimeFormatter), style = MaterialTheme.typography.headlineSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("Готово") } },
    )
}

private enum class PickerTarget { SLEEP, WAKE }
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun parseTime(raw: String?, fallback: LocalTime): LocalTime = runCatching { LocalTime.parse(raw?.take(5)) }.getOrDefault(fallback)
