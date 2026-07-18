package com.asr.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class ExportSchema(
    val schemaVersion: Int = CURRENT_VERSION,
    val exportedAt: String,
    val tasks: List<TaskSchema>,
    val habits: List<HabitSchema>,
    val habitRecords: List<HabitRecordSchema>,
    val tags: List<TagSchema>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class TaskSchema(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val parentId: Long? = null,
    val isPinned: Boolean = false,
    val reminderTime: String? = null,
)

@Serializable
data class HabitSchema(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequencyType: String = "DAILY",
    val frequencyCount: Int = 1,
    val daysOfWeek: Set<Int> = emptySet(),
    val daysOfMonth: Set<Int> = emptySet(),
    val yearlyDates: Set<Int> = emptySet(),
    val isPinned: Boolean = false,
    val reminderTime: String? = null,
)

@Serializable
data class HabitRecordSchema(
    val id: Long = 0,
    val habitId: Long,
    val date: Long,
    val state: String = "NOT_DONE",
    val count: Int = 0,
)

@Serializable
data class TagSchema(
    val id: Long = 0,
    val name: String,
    val color: Long? = null,
)
