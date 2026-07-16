package com.asr.core.tag

import kotlinx.coroutines.flow.Flow

class SharedTagRepo(private val storage: TagStorage) : TagRepo {
    override fun getTagsFlow(): Flow<List<Tag>> = storage.observeTags()
    override suspend fun getTags(): List<Tag> = storage.getTags()
    override suspend fun getTagById(id: Long): Tag? = storage.getTagById(id)
    override suspend fun upsertTag(tag: Tag): Long = storage.upsertTag(tag)
    override suspend fun deleteTag(tagId: Long) = storage.deleteTag(tagId)
    override suspend fun getTagsForTask(taskId: Long): List<Tag> = storage.getTagsForTask(taskId)
    override suspend fun getTagsForHabit(habitId: Long): List<Tag> = storage.getTagsForHabit(habitId)
    override fun getTaskTagMappingsFlow(): Flow<Map<Long, List<Long>>> = storage.observeTaskTagMappings()
    override fun getHabitTagMappingsFlow(): Flow<Map<Long, List<Long>>> = storage.observeHabitTagMappings()

    override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {
        val validIds = tagIds.filter { it in storage.getTags().map { t -> t.id }.toSet() }
        storage.setTagsForTask(taskId, validIds)
    }

    override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {
        val validIds = tagIds.filter { it in storage.getTags().map { t -> t.id }.toSet() }
        storage.setTagsForHabit(habitId, validIds)
    }

    override suspend fun insertAll(tags: List<Tag>) = storage.replaceAll(tags)
}
