package com.asr.ui.tasks

import com.asr.core.task.Task

internal fun buildFlatList(
    tasks: List<Task>,
    expandedIds: Set<Long>,
): List<Pair<Task, Int>> {
    val subTaskMap = tasks.mapNotNull { t -> t.parentId?.let { pid -> pid to t } }
        .groupBy({ it.first }, { it.second })
    fun recurse(items: List<Task>, depth: Int): List<Pair<Task, Int>> {
        val result = mutableListOf<Pair<Task, Int>>()
        for (task in items) {
            result.add(task to depth)
            if (task.id in expandedIds) {
                result.addAll(recurse(subTaskMap[task.id].orEmpty(), depth + 1))
            }
        }
        return result
    }
    return recurse(tasks.filter { it.parentId == null }, 0)
}
