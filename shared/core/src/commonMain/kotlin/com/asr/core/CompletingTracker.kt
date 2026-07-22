package com.asr.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun MutableStateFlow<Set<Long>>.hideAfter(delayMs: Long, id: Long) {
    delay(delayMs)
    value = value - id
}
