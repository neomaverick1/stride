package com.neomaverick.stride.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A habit the user wants to repeat. [days] is a 7-bit mask, Monday = bit 0. */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,
    val days: Int = 0b1111111,
    val reminderMinutes: Int? = null,
    val createdDay: Long,
    val archived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun isDueOn(day: Long): Boolean = days and (1 shl Days.weekdayIndex(day)) != 0
}

/** One completion of a habit on a given epoch day. */
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "day"],
    indices = [Index("day")],
)
data class HabitLog(
    val habitId: Long,
    val day: Long,
    val doneAt: Long = System.currentTimeMillis(),
)

enum class WorkoutType(val label: String, val emoji: String, val kcalPerMin: Double) {
    RUN("Run", "🏃", 10.0), WALK("Walk", "🚶", 4.5), STRENGTH("Strength", "🏋️", 6.0),
    CYCLE("Cycle", "🚴", 8.0), YOGA("Yoga", "🧘", 3.0), HIIT("HIIT", "⚡", 12.0),
}

@Entity(tableName = "workouts", indices = [Index("day")])
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: WorkoutType,
    val day: Long,
    val minutes: Int,
    val effort: Int,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val calories: Int get() = (minutes * type.kcalPerMin * (0.7 + effort * 0.1)).toInt()
}

/** Daily step totals, cached from Health Connect (or generated in demo mode). */
@Entity(tableName = "steps")
data class StepDay(@PrimaryKey val day: Long, val steps: Long)
