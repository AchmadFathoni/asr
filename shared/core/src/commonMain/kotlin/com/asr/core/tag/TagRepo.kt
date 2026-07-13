package com.asr.core.tag

import kotlinx.coroutines.flow.Flow

interface TagRepo {
    fun getTagsFlow(): Flow<List<Tag>>

    suspend fun getTags(): List<Tag>

    suspend fun getTagById(id: Long): Tag?

    suspend fun upsertTag(tag: Tag): Long

    suspend fun deleteTag(tagId: Long)

    suspend fun getTagsForTask(taskId: Long): List<Tag>

    suspend fun getTagsForHabit(habitId: Long): List<Tag>

    fun getTaskTagMappingsFlow(): Flow<Map<Long, List<Long>>>

    fun getHabitTagMappingsFlow(): Flow<Map<Long, List<Long>>>

    suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>)

    suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>)

    suspend fun insertAll(tags: List<Tag>)
}
