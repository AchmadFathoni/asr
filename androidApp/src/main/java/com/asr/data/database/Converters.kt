package com.asr.data.database

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object Converters {
    @ColumnTypeConverter
    fun dateTimeFromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    @ColumnTypeConverter
    fun dateTimeToTimestamp(date: LocalDateTime?): Long? {
        return date?.toInstant(TimeZone.currentSystemDefault())?.epochSeconds
    }

    @ColumnTypeConverter
    fun dateFromTimestamp(value: Long): LocalDate {
        return LocalDate.fromEpochDays(value.toInt())
    }

    @ColumnTypeConverter
    fun dateToTimestamp(date: LocalDate): Long {
        return date.toEpochDays().toLong()
    }
}
