package com.asr.core.task

import com.asr.core.interfaces.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SharedTaskRepo(
    private val storage: TaskStorage,
    private val widgetUpdater: WidgetUpdater,
) : TaskRepo {
    override fun getTasksFlow(): Flow<List<Task>> = storage.observeAll()

    override fun getUndoneTasksFlow(): Flow<List<Task>> =
        storage.observeAll().map { it.filter { t -> !t.isDone } }

    override fun getDoneTasksFlow(): Flow<List<Task>> =
        storage.observeAll().map { it.filter { t -> t.isDone } }

    override suspend fun getTaskById(id: Long): Task? = storage.getById(id)

    override suspend fun getSubTasks(parentId: Long): List<Task> = storage.getByParent(parentId)

    override suspend fun upsertTask(task: Task): Long {
        val id = storage.upsert(task)
        task.parentId?.let { parentId -> uncompleteAncestors(parentId) }
        widgetUpdater.notifyDataChanged()
        return id
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
            uncompleteAncestors(current.parentId)
        }
        widgetUpdater.notifyDataChanged()
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

    private suspend fun uncompleteAncestors(startParentId: Long?) {
        var current = startParentId
        while (current != null) {
            val node = storage.getById(current) ?: break
            if (storage.getByParent(current).all { it.isDone }) break
            if (node.isDone) {
                storage.upsert(node.copy(isDone = false))
            }
            current = node.parentId
        }
    }

    override suspend fun deleteTask(task: Task) {
        deleteRecursive(task.id)
        widgetUpdater.notifyDataChanged()
    }

    private suspend fun deleteRecursive(id: Long) {
        storage.getByParent(id).forEach { deleteRecursive(it.id) }
        storage.delete(id)
    }

    override suspend fun deleteDoneTasks() {
        storage.observeAll().first().filter { it.isDone }.map { it.id }.toSet().forEach { deleteRecursive(it) }
        widgetUpdater.notifyDataChanged()
    }

    override suspend fun insertAll(tasks: List<Task>) {
        storage.replaceAll(tasks)
        widgetUpdater.notifyDataChanged()
    }
}
