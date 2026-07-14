package com.asr.ui.viewmodel

import com.asr.core.backup.ExportRepo
import com.asr.core.backup.RestoreRepo
import com.asr.core.backup.RestoreResult
import com.asr.core.settings.SettingsRepo
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private class FakeSettingsRepo(initial: Boolean?) {
        private var dark: Boolean? = initial
        fun isDarkMode(): Boolean? = dark
        fun setDarkMode(isDark: Boolean?) { dark = isDark }
    }

    private class FakeTagRepo : TagRepo {
        override fun getTagsFlow(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun getTags(): List<Tag> = emptyList()
        override suspend fun getTagById(id: Long): Tag? = null
        override suspend fun upsertTag(tag: Tag): Long = 1
        override suspend fun deleteTag(tagId: Long) {}
        override suspend fun getTagsForTask(taskId: Long): List<Tag> = emptyList()
        override suspend fun getTagsForHabit(habitId: Long): List<Tag> = emptyList()
        override fun getTaskTagMappingsFlow(): Flow<Map<Long, List<Long>>> = flowOf(emptyMap())
        override fun getHabitTagMappingsFlow(): Flow<Map<Long, List<Long>>> = flowOf(emptyMap())
        override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {}
        override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {}
        override suspend fun insertAll(tags: List<Tag>) {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(initialDark: Boolean? = true): SettingsViewModel {
        val repo = FakeSettingsRepo(initial = initialDark)
        return SettingsViewModel(
            exportRepo = object : ExportRepo {
                override suspend fun exportToJson() {}
            },
            restoreRepo = object : RestoreRepo {
                override suspend fun restoreData() = RestoreResult.Success
            },
            tagRepo = FakeTagRepo(),
            settingsRepo = object : SettingsRepo {
                override fun isDarkMode(): Boolean? = repo.isDarkMode()
                override fun setDarkMode(isDark: Boolean?) = repo.setDarkMode(isDark)
            },
        )
    }

    @Test
    fun `setDarkMode false switches to explicit light`() = runBlocking {
        val vm = createViewModel(initialDark = true)
        vm.state.first()
        vm.onAction(SettingsViewModel.Action.SetDarkMode(false))
        assertEquals(false, vm.state.value.isDarkMode)
    }

    @Test
    fun `setDarkMode true switches to dark`() = runBlocking {
        val vm = createViewModel(initialDark = false)
        vm.state.first()
        vm.onAction(SettingsViewModel.Action.SetDarkMode(true))
        assertEquals(true, vm.state.value.isDarkMode)
    }

    @Test
    fun `setDarkMode null falls back to system theme`() = runBlocking {
        val vm = createViewModel(initialDark = true)
        vm.state.first()
        vm.onAction(SettingsViewModel.Action.SetDarkMode(null))
        assertEquals(null, vm.state.value.isDarkMode)
    }

    @Test
    fun `initial dark mode from repo`() = runBlocking {
        val vm = createViewModel(initialDark = true)
        assertEquals(true, vm.state.first().isDarkMode)
    }
}
