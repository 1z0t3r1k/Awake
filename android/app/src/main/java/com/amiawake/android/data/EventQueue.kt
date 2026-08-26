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

    suspend fun count(): Int = mutex.withLock { readUnlocked().size }

    private suspend fun readUnlocked(): List<DeviceEventRequest> {
        val raw = context.eventDataStore.data.first()[eventsKey] ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    private suspend fun writeUnlocked(events: List<DeviceEventRequest>) {
        context.eventDataStore.edit { it[eventsKey] = json.encodeToString(serializer, events) }
    }
}
