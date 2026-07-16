package com.asr.core.task

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val dueDate: LocalDate? = null,
    val parentId: Long? = null,
    val isPinned: Boolean = false,
    val reminderTime: String? = null,
)
