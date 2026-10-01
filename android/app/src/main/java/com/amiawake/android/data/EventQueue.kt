package com.amiawake.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
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

    suspend fun enqueue(type: DeviceEventType) = mutex.withLock {
        val event = DeviceEventRequest(
            eventId = UUID.randomUUID().toString(),
            type = type,
            occurredAt = Instant.now().toString(),
        )
        val events = readUnlocked() + event
        writeUnlocked(events)
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
