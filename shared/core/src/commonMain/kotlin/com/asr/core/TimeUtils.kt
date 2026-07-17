package com.asr.core

import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun LocalDateTime.Companion.now(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDate.Companion.now(): LocalDate =
    Clock.System.todayIn(TimeZone.currentSystemDefault())

fun LocalTime.Companion.now(): LocalTime =
    LocalDateTime.now().time

fun currentDateFlow(): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now())
        delay(60_000)
    }
}
