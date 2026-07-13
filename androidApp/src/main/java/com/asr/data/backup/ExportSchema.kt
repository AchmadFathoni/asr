package com.asr.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class ExportSchema(
    val schemaVersion: Int = 1,
    val exportedAt: String,
    val tasks: List<TaskSchema>,
    val habits: List<HabitSchema>,
    val habitRecords: List<HabitRecordSchema>,
    val tags: List<TagSchema>,
)

@Serializable
data class TaskSchema(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val parentId: Long? = null,
    val order: Int = 0,
    val reminderTime: String? = null,
)

@Serializable
data class HabitSchema(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequencyType: String = "DAILY",
    val frequencyCount: Int = 1,
    val order: Int = 0,
    val reminderTime: String? = null,
)

@Serializable
data class HabitRecordSchema(
    val id: Long = 0,
    val habitId: Long,
    val date: Long,
    val state: String = "NOT_DONE",
    val count: Int = 0,
    val doneAt: Long? = null,
)

@Serializable
data class TagSchema(
    val id: Long = 0,
    val name: String,
    val color: Long? = null,
)
