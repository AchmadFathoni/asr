package com.asr.data.storage

import com.asr.core.tag.Tag
import com.asr.core.tag.TagStorage
import com.asr.data.database.HabitTagEntity
import com.asr.data.database.TagDao
import com.asr.data.database.TagEntity
import com.asr.data.database.TaskTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTagStorage(private val tagDao: TagDao) : TagStorage {
    override fun observeTags(): Flow<List<Tag>> =
        tagDao.getAllTagsFlow().map { it.map { e -> e.toDomain() } }

    override suspend fun getTags(): List<Tag> =
        tagDao.getAllTags().map { it.toDomain() }

    override suspend fun getTagById(id: Long): Tag? =
        tagDao.getTagById(id)?.toDomain()

    override suspend fun upsertTag(tag: Tag): Long =
        tagDao.upsertTag(tag.toEntity())

    override suspend fun deleteTag(tagId: Long) =
        tagDao.deleteTag(tagId)

    override suspend fun getTagsForTask(taskId: Long): List<Tag> =
        tagDao.getTagsForTask(taskId).map { it.toDomain() }

    override suspend fun getTagsForHabit(habitId: Long): List<Tag> =
        tagDao.getTagsForHabit(habitId).map { it.toDomain() }

    override fun observeTaskTagMappings(): Flow<Map<Long, List<Long>>> =
        tagDao.getAllTaskTagsFlow().map { list -> list.groupBy({ it.taskId }, { it.tagId }) }

    override fun observeHabitTagMappings(): Flow<Map<Long, List<Long>>> =
        tagDao.getAllHabitTagsFlow().map { list -> list.groupBy({ it.habitId }, { it.tagId }) }

    override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {
        tagDao.clearTaskTags(taskId)
        tagIds.forEach { tagId -> tagDao.addTaskTag(TaskTagEntity(taskId, tagId)) }
    }

    override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {
        tagDao.clearHabitTags(habitId)
        tagIds.forEach { tagId -> tagDao.addHabitTag(HabitTagEntity(habitId, tagId)) }
    }

    override suspend fun replaceAll(tags: List<Tag>) =
        tagDao.insertAllTags(tags.map { it.toEntity() })

    private fun TagEntity.toDomain() = Tag(id = id, name = name)
    private fun Tag.toEntity() = TagEntity(id = id, name = name)
}
