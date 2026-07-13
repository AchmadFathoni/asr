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
        val newDone = !entity.isDone

        taskDao.upsertTask(entity.copy(isDone = newDone))

        if (newDone) {
            completeDescendants(entity.id)
            entity.parentId?.let { parentId ->
                val parent = taskDao.getTaskById(parentId) ?: return@let
                val siblings = taskDao.getSubTasks(parentId)
                if (siblings.all { it.isDone }) {
                    taskDao.upsertTask(parent.copy(isDone = true))
                }
            }
        } else {
            uncompleteDescendants(entity.id)
            entity.parentId?.let { parentId ->
                val parent = taskDao.getTaskById(parentId) ?: return@let
                if (parent.isDone) {
                    taskDao.upsertTask(parent.copy(isDone = false))
                }
            }
        }
    }

    override suspend fun deleteTask(task: Task) {
        deleteRecursive(task.id)
    }

    private suspend fun completeDescendants(parentId: Long) {
        taskDao.getSubTasks(parentId).forEach { sub ->
            taskDao.upsertTask(sub.copy(isDone = true))
            completeDescendants(sub.id)
        }
    }

    private suspend fun uncompleteDescendants(parentId: Long) {
        taskDao.getSubTasks(parentId).forEach { sub ->
            taskDao.upsertTask(sub.copy(isDone = false))
            uncompleteDescendants(sub.id)
        }
    }

    private suspend fun deleteRecursive(id: Long) {
        taskDao.getSubTasks(id).forEach { deleteRecursive(it.id) }
        taskDao.deleteTask(id)
    }

    override suspend fun deleteDoneTasks() =
        taskDao.deleteDoneTasks()

    override suspend fun insertAll(tasks: List<Task>) =
        taskDao.insertAll(tasks.map { it.toEntity() })

    private fun TaskEntity.toDomain() = Task(
        id = id,
        title = title,
        description = description,
        isDone = isDone,
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
        dueDate = dueDate?.let { Converters.dateToTimestamp(it) },
        parentId = parentId,
        order = order,
        reminderTime = reminderTime,
    )
}
