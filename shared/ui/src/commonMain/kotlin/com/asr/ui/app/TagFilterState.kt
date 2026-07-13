package com.asr.ui.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TagFilterState {
    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    fun toggle(tagId: Long) {
        _selectedTagIds.value = if (tagId in _selectedTagIds.value)
            _selectedTagIds.value - tagId
        else _selectedTagIds.value + tagId
    }

    fun clear() { _selectedTagIds.value = emptySet() }
}
