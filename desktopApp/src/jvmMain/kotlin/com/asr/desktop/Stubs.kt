package com.asr.desktop

import com.asr.core.backup.ExportRepo
import com.asr.core.backup.RestoreFailedException
import com.asr.core.backup.RestoreRepo
import com.asr.core.backup.RestoreResult
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.File

@Serializable
data class PersistedData(
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val records: List<HabitRecord> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val theme: String? = null,
    val punishmentDate: String? = null,
    val taskTags: Map<Long, List<Long>> = emptyMap(),
    val habitTags: Map<Long, List<Long>> = emptyMap(),
)

object DataStore {
    private val lock = Any()
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dir: File by lazy { File(System.getProperty("user.home"), ".asr").also { it.mkdirs() } }
    private val file: File by lazy { File(dir, "data.json") }

    var data: PersistedData = PersistedData()
        private set

    init {
        try {
            val f = File(File(System.getProperty("user.home"), ".asr"), "data.json")
            if (f.exists()) data = json.decodeFromString(f.readText())
        } catch (_: Exception) { /* use defaults */ }
    }

    fun update(block: (PersistedData) -> PersistedData) {
        synchronized(lock) {
            data = block(data)
            dir.mkdirs()
            file.writeText(json.encodeToString(data))
        }
    }

    fun restore(schema: PersistedData) { update { schema } }
}

@Single(binds = [ExportRepo::class])
class ExportRepoStub : ExportRepo {
    override suspend fun exportToJson() {
        val exportDir = File(System.getProperty("user.home"), ".asr/exports")
        exportDir.mkdirs()
        val snapshot = DataStore.data
        File(exportDir, "ASR-Export-${System.currentTimeMillis()}.json").writeText(DataStore.json.encodeToString(snapshot))
    }
}

@Single(binds = [RestoreRepo::class])
class RestoreRepoStub(
    private val taskRepo: TaskRepo,
    private val habitRepo: HabitRepo,
    private val tagRepo: TagRepo,
) : RestoreRepo {
    override suspend fun restoreData(): RestoreResult {
        val exportDir = File(System.getProperty("user.home"), ".asr/exports")
        val files = exportDir.listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.lastModified() }
        val latest = files?.firstOrNull() ?: return RestoreResult.Failure(RestoreFailedException.InvalidFile)
        return try {
            val schema = DataStore.json.decodeFromString(PersistedData.serializer(), latest.readText())
            taskRepo.insertAll(schema.tasks)
            habitRepo.insertAll(schema.habits, schema.records)
            tagRepo.insertAll(schema.tags)
            for ((taskId, tagIds) in schema.taskTags) tagRepo.setTagsForTask(taskId, tagIds)
            for ((habitId, tagIds) in schema.habitTags) tagRepo.setTagsForHabit(habitId, tagIds)
            DataStore.restore(schema)
            RestoreResult.Success
        } catch (_: Exception) { RestoreResult.Failure(RestoreFailedException.InvalidFile) }
    }
}

@Single(binds = [AlarmScheduler::class])
class AlarmSchedulerStub : AlarmScheduler {
    override fun schedule(habit: Habit) {}
    override fun schedule(task: Task) {}
    override fun cancel(habit: Habit) {}
    override fun cancel(task: Task) {}
    override fun cancelAll() {}
}
