package com.asr.core

import com.asr.core.task.Task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubTaskTest {

    private fun getDescendantIds(tasks: List<Task>, parentId: Long): Set<Long> {
        val children = tasks.filter { it.parentId == parentId }.map { it.id }
        return (children + children.flatMap { getDescendantIds(tasks, it) }).toSet()
    }

    private fun cascadeDelete(tasks: List<Task>, id: Long): List<Task> {
        val ids = getDescendantIds(tasks, id) + id
        return tasks.filter { it.id !in ids }
    }

    private fun toggleTask(tasks: List<Task>, id: Long): List<Task> {
        val task = tasks.find { it.id == id } ?: return tasks
        val newDone = !task.isDone
        val descendantIds = getDescendantIds(tasks, id)
        val updated = tasks.map { t ->
            when {
                t.id == id || t.id in descendantIds -> t.copy(isDone = newDone)
                else -> t
            }
        }
        if (!newDone && task.parentId != null) {
            val parentIdx = updated.indexOfFirst { it.id == task.parentId }
            if (parentIdx >= 0) updated[parentIdx].copy(isDone = false).also {
                return updated.toMutableList().apply { this[parentIdx] = it }
            }
        }
        if (newDone && task.parentId != null) {
            val siblings = updated.filter { it.parentId == task.parentId }
            if (siblings.all { it.isDone }) {
                val parentIdx = updated.indexOfFirst { it.id == task.parentId }
                if (parentIdx >= 0) updated[parentIdx].copy(isDone = true).also {
                    return updated.toMutableList().apply { this[parentIdx] = it }
                }
            }
        }
        return updated
    }

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

    // ── Cascade delete ──

    @Test
    fun cascadeDeleteRemovesParentAndChildren() {
        val tasks = listOf(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C", parentId = 1),
            Task(id = 3, title = "U"),
        )
        val result = cascadeDelete(tasks, 1)
        assertEquals(1, result.size)
        assertEquals("U", result[0].title)
    }

    @Test
    fun cascadeDeleteRemovesDescendantsRecursively() {
        val tasks = listOf(
            Task(id = 1, title = "P"),
            Task(id = 2, title = "C1", parentId = 1),
            Task(id = 3, title = "C2", parentId = 2),
            Task(id = 4, title = "C3", parentId = 3),
            Task(id = 5, title = "U"),
        )
        val result = cascadeDelete(tasks, 1)
        assertEquals(1, result.size)
        assertEquals("U", result[0].title)
    }

    @Test
    fun cascadeDeleteSingleTaskNoChildren() {
        val tasks = listOf(Task(id = 1, title = "S"))
        assertTrue(cascadeDelete(tasks, 1).isEmpty())
    }

    @Test
    fun cascadeDeleteKeepsUnrelatedTasks() {
        val tasks = listOf(
            Task(id = 1, title = "P", parentId = null),
            Task(id = 2, title = "C", parentId = 1),
            Task(id = 3, title = "O", parentId = null),
            Task(id = 4, title = "OC", parentId = 3),
        )
        val result = cascadeDelete(tasks, 1)
        assertEquals(2, result.size)
        assertEquals("O", result.first { it.id == 3L }.title)
        assertEquals("OC", result.first { it.id == 4L }.title)
    }

    // ── Auto-complete: parent done → descendants done ──

    @Test
    fun completingParentCompletesAllDescendants() {
        val tasks = listOf(
            Task(id = 1, title = "P", isDone = false),
            Task(id = 2, title = "C1", parentId = 1, isDone = false),
            Task(id = 3, title = "C2", parentId = 1, isDone = false),
        )
        val result = toggleTask(tasks, 1)
        assertTrue(result.first { it.id == 1L }.isDone)
        assertTrue(result.first { it.id == 2L }.isDone)
        assertTrue(result.first { it.id == 3L }.isDone)
    }

    @Test
    fun completingParentRecursivelyCompletesDeepDescendants() {
        val tasks = listOf(
            Task(id = 1, title = "P", isDone = false),
            Task(id = 2, title = "C", parentId = 1, isDone = false),
            Task(id = 3, title = "GC", parentId = 2, isDone = false),
        )
        val result = toggleTask(tasks, 1)
        assertTrue(result.first { it.id == 3L }.isDone)
    }

    // ── Auto-complete: parent undone → descendants undone ──

    @Test
    fun uncompletingParentUncompletesDescendants() {
        val tasks = listOf(
            Task(id = 1, title = "P", isDone = true),
            Task(id = 2, title = "C", parentId = 1, isDone = true),
        )
        val result = toggleTask(tasks, 1)
        assertFalse(result.first { it.id == 1L }.isDone)
        assertFalse(result.first { it.id == 2L }.isDone)
    }

    // ── Auto-complete: last subtask done → parent done ──

    @Test
    fun completingLastSubtaskCompletesParent() {
        val tasks = listOf(
            Task(id = 1, title = "P", isDone = false),
            Task(id = 2, title = "C1", parentId = 1, isDone = true),
            Task(id = 3, title = "C2", parentId = 1, isDone = false),
        )
        val result = toggleTask(tasks, 3)
        assertTrue(result.first { it.id == 3L }.isDone)
        assertTrue(result.first { it.id == 1L }.isDone)
    }

    @Test
    fun completingSubtaskDoesNotCompleteParentWhenSiblingsRemain() {
        val tasks = listOf(
            Task(id = 1, title = "P", isDone = false),
            Task(id = 2, title = "C1", parentId = 1, isDone = false),
            Task(id = 3, title = "C2", parentId = 1, isDone = false),
        )
        val result = toggleTask(tasks, 2)
        assertTrue(result.first { it.id == 2L }.isDone)
        assertFalse(result.first { it.id == 1L }.isDone)
    }

    @Test
    fun uncompletingSubtaskUncompletesParent() {
        val tasks = listOf(
            Task(id = 1, title = "P", isDone = true),
            Task(id = 2, title = "C", parentId = 1, isDone = true),
        )
        val result = toggleTask(tasks, 2)
        assertFalse(result.first { it.id == 2L }.isDone)
        assertFalse(result.first { it.id == 1L }.isDone)
    }

    // ── buildFlatList ──

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
