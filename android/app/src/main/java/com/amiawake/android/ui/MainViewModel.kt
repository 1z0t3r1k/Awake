package com.amiawake.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amiawake.android.AppContainer
import com.amiawake.android.data.AvailabilityStatus
import com.amiawake.android.data.DashboardData
import com.amiawake.android.data.FriendsData
import com.amiawake.android.data.SleepScheduleResponse
import com.amiawake.android.data.UserSearchResponse
import com.amiawake.android.data.userMessage
import java.time.LocalTime
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import retrofit2.HttpException

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
    val searchQuery: String = "",
    val searchResults: List<UserSearchResponse> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: String? = null,
    val runningActions: Set<String> = emptySet(),
    val message: String? = null,
) {
    fun isRunning(action: String): Boolean = action in runningActions
}

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val authenticated = container.sessionStore.current() != null
            _state.update { it.copy(checkingSession = false, authenticated = authenticated) }
            if (authenticated) refreshAll(initial = true)
        }
    }

    fun authenticate(username: String, password: String, displayName: String, register: Boolean) {
        if (_state.value.isRunning(AUTH_ACTION)) return
        launchAction(AUTH_ACTION, errorTarget = ErrorTarget.AUTH) {
            if (register) repository.register(username, password, displayName) else repository.login(username, password)
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
            try {
                val (dashboard, friends, schedule) = supervisorScope {
                    val dashboard = async { repository.loadDashboard() }
                    val friends = async { repository.loadFriends() }
                    val schedule = async { repository.loadSchedule() }
                    Triple(dashboard.await(), friends.await(), schedule.await())
                }
                _state.update { it.copy(dashboard = dashboard, friends = friends, schedule = schedule) }
            } catch (error: Throwable) {
                if (!handleExpiredSession(error)) {
                    val message = error.userMessage(container.network.json)
                    _state.update { it.copy(loadError = message, message = if (it.dashboard != null) message else it.message) }
                }
            } finally {
                _state.update { it.copy(initialLoading = false, refreshing = false) }
            }
        }
    }

    fun setStatus(status: AvailabilityStatus) = launchAction(STATUS_ACTION) {
        repository.setStatus(status)
        _state.update { current ->
            current.copy(
                dashboard = current.dashboard?.let { dashboard -> dashboard.copy(status = status, user = dashboard.user.copy(status = status)) },
                message = "Статус обновлён",
            )
        }
    }

    fun updateDisplayName(displayName: String) = launchAction(PROFILE_ACTION) {
        require(displayName.isNotBlank()) { "Введите имя" }
        require(displayName.trim().length <= 32) { "Имя должно быть не длиннее 32 символов" }
        val user = repository.updateDisplayName(displayName)
        _state.update { it.copy(dashboard = it.dashboard?.copy(user = user, status = user.status), message = "Имя обновлено") }
    }

    fun updateTimeZone(zoneId: String) = launchAction(PROFILE_ACTION) {
        require(zoneId.isNotBlank()) { "Укажите часовой пояс" }
        val user = repository.updateTimeZone(zoneId)
        _state.update { it.copy(dashboard = it.dashboard?.copy(user = user, status = user.status), message = "Часовой пояс обновлён") }
    }

    fun searchUsers(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(searchResults = emptyList(), searchLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(searchLoading = true) }
            try {
                val results = repository.searchUsers(query)
                if (_state.value.searchQuery == query) _state.update { it.copy(searchResults = results, searchLoading = false) }
            } catch (error: Throwable) {
                if (!handleExpiredSession(error) && _state.value.searchQuery == query) {
                    _state.update { it.copy(searchLoading = false, searchError = error.userMessage(container.network.json)) }
                }
            }
        }
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
            try {
                block()
            } catch (error: Throwable) {
                if (errorTarget == ErrorTarget.AUTH || !handleExpiredSession(error)) {
                    val message = error.userMessage(container.network.json)
                    _state.update { if (errorTarget == ErrorTarget.AUTH) it.copy(authError = message) else it.copy(message = message) }
                }
            } finally {
                _state.update { it.copy(runningActions = it.runningActions - action) }
            }
        }
    }

    private suspend fun handleExpiredSession(error: Throwable): Boolean {
        if (error !is HttpException || error.code() != 401) return false

        container.sessionStore.clear()
        _state.value = MainUiState(
            checkingSession = false,
            authError = "Сессия истекла. Войдите снова.",
        )
        return true
    }

    private enum class ErrorTarget { AUTH, MESSAGE }

    companion object {
        const val AUTH_ACTION = "auth"
        const val LOGOUT_ACTION = "logout"
        const val STATUS_ACTION = "status"
        const val FRIEND_REQUEST_ACTION = "friend-request"
        const val SCHEDULE_ACTION = "schedule"
        const val PROFILE_ACTION = "profile"
    }
}

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container) as T
}
