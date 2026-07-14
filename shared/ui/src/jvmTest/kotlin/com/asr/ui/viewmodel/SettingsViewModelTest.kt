package com.asr.ui.viewmodel

import com.asr.core.backup.ExportRepo
import com.asr.core.backup.RestoreRepo
import com.asr.core.backup.RestoreResult
import com.asr.core.settings.SettingsRepo
import com.asr.core.settings.ThemeOption
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
    private class FakeSettingsRepo(initial: ThemeOption) {
        private var theme: ThemeOption = initial
        fun getTheme(): ThemeOption = theme
        fun setTheme(t: ThemeOption) { theme = t }
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

    private fun createViewModel(initialTheme: ThemeOption = ThemeOption.SYSTEM): SettingsViewModel {
        val repo = FakeSettingsRepo(initial = initialTheme)
        return SettingsViewModel(
            exportRepo = object : ExportRepo {
                override suspend fun exportToJson() {}
            },
            restoreRepo = object : RestoreRepo {
                override suspend fun restoreData() = RestoreResult.Success
            },
            tagRepo = FakeTagRepo(),
            settingsRepo = object : SettingsRepo {
                override fun getTheme(): ThemeOption = repo.getTheme()
                override fun setTheme(t: ThemeOption) = repo.setTheme(t)
            },
        )
    }

    @Test
    fun `setTheme light switches to explicit light`() = runBlocking {
        val vm = createViewModel(initialTheme = ThemeOption.DARK)
        vm.state.first()
        vm.onAction(SettingsViewModel.Action.SetTheme(ThemeOption.LIGHT))
        assertEquals(ThemeOption.LIGHT, vm.state.value.theme)
    }

    @Test
    fun `setTheme dark switches to dark`() = runBlocking {
        val vm = createViewModel(initialTheme = ThemeOption.LIGHT)
        vm.state.first()
        vm.onAction(SettingsViewModel.Action.SetTheme(ThemeOption.DARK))
        assertEquals(ThemeOption.DARK, vm.state.value.theme)
    }

    @Test
    fun `setTheme system falls back to system theme`() = runBlocking {
        val vm = createViewModel(initialTheme = ThemeOption.DARK)
        vm.state.first()
        vm.onAction(SettingsViewModel.Action.SetTheme(ThemeOption.SYSTEM))
        assertEquals(ThemeOption.SYSTEM, vm.state.value.theme)
    }

    @Test
    fun `initial theme from repo`() = runBlocking {
        val vm = createViewModel(initialTheme = ThemeOption.DARK)
        assertEquals(ThemeOption.DARK, vm.state.first().theme)
    }
}
