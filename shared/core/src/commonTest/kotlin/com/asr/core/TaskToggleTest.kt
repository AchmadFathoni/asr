package com.asr.core

import com.asr.core.interfaces.WidgetUpdater
import com.asr.core.task.SharedTaskRepo
import com.asr.core.task.Task
import com.asr.core.task.TaskStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskToggleTest {
    private fun runTest(block: suspend SharedTaskRepo.() -> Unit) {
        val store = InMemoryTaskStorage(mutableListOf())
        val repo = SharedTaskRepo(store, object : WidgetUpdater { override fun notifyDataChanged() {} })
        runBlocking { block(repo) }
    }

    @Test
    fun `toggling the last undone subtask marks parent done`() = runTest {
        val init = listOf(
            Task(id = 1, title = "Parent"),
            Task(id = 2, title = "Child A", parentId = 1, isDone = true),
            Task(id = 3, title = "Child B", parentId = 1),
        )
        insertAll(init)

        toggleTask(3)
        assertTrue(getTaskById(1)!!.isDone)
        assertTrue(getTaskById(3)!!.isDone)
    }

    @Test
    fun `toggling a subtask does not complete parent when siblings remain undone`() = runTest {
        val init = listOf(
            Task(id = 1, title = "Parent"),
            Task(id = 2, title = "Child A", parentId = 1),
            Task(id = 3, title = "Child B", parentId = 1),
        )
        insertAll(init)

        toggleTask(2)
        assertTrue(getTaskById(2)!!.isDone)
        assertFalse(getTaskById(1)!!.isDone)
    }

    @Test
    fun `uncompleting a subtask uncompletes the parent`() = runTest {
        val init = listOf(
            Task(id = 1, title = "Parent", isDone = true),
            Task(id = 2, title = "Child", parentId = 1, isDone = true),
        )
        insertAll(init)

        toggleTask(2)
        assertFalse(getTaskById(2)!!.isDone)
        assertFalse(getTaskById(1)!!.isDone)
    }

    @Test
    fun `toggling parent done marks all descendants done recursively`() = runTest {
        val init = listOf(
            Task(id = 1, title = "Parent"),
            Task(id = 2, title = "Child", parentId = 1),
            Task(id = 3, title = "Grandchild", parentId = 2),
        )
        insertAll(init)

        toggleTask(1)
        assertTrue(getTaskById(1)!!.isDone)
        assertTrue(getTaskById(2)!!.isDone)
        assertTrue(getTaskById(3)!!.isDone)
    }

    @Test
    fun `toggling parent undone marks all descendants undone recursively`() = runTest {
        val init = listOf(
            Task(id = 1, title = "Parent", isDone = true),
            Task(id = 2, title = "Child", parentId = 1, isDone = true),
            Task(id = 3, title = "Grandchild", parentId = 2, isDone = true),
        )
        insertAll(init)

        toggleTask(1)
        assertFalse(getTaskById(1)!!.isDone)
        assertFalse(getTaskById(2)!!.isDone)
        assertFalse(getTaskById(3)!!.isDone)
    }

    @Test
    fun `double-toggle restores original state`() = runTest {
        val init = listOf(Task(id = 1, title = "Task"))
        insertAll(init)

        toggleTask(1)
        assertTrue(getTaskById(1)!!.isDone)

        toggleTask(1)
        assertFalse(getTaskById(1)!!.isDone)
    }

    @Test
    fun `toggle non-existent id is no-op`() = runTest {
        val init = listOf(Task(id = 1, title = "Task"))
        insertAll(init)

        toggleTask(999)

        assertEquals(1, getTasksFlow().let { (it as MutableStateFlow).value.size })
    }

    @Test
    fun `toggling deepest leaf propagates one level up only`() = runTest {
        val init = listOf(
            Task(id = 1, title = "L1"),
            Task(id = 2, title = "L2", parentId = 1),
            Task(id = 3, title = "L3", parentId = 2),
            Task(id = 4, title = "L4", parentId = 3),
        )
        insertAll(init)

        toggleTask(4)
        assertTrue(getTaskById(4)!!.isDone)
        assertTrue(getTaskById(3)!!.isDone)
        assertFalse(getTaskById(2)!!.isDone) // only direct parent is auto-completed
        assertFalse(getTaskById(1)!!.isDone) // grandparent untouched
    }

    @Test
    fun `toggling leaf propagates one level up to parent but stops there`() = runTest {
        val init = listOf(
            Task(id = 1, title = "L1"),
            Task(id = 2, title = "L2a", parentId = 1, isDone = true),
            Task(id = 3, title = "L2b", parentId = 1),
            Task(id = 4, title = "L3a", parentId = 3, isDone = true),
            Task(id = 5, title = "L3b", parentId = 3),
        )
        insertAll(init)

        toggleTask(5)
        assertTrue(getTaskById(5)!!.isDone)
        assertTrue(getTaskById(3)!!.isDone) // direct parent done
        assertFalse(getTaskById(1)!!.isDone) // grandparent stays undone (L2a is done, but toggleTask only checks direct parent)
    }
}

private class InMemoryTaskStorage(private val tasks: MutableList<Task>) : TaskStorage {
    private val _flow = MutableStateFlow(tasks.toList())

    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Task>> = _flow

    override suspend fun getById(id: Long): Task? = tasks.find { it.id == id }

    override suspend fun getByParent(parentId: Long): List<Task> =
        tasks.filter { it.parentId == parentId }

    override suspend fun upsert(task: Task): Long {
        val id = if (task.id == 0L) (tasks.maxOfOrNull { it.id } ?: 0) + 1 else task.id
        tasks.removeAll { it.id == id }
        tasks.add(task.copy(id = id))
        _flow.value = tasks.toList()
        return id
    }

    override suspend fun delete(id: Long) {
        tasks.removeAll { it.id == id }
        _flow.value = tasks.toList()
    }

    override suspend fun deleteDone() {
        tasks.removeAll { it.isDone }
        _flow.value = tasks.toList()
    }

    override suspend fun replaceAll(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        _flow.value = tasks.toList()
    }
}
