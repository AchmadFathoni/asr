package com.asr.data.repository

import com.asr.core.now
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import com.asr.data.database.Converters
import com.asr.data.database.TaskDao
import com.asr.data.database.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single

@Single(binds = [TaskRepo::class])
class TaskRepository(private val taskDao: TaskDao) : TaskRepo {

    override fun getTasksFlow(): Flow<List<Task>> =
        taskDao.getAllTasksFlow().map { it.map { e -> e.toDomain() } }

    override fun getUndoneTasksFlow(): Flow<List<Task>> =
        taskDao.getUndoneTasksFlow().map { it.map { e -> e.toDomain() } }

    override fun getDoneTasksFlow(): Flow<List<Task>> =
        taskDao.getDoneTasksFlow().map { it.map { e -> e.toDomain() } }

    override suspend fun getTaskById(id: Long): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun getSubTasks(parentId: Long): List<Task> =
        taskDao.getSubTasks(parentId).map { it.toDomain() }

    override suspend fun upsertTask(task: Task): Long {
        val result = taskDao.upsertTask(task.toEntity())
        return if (task.id == 0L) result else task.id
    }

    override suspend fun toggleTask(id: Long) {
        val entity = taskDao.getTaskById(id) ?: return
        val now = LocalDateTime.now()
        taskDao.upsertTask(
            entity.copy(
                isDone = !entity.isDone,
                doneAt = if (!entity.isDone) {
                    now.toInstant(TimeZone.currentSystemDefault()).epochSeconds
                } else null,
            )
        )
    }

    override suspend fun deleteTask(task: Task) =
        taskDao.deleteTask(task.id)

    override suspend fun deleteDoneTasks() =
        taskDao.deleteDoneTasks()

    override suspend fun insertAll(tasks: List<Task>) =
        taskDao.insertAll(tasks.map { it.toEntity() })

    private fun TaskEntity.toDomain() = Task(
        id = id,
        title = title,
        description = description,
        isDone = isDone,
        doneAt = Converters.dateTimeFromTimestamp(doneAt),
        dueDate = dueDate?.let { Converters.dateFromTimestamp(it) },
        parentId = parentId,
        order = order,
        reminderTime = reminderTime,
    )

    private fun Task.toEntity() = TaskEntity(
        id = id,
        title = title,
        description = description,
        isDone = isDone,
        doneAt = Converters.dateTimeToTimestamp(doneAt),
        dueDate = dueDate?.let { Converters.dateToTimestamp(it) },
        parentId = parentId,
        order = order,
        reminderTime = reminderTime,
    )
}
