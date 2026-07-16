package com.asr.data.storage

import com.asr.core.task.Task
import com.asr.core.task.TaskStorage
import com.asr.data.database.Converters
import com.asr.data.database.TaskDao
import com.asr.data.database.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskStorage(private val taskDao: TaskDao) : TaskStorage {
    override fun observeAll(): Flow<List<Task>> =
        taskDao.getAllTasksFlow().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: Long): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun getByParent(parentId: Long): List<Task> =
        taskDao.getSubTasks(parentId).map { it.toDomain() }

    override suspend fun upsert(task: Task): Long {
        val result = taskDao.upsertTask(task.toEntity())
        return if (task.id == 0L) result else task.id
    }

    override suspend fun delete(id: Long) = taskDao.deleteTask(id)

    override suspend fun deleteDone() = taskDao.deleteDoneTasks()

    override suspend fun replaceAll(tasks: List<Task>) =
        taskDao.insertAll(tasks.map { it.toEntity() })

    private fun TaskEntity.toDomain() = Task(
        id = id,
        title = title,
        description = description,
        isDone = isDone,
        dueDate = dueDate?.let { Converters.dateFromTimestamp(it) },
        parentId = parentId,
        isPinned = isPinned,
        reminderTime = reminderTime,
    )

    private fun Task.toEntity() = TaskEntity(
        id = id,
        title = title,
        description = description,
        isDone = isDone,
        dueDate = dueDate?.let { Converters.dateToTimestamp(it) },
        parentId = parentId,
        isPinned = isPinned,
        reminderTime = reminderTime,
    )
}
