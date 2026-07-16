package com.asr.core.task

import kotlinx.coroutines.flow.Flow

interface TaskStorage {
    fun observeAll(): Flow<List<Task>>
    suspend fun getById(id: Long): Task?
    suspend fun getByParent(parentId: Long): List<Task>
    suspend fun upsert(task: Task): Long
    suspend fun delete(id: Long)
    suspend fun deleteDone()
    suspend fun replaceAll(tasks: List<Task>)
}
