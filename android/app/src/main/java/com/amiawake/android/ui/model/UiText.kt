package com.amiawake.android.ui.model

import com.amiawake.android.data.AvailabilityStatus
import com.amiawake.android.data.SleepState
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale

data class AvailabilityCopy(val title: String, val description: String)

fun AvailabilityStatus.copy(): AvailabilityCopy = when (this) {
    AvailabilityStatus.AVAILABLE -> AvailabilityCopy("Можно звонить", "Вы открыты для звонков и сообщений")
    AvailabilityStatus.TEXT_ONLY -> AvailabilityCopy("Лучше написать", "Сейчас удобнее общаться сообщениями")
    AvailabilityStatus.DO_NOT_DISTURB -> AvailabilityCopy("Не беспокоить", "Вы просите не отвлекать вас")
    AvailabilityStatus.SLEEPING -> AvailabilityCopy("Сплю", "Установлено вами вручную")
}

fun SleepState.title(forSelf: Boolean = true): String = when (this) {
    SleepState.AWAKE -> if (forSelf) "Вы сейчас не спите" else "Сейчас не спит"
    SleepState.SLEEPING -> if (forSelf) "Похоже, вы спите" else "Похоже, спит"
    SleepState.UNKNOWN -> "Пока недостаточно данных"
}

fun SleepState.description(): String = when (this) {
    SleepState.AWAKE -> "Приложение видит недавнюю активность"
    SleepState.SLEEPING -> "Сигналы устройства похожи на сон"
    SleepState.UNKNOWN -> "Приложению нужно немного времени, чтобы определить состояние"
}

fun confidenceLabel(value: Double): String = when {
    value >= .75 -> "Высокая уверенность"
    value >= .45 -> "Средняя уверенность"
    else -> "Недостаточно данных"
}

fun freshnessLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return "Обновлений пока нет"
    val instant = try { Instant.parse(raw) } catch (_: DateTimeParseException) { return "Недавно обновлено" }
    val minutes = Duration.between(instant, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Обновлено только что"
        minutes < 60 -> "Обновлено $minutes ${pluralMinutes(minutes)} назад"
        minutes < 24 * 60 -> "Обновлено ${minutes / 60} ч назад"
        else -> "Обновлено ${minutes / (24 * 60)} дн. назад"
    }
}

fun initials(name: String): String = name.trim().split(Regex("[ _.-]+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1) }
    .uppercase(Locale.getDefault())
    .ifBlank { "?" }

private fun pluralMinutes(value: Long): String {
    val mod10 = value % 10
    val mod100 = value % 100
    return when {
        mod10 == 1L && mod100 != 11L -> "минуту"
        mod10 in 2L..4L && mod100 !in 12L..14L -> "минуты"
        else -> "минут"
    }
}
