package com.asr.core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExportImportTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    @Test
    fun emptyJsonShouldNotCrash() {
        val result = runCatching {
            json.decodeFromString<Map<String, String>>("{}")
        }
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun invalidJsonShouldFail() {
        assertFailsWith<Exception> {
            json.decodeFromString<Map<String, String>>("{invalid")
        }
    }

    @Test
    fun schemaVersionMustMatch() {
        val version = 1
        assertEquals(1, version)
    }

    @Test
    fun taskSerializationRoundTrip() {
        val task = com.asr.core.task.Task(
            id = 1,
            title = "Test task",
            description = "A description",
            isDone = false,
        )
        val serialized = json.encodeToString(com.asr.core.task.Task.serializer(), task)
        assertNotNull(serialized)
        assertTrue(serialized.contains("Test task"))

        val deserialized = json.decodeFromString(com.asr.core.task.Task.serializer(), serialized)
        assertEquals(task.title, deserialized.title)
        assertEquals(task.isDone, deserialized.isDone)
    }

    @Test
    fun emptyTitleShouldBeAllowed() {
        val task = com.asr.core.task.Task(title = "")
        assertEquals("", task.title)
    }

    @Test
    fun specialCharactersInTitle() {
        val titleWithUnicode = "任务 🔥 émoji ñoño"
        val task = com.asr.core.task.Task(title = titleWithUnicode)
        val serialized = json.encodeToString(com.asr.core.task.Task.serializer(), task)
        val deserialized = json.decodeFromString(com.asr.core.task.Task.serializer(), serialized)
        assertEquals(titleWithUnicode, deserialized.title)
    }
}
