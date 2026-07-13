package com.asr.core.task

import kotlinx.coroutines.flow.Flow

interface TaskRepo {
    fun getTasksFlow(): Flow<List<Task>>

    fun getUndoneTasksFlow(): Flow<List<Task>>

    fun getDoneTasksFlow(): Flow<List<Task>>

    suspend fun getTaskById(id: Long): Task?

    suspend fun getSubTasks(parentId: Long): List<Task>

    suspend fun upsertTask(task: Task): Long

    suspend fun toggleTask(id: Long)

    suspend fun deleteTask(task: Task)

    suspend fun deleteDoneTasks()

    suspend fun insertAll(tasks: List<Task>)
}
