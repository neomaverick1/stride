package com.neomaverick.stride.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Days are stored as epoch days (LocalDate.toEpochDay) so they are time-zone safe. */
object Days {
    fun today(zone: ZoneId = ZoneId.systemDefault()): Long = LocalDate.now(zone).toEpochDay()
    fun date(day: Long): LocalDate = LocalDate.ofEpochDay(day)
    fun weekdayIndex(day: Long): Int = date(day).dayOfWeek.value - 1 // Monday = 0
    fun weekStart(day: Long): Long = date(day).with(DayOfWeek.MONDAY).toEpochDay()
    val shortNames = listOf("M", "T", "W", "T", "F", "S", "S")
}
