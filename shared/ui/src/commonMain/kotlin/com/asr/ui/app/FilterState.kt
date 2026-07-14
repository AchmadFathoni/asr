package com.asr.ui.app

import kotlinx.datetime.LocalDate

data class FilterState(
    val searchQuery: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val filterDate: LocalDate? = null,
    val showFilterSheet: Boolean = false,
)
