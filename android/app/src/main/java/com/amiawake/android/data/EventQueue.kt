package com.amiawake.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.eventDataStore by preferencesDataStore("device_events")

class EventQueue(private val context: Context, private val json: Json) {
    private val eventsKey = stringPreferencesKey("pending_events")
    private val classificationsKey = stringPreferencesKey("pending_sleep_classifications")
    private val classificationSerializer = ListSerializer(SleepClassificationRequest.serializer())
    private val mutex = Mutex()
    private val serializer = ListSerializer(DeviceEventRequest.serializer())

    private val chargingKey = booleanPreferencesKey("last_observed_power_connected")
    private val motionKey = longPreferencesKey("last_motion_occurred_at")

    suspend fun enqueue(type: DeviceEventType, occurredAt: Instant = Instant.now()): Boolean = mutex.withLock {
        val preferences = context.eventDataStore.data.first()
        val connected = when (type) {
            DeviceEventType.CHARGING_STARTED -> true
            DeviceEventType.CHARGING_STOPPED -> false
            else -> null
        }
        if (connected != null && preferences[chargingKey] == connected) return@withLock false
        if (type == DeviceEventType.MOTION && preferences[motionKey]?.let {
                occurredAt.toEpochMilli() - it < 60_000
            } == true) return@withLock false
        val event = DeviceEventRequest(
            eventId = UUID.randomUUID().toString(),
            type = type,
            occurredAt = occurredAt.toString(),
        )
        val events = readUnlocked() + event
        context.eventDataStore.edit {
            it[eventsKey] = json.encodeToString(serializer, events)
            if (connected != null) it[chargingKey] = connected
            if (type == DeviceEventType.MOTION) it[motionKey] = occurredAt.toEpochMilli()
        }
        true
    }

    suspend fun peek(limit: Int = 500): List<DeviceEventRequest> = mutex.withLock {
        readUnlocked().take(limit)
    }

    suspend fun remove(eventIds: Set<String>) = mutex.withLock {
        writeUnlocked(readUnlocked().filterNot { it.eventId in eventIds })
    }

    suspend fun enqueueClassification(request: SleepClassificationRequest) = mutex.withLock {
        writeClassificationsUnlocked((readClassificationsUnlocked() + request).distinct())
    }

    suspend fun peekClassifications(): List<SleepClassificationRequest> = mutex.withLock {
        readClassificationsUnlocked().take(100)
    }

    suspend fun removeClassification(request: SleepClassificationRequest) = mutex.withLock {
        writeClassificationsUnlocked(readClassificationsUnlocked().filterNot { it == request })
    }

    private suspend fun readClassificationsUnlocked(): List<SleepClassificationRequest> {
        val raw = context.eventDataStore.data.first()[classificationsKey] ?: return emptyList()
        return json.decodeFromString(classificationSerializer, raw)
    }

    private suspend fun writeClassificationsUnlocked(events: List<SleepClassificationRequest>) {
        context.eventDataStore.edit { it[classificationsKey] = json.encodeToString(classificationSerializer, events) }
    }

    suspend fun clear() = mutex.withLock {
        writeUnlocked(emptyList())
        writeClassificationsUnlocked(emptyList())
        context.eventDataStore.edit { it.remove(chargingKey); it.remove(motionKey) }
    }

    suspend fun count(): Int = mutex.withLock { readUnlocked().size + readClassificationsUnlocked().size }

    private suspend fun readUnlocked(): List<DeviceEventRequest> {
        val raw = context.eventDataStore.data.first()[eventsKey] ?: return emptyList()
        return json.decodeFromString(serializer, raw)
    }

    private suspend fun writeUnlocked(events: List<DeviceEventRequest>) {
        context.eventDataStore.edit { it[eventsKey] = json.encodeToString(serializer, events) }
    }
}
