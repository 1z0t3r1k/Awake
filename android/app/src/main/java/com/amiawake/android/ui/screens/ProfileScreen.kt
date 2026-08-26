package com.amiawake.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amiawake.android.ui.MainUiState
import com.amiawake.android.ui.components.ConfirmationDialog
import com.amiawake.android.ui.components.SectionHeader
import com.amiawake.android.ui.components.SettingsRow
import com.amiawake.android.ui.components.UserAvatar

@Composable
fun ProfileScreen(state: MainUiState, padding: PaddingValues, onSchedule: () -> Unit, onLogout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) ConfirmationDialog(
        title = "Выйти из аккаунта?",
        message = "На этом устройстве потребуется снова ввести имя пользователя и пароль.",
        confirmText = "Выйти",
        onConfirm = { confirmLogout = false; onLogout() },
        onDismiss = { confirmLogout = false },
    )
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val user = state.dashboard?.user
            UserAvatar(user?.displayName ?: "?", large = true)
            Spacer(Modifier.height(14.dp))
            Text(user?.displayName ?: "Профиль", style = MaterialTheme.typography.headlineSmall)
            Text("@${user?.username ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(32.dp))
        SectionHeader("Настройки")
        SettingsRow(
            Icons.Outlined.Bedtime,
            "Обычное время сна",
            state.schedule?.let { "${it.sleepTime.take(5)} — ${it.wakeTime.take(5)} · ${if (it.enabled) "учитывается" else "выключено"}" } ?: "Пока не настроено",
            onClick = onSchedule,
        )
        SettingsRow(Icons.Outlined.DarkMode, "Оформление", "Как в настройках устройства")
        SettingsRow(Icons.Outlined.Language, "Часовой пояс", state.dashboard?.user?.timeZone ?: "Определяется аккаунтом")
        Spacer(Modifier.height(28.dp))
        SectionHeader("Синхронизация")
        val pending = state.dashboard?.pendingEventCount ?: 0
        SettingsRow(
            if (pending == 0) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
            if (pending == 0) "Данные актуальны" else "Ожидает подключения",
            if (pending == 0) "Фоновая синхронизация работает автоматически" else "Данные обновятся, когда появится интернет",
        )
        Spacer(Modifier.height(28.dp))
        TextButton(onClick = { confirmLogout = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            androidx.compose.material3.Icon(Icons.Outlined.Logout, null)
            Spacer(Modifier.padding(4.dp))
            Text("Выйти из аккаунта")
        }
    }
}
