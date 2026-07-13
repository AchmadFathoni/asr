package com.asr.data.repository

import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.data.database.HabitTagEntity
import com.asr.data.database.TagDao
import com.asr.data.database.TagEntity
import com.asr.data.database.TaskTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [TagRepo::class])
class TagRepository(private val tagDao: TagDao) : TagRepo {

    override fun getTagsFlow(): Flow<List<Tag>> =
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

    override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {
        tagDao.clearTaskTags(taskId)
        tagIds.forEach { tagDao.addTaskTag(TaskTagEntity(taskId, it)) }
    }

    override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {
        tagDao.clearHabitTags(habitId)
        tagIds.forEach { tagDao.addHabitTag(HabitTagEntity(habitId, it)) }
    }

    override suspend fun insertAll(tags: List<Tag>) {
        tagDao.insertAllTags(tags.map { it.toEntity() })
    }

    private fun TagEntity.toDomain() = Tag(id = id, name = name, color = color)
    private fun Tag.toEntity() = TagEntity(id = id, name = name, color = color)
}
