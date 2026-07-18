package com.asr.data.backup.export

import com.asr.core.backup.ExportRepo
import com.asr.data.backup.ExportSchema
import com.asr.data.backup.HabitRecordSchema
import com.asr.data.backup.HabitSchema
import com.asr.data.backup.TagSchema
import com.asr.data.backup.TaskSchema
import com.asr.data.database.AppDatabase
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [ExportRepo::class])
class ExportImpl(private val db: AppDatabase) : ExportRepo {

    private companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }

    override suspend fun exportToJson() {
        coroutineScope {
            val tasksDeferred = async { db.taskDao().getAllTasks() }
            val habitsDeferred = async { db.habitDao().getAllHabits() }
            val recordsDeferred = async { db.habitDao().getAllRecords() }
            val tagsDeferred = async { db.tagDao().getAllTags() }

            val tasks = tasksDeferred.await()
            val habits = habitsDeferred.await()
            val records = recordsDeferred.await()
            val tags = tagsDeferred.await()

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            val schema = ExportSchema(
                exportedAt = now.toString(),
                tasks = tasks.map { it.toSchema() },
                habits = habits.map { it.toSchema() },
                habitRecords = records.map { it.toSchema() },
                tags = tags.map { it.toSchema() },
            )

            val jsonString = json.encodeToString(ExportSchema.serializer(), schema)

            val timestamp = "${now.year}${now.month.toString().padStart(2, '0')}" +
                "${now.day.toString().padStart(2, '0')}" +
                "${now.hour.toString().padStart(2, '0')}" +
                "${now.minute.toString().padStart(2, '0')}" +
                "${now.second.toString().padStart(2, '0')}"

            val file = FileKit.openFileSaver(
                suggestedName = "ASR-Export-$timestamp",
                defaultExtension = "json",
            )

            file?.writeString(jsonString)
        }
    }
}

private fun com.asr.data.database.TaskEntity.toSchema() = TaskSchema(
    id = id, title = title, description = description, isDone = isDone,
    parentId = parentId, isPinned = isPinned, reminderTime = reminderTime,
)

private fun com.asr.data.database.HabitEntity.toSchema() = HabitSchema(
    id = id, title = title, description = description,
    frequencyType = frequencyType, frequencyCount = frequencyCount,
    daysOfWeek = if (daysOfWeek.isNotBlank()) daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
    daysOfMonth = if (daysOfMonth.isNotBlank()) daysOfMonth.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
    yearlyDates = if (yearlyDates.isNotBlank()) yearlyDates.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
    isPinned = isPinned, reminderTime = reminderTime,
)

private fun com.asr.data.database.HabitRecordEntity.toSchema() = HabitRecordSchema(
    id = id, habitId = habitId, date = date, state = state,
    count = count,
)

private fun com.asr.data.database.TagEntity.toSchema() = TagSchema(
    id = id, name = name, color = color,
)
