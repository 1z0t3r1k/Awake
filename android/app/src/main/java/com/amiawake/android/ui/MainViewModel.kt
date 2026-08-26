package com.amiawake.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amiawake.android.AppContainer
import com.amiawake.android.data.AvailabilityStatus
import com.amiawake.android.data.DashboardData
import com.amiawake.android.data.FriendsData
import com.amiawake.android.data.SleepScheduleResponse
import com.amiawake.android.data.userMessage
import java.time.LocalTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val checkingSession: Boolean = true,
    val authenticated: Boolean = false,
    val initialLoading: Boolean = false,
    val refreshing: Boolean = false,
    val dashboard: DashboardData? = null,
    val friends: FriendsData = FriendsData(emptyList(), emptyList(), emptyList()),
    val schedule: SleepScheduleResponse? = null,
    val loadError: String? = null,
    val authError: String? = null,
    val runningActions: Set<String> = emptySet(),
    val message: String? = null,
) {
    fun isRunning(action: String): Boolean = action in runningActions
}

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val authenticated = container.sessionStore.current() != null
            _state.update { it.copy(checkingSession = false, authenticated = authenticated) }
            if (authenticated) refreshAll(initial = true)
        }
    }

    fun authenticate(username: String, password: String, register: Boolean) {
        if (_state.value.isRunning(AUTH_ACTION)) return
        launchAction(AUTH_ACTION, errorTarget = ErrorTarget.AUTH) {
            if (register) repository.register(username, password) else repository.login(username, password)
            _state.update { it.copy(authenticated = true, authError = null) }
            refreshAll(initial = true)
        }
    }

    fun logout() = launchAction(LOGOUT_ACTION) {
        repository.logout()
        _state.value = MainUiState(checkingSession = false)
    }

    fun refreshAll(initial: Boolean = false) {
        if (_state.value.refreshing || _state.value.initialLoading) return
        viewModelScope.launch {
            _state.update { it.copy(initialLoading = initial && it.dashboard == null, refreshing = !initial, loadError = null) }
            runCatching {
                val dashboard = async { repository.loadDashboard() }
                val friends = async { repository.loadFriends() }
                val schedule = async { repository.loadSchedule() }
                Triple(dashboard.await(), friends.await(), schedule.await())
            }.onSuccess { (dashboard, friends, schedule) ->
                _state.update { it.copy(dashboard = dashboard, friends = friends, schedule = schedule) }
            }.onFailure { error ->
                val message = error.userMessage(container.network.json)
                _state.update { it.copy(loadError = message, message = if (it.dashboard != null) message else it.message) }
            }
            _state.update { it.copy(initialLoading = false, refreshing = false) }
        }
    }

    fun setStatus(status: AvailabilityStatus) = launchAction(STATUS_ACTION) {
        repository.setStatus(status)
        _state.update { current -> current.copy(dashboard = current.dashboard?.copy(status = status), message = "Статус обновлён") }
    }

    fun sendFriendRequest(username: String) = launchAction(FRIEND_REQUEST_ACTION) {
        require(username.isNotBlank()) { "Введите имя пользователя" }
        repository.sendFriendRequest(username)
        reloadFriends()
        _state.update { it.copy(message = "Заявка отправлена") }
    }

    fun acceptFriend(username: String) = launchAction("accept:$username") {
        repository.acceptFriendRequest(username)
        reloadFriends()
        _state.update { it.copy(message = "@$username теперь в ваших друзьях") }
    }

    fun removeFriend(username: String) = launchAction("remove:$username") {
        repository.deleteFriend(username)
        reloadFriends()
        _state.update { it.copy(message = "Друг удалён") }
    }

    fun cancelRequest(username: String) = launchAction("cancel:$username") {
        repository.deletePendingRequest(username)
        reloadFriends()
        _state.update { it.copy(message = "Заявка отменена") }
    }

    fun declineRequest(username: String) = launchAction("decline:$username") {
        repository.deletePendingRequest(username)
        reloadFriends()
        _state.update { it.copy(message = "Заявка отклонена") }
    }

    fun saveSchedule(sleepTime: LocalTime, wakeTime: LocalTime) = launchAction(SCHEDULE_ACTION) {
        val schedule = repository.saveSchedule(sleepTime.toString(), wakeTime.toString())
        _state.update { it.copy(schedule = schedule, message = "Расписание сохранено") }
    }

    fun setScheduleEnabled(enabled: Boolean) = launchAction(SCHEDULE_ACTION) {
        val current = _state.value.schedule
        val schedule = if (current == null) {
            repository.saveSchedule("23:00", "07:00").let {
                if (enabled) it else repository.setScheduleEnabled(false)
            }
        } else repository.setScheduleEnabled(enabled)
        _state.update { it.copy(schedule = schedule, message = if (enabled) "Расписание включено" else "Расписание выключено") }
    }

    fun deleteSchedule() = launchAction(SCHEDULE_ACTION) {
        repository.deleteSchedule()
        _state.update { it.copy(schedule = null, message = "Расписание удалено") }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private suspend fun reloadFriends() {
        val friends = repository.loadFriends()
        _state.update { it.copy(friends = friends) }
    }

    private fun launchAction(action: String, errorTarget: ErrorTarget = ErrorTarget.MESSAGE, block: suspend () -> Unit) {
        if (_state.value.isRunning(action)) return
        viewModelScope.launch {
            _state.update { it.copy(runningActions = it.runningActions + action, authError = if (errorTarget == ErrorTarget.AUTH) null else it.authError) }
            runCatching { block() }.onFailure { error ->
                val message = error.userMessage(container.network.json)
                _state.update { if (errorTarget == ErrorTarget.AUTH) it.copy(authError = message) else it.copy(message = message) }
            }
            _state.update { it.copy(runningActions = it.runningActions - action) }
        }
    }

    private enum class ErrorTarget { AUTH, MESSAGE }

    companion object {
        const val AUTH_ACTION = "auth"
        const val LOGOUT_ACTION = "logout"
        const val STATUS_ACTION = "status"
        const val FRIEND_REQUEST_ACTION = "friend-request"
        const val SCHEDULE_ACTION = "schedule"
    }
}

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container) as T
}
