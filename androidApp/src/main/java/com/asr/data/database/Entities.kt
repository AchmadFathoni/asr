package com.asr.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val dueDate: Long? = null,
    val parentId: Long? = null,
    @ColumnInfo(name = "order_index") val order: Int = 0,
    val reminderTime: String? = null,
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequencyType: String = "DAILY",
    val frequencyCount: Int = 1,
    val dayOfWeek: Int? = null,
    val daysOfWeek: String = "",
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null,
    @ColumnInfo(name = "order_index") val order: Int = 0,
    val reminderTime: String? = null,
)

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        androidx.room3.ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        )
    ],
    indices = [androidx.room3.Index(value = ["habitId"])],
)
data class HabitRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: Long,
    val state: String = "NOT_DONE",
    val count: Int = 0,
    val doneAt: Long? = null,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long? = null,
)

@Entity(
    tableName = "task_tags",
    foreignKeys = [
        androidx.room3.ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        ),
        androidx.room3.ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room3.Index(value = ["taskId"]),
        androidx.room3.Index(value = ["tagId"]),
    ],
    primaryKeys = ["taskId", "tagId"],
)
data class TaskTagEntity(
    val taskId: Long,
    val tagId: Long,
)

@Entity(
    tableName = "habit_tags",
    foreignKeys = [
        androidx.room3.ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        ),
        androidx.room3.ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room3.Index(value = ["habitId"]),
        androidx.room3.Index(value = ["tagId"]),
    ],
    primaryKeys = ["habitId", "tagId"],
)
data class HabitTagEntity(
    val habitId: Long,
    val tagId: Long,
)
