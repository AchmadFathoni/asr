package com.asr.core

import com.asr.core.task.SharedTaskRepo
import com.asr.core.task.Task
import com.asr.core.task.TaskStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubTaskTest {

    // ── Cascade delete via SharedTaskRepo ──

    @Test
    fun cascadeDeleteRemovesParentAndChildren() = runBlocking {
        val repo = repoWith(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
            Task(id = 3, title = "U"),
        )
        repo.deleteTask(Task(id = 1, title = ""))
        assertTrue(repo.getTaskById(1) == null)
        assertTrue(repo.getTaskById(2) == null)
        assertTrue(repo.getTaskById(3) != null)
    }

    @Test
    fun cascadeDeleteRemovesDescendantsRecursively() = runBlocking {
        val repo = repoWith(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C1", parentId = 1),
            Task(id = 3, title = "C2", parentId = 2),
            Task(id = 4, title = "C3", parentId = 3),
            Task(id = 5, title = "U"),
        )
        repo.deleteTask(Task(id = 1, title = ""))
        assertTrue(repo.getTaskById(1) == null)
        assertTrue(repo.getTaskById(2) == null)
        assertTrue(repo.getTaskById(3) == null)
        assertTrue(repo.getTaskById(4) == null)
        assertTrue(repo.getTaskById(5) != null)
    }

    @Test
    fun cascadeDeleteSingleTaskNoChildren() = runBlocking {
        val repo = repoWith(Task(id = 1, title = "S"))
        repo.deleteTask(Task(id = 1, title = ""))
        assertTrue(repo.getTaskById(1) == null)
    }

    @Test
    fun cascadeDeleteKeepsUnrelatedTasks() = runBlocking {
        val repo = repoWith(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
            Task(id = 3, title = "O"),
            Task(id = 4, title = "OC", parentId = 3),
        )
        repo.deleteTask(Task(id = 1, title = ""))
        assertTrue(repo.getTaskById(1) == null)
        assertTrue(repo.getTaskById(2) == null)
        assertTrue(repo.getTaskById(3) != null)
        assertTrue(repo.getTaskById(4) != null)
    }

    // ── buildFlatList (UI-level, kept as-is) ──

    private fun buildFlatList(
        tasks: List<Task>,
        expandedIds: Set<Long>,
    ): List<Task> {
        val subTaskMap = tasks.filter { it.parentId != null }.groupBy { it.parentId!! }
        fun recurse(items: List<Task>): List<Task> {
            val result = mutableListOf<Task>()
            for (task in items) {
                result.add(task)
                if (task.id in expandedIds) {
                    result.addAll(recurse(subTaskMap[task.id].orEmpty()))
                }
            }
            return result
        }
        return recurse(tasks.filter { it.parentId == null })
    }

    @Test
    fun flatListShowsTopLevelWhenNothingExpanded() {
        val tasks = listOf(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
        )
        val flat = buildFlatList(tasks, emptySet())
        assertEquals(1, flat.size)
        assertEquals("P", flat[0].title)
    }

    @Test
    fun flatListShowsChildrenWhenExpanded() {
        val tasks = listOf(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
        )
        val flat = buildFlatList(tasks, setOf(1))
        assertEquals(2, flat.size)
        assertEquals("P", flat[0].title)
        assertEquals("C", flat[1].title)
    }

    @Test
    fun flatListRecursiveExpand() {
        val tasks = listOf(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
            Task(id = 3, title = "GC", parentId = 2),
        )
        val flat = buildFlatList(tasks, setOf(1, 2))
        assertEquals(3, flat.size)
        assertEquals("GC", flat[2].title)
    }

    @Test
    fun flatListPartialExpand() {
        val tasks = listOf(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
            Task(id = 3, title = "GC", parentId = 2),
        )
        val flat = buildFlatList(tasks, setOf(1))
        assertEquals(2, flat.size)
    }

    @Test
    fun flatListEmpty() {
        assertTrue(buildFlatList(emptyList(), emptySet()).isEmpty())
    }

    @Test
    fun flatListNoChildren() {
        val tasks = listOf(
            Task(id = 1, title = "A"),
            Task(id = 2, title = "B"),
        )
        val flat = buildFlatList(tasks, emptySet())
        assertEquals(2, flat.size)
    }
}

private fun repoWith(vararg tasks: Task): SharedTaskRepo {
    val list = tasks.toMutableList()
    val flow = MutableStateFlow(list.toList())
    val storage = object : TaskStorage {
        override fun observeAll() = flow
        override suspend fun getById(id: Long) = list.find { it.id == id }
        override suspend fun getByParent(parentId: Long) = list.filter { it.parentId == parentId }
        override suspend fun upsert(task: Task): Long {
            val id = if (task.id == 0L) (list.maxOfOrNull { it.id } ?: 0) + 1 else task.id
            list.removeAll { it.id == id }
            list.add(task.copy(id = id))
            flow.value = list.toList()
            return id
        }
        override suspend fun delete(id: Long) { list.removeAll { it.id == id }; flow.value = list.toList() }
        override suspend fun deleteDone() { list.removeAll { it.isDone }; flow.value = list.toList() }
        override suspend fun replaceAll(newTasks: List<Task>) { list.clear(); list.addAll(newTasks); flow.value = list.toList() }
    }
    return SharedTaskRepo(storage)
}
