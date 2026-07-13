package com.asr.data.backup.restore

import com.asr.core.backup.RestoreFailedException
import com.asr.core.backup.RestoreRepo
import com.asr.core.backup.RestoreResult
import com.asr.data.backup.ExportSchema
import com.asr.data.database.AppDatabase
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [RestoreRepo::class])
class RestoreImpl(private val db: AppDatabase) : RestoreRepo {

    override suspend fun restoreData(): RestoreResult {
        return try {
            val file = FileKit.openFilePicker(
                mode = FileKitMode.Single,
                type = FileKitType.File("json"),
            ) ?: return RestoreResult.Failure(RestoreFailedException.InvalidFile)

            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            val content = file.readString()
            val schema = try {
                json.decodeFromString(ExportSchema.serializer(), content)
            } catch (e: SerializationException) {
                return RestoreResult.Failure(RestoreFailedException.InvalidFile)
            }

            if (schema.schemaVersion != AppDatabase.SCHEMA_VERSION) {
                return RestoreResult.Failure(RestoreFailedException.OldSchema)
            }

            coroutineScope {
                val deleteAndInsertTasks = async {
                    db.taskDao().deleteAllTasks()
                    db.taskDao().insertAll(schema.tasks.map { it.toEntity() })
                }
                val deleteAndInsertHabits = async {
                    db.habitDao().deleteAllHabits()
                    db.habitDao().deleteAllRecords()
                    db.habitDao().insertAllHabits(schema.habits.map { it.toEntity() })
                    db.habitDao().insertAllRecords(schema.habitRecords.map { it.toEntity() })
                }
                val deleteAndInsertTags = async {
                    db.tagDao().deleteAllTags()
                    db.tagDao().insertAllTags(schema.tags.map { it.toEntity() })
                }
                awaitAll(deleteAndInsertTasks, deleteAndInsertHabits, deleteAndInsertTags)
            }

            RestoreResult.Success
        } catch (e: Exception) {
            RestoreResult.Failure(RestoreFailedException.InvalidFile)
        }
    }
}

private fun com.asr.data.backup.TaskSchema.toEntity() =
    com.asr.data.database.TaskEntity(
        id = id, title = title, description = description, isDone = isDone,
        doneAt = doneAt, parentId = parentId, order = order, reminderTime = reminderTime,
    )

private fun com.asr.data.backup.HabitSchema.toEntity() =
    com.asr.data.database.HabitEntity(
        id = id, title = title, description = description,
        frequencyType = frequencyType, frequencyCount = frequencyCount,
        order = order, reminderTime = reminderTime,
    )

private fun com.asr.data.backup.HabitRecordSchema.toEntity() =
    com.asr.data.database.HabitRecordEntity(
        id = id, habitId = habitId, date = date, state = state,
        count = count, doneAt = doneAt,
    )

private fun com.asr.data.backup.TagSchema.toEntity() =
    com.asr.data.database.TagEntity(
        id = id, name = name, color = color,
    )
