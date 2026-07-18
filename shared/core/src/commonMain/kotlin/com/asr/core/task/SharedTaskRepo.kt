package com.asr.core.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SharedTaskRepo(private val storage: TaskStorage) : TaskRepo {
    override fun getTasksFlow(): Flow<List<Task>> = storage.observeAll()

    override fun getUndoneTasksFlow(): Flow<List<Task>> =
        storage.observeAll().map { it.filter { t -> !t.isDone } }

    override fun getDoneTasksFlow(): Flow<List<Task>> =
        storage.observeAll().map { it.filter { t -> t.isDone } }

    override suspend fun getTaskById(id: Long): Task? = storage.getById(id)

    override suspend fun getSubTasks(parentId: Long): List<Task> = storage.getByParent(parentId)

    override suspend fun upsertTask(task: Task): Long {
        val updated = if (task.parentId != null) {
            val parent = storage.getById(task.parentId)
            if (parent?.isDone == true) task.copy(isDone = true) else task
        } else task
        return storage.upsert(updated)
    }

    override suspend fun toggleTask(id: Long) {
        val current = storage.getById(id) ?: return
        val newDone = !current.isDone
        storage.upsert(current.copy(isDone = newDone))
        if (newDone) {
            completeDescendants(id)
            current.parentId?.let { parentId ->
                val parent = storage.getById(parentId) ?: return@let
                val siblings = storage.getByParent(parentId)
                if (siblings.all { it.isDone }) {
                    storage.upsert(parent.copy(isDone = true))
                }
            }
        } else {
            uncompleteDescendants(id)
            current.parentId?.let { parentId ->
                val parent = storage.getById(parentId) ?: return@let
                if (parent.isDone) {
                    storage.upsert(parent.copy(isDone = false))
                }
            }
        }
    }

    private suspend fun completeDescendants(parentId: Long) {
        storage.getByParent(parentId).filter { !it.isDone }.forEach { sub ->
            storage.upsert(sub.copy(isDone = true))
            completeDescendants(sub.id)
        }
    }

    private suspend fun uncompleteDescendants(parentId: Long) {
        storage.getByParent(parentId).filter { it.isDone }.forEach { sub ->
            storage.upsert(sub.copy(isDone = false))
            uncompleteDescendants(sub.id)
        }
    }

    override suspend fun deleteTask(task: Task) {
        deleteRecursive(task.id)
    }

    private suspend fun deleteRecursive(id: Long) {
        storage.getByParent(id).forEach { deleteRecursive(it.id) }
        storage.delete(id)
    }

    override suspend fun deleteDoneTasks() {
        storage.observeAll().first().filter { it.isDone }.map { it.id }.toSet().forEach { deleteRecursive(it) }
    }

    override suspend fun insertAll(tasks: List<Task>) = storage.replaceAll(tasks)
}
