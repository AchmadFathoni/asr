package com.asr.core.tag

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Long? = null,
)
