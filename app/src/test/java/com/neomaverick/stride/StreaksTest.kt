package com.neomaverick.stride

import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.domain.Streaks
import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {
    // 2026-11-09 is a Monday.
    private val monday = java.time.LocalDate.of(2026, 11, 9).toEpochDay()
    private fun habit(days: Int = 0b1111111, created: Long = monday - 30) = Habit(1, "Read", "📚", 0, days = days, createdDay = created)

    @Test fun unfinishedTodayDoesNotBreakStreak() {
        val done = setOf(monday - 1, monday - 2, monday - 3)
        assertEquals(3, Streaks.current(habit(), done, monday))
    }

    @Test fun todayCountsOnceDone() {
        val done = setOf(monday, monday - 1)
        assertEquals(2, Streaks.current(habit(), done, monday))
    }

    @Test fun missedDayBreaksStreak() {
        val done = setOf(monday - 1, monday - 3, monday - 4)
        assertEquals(1, Streaks.current(habit(), done, monday))
    }

    @Test fun unscheduledDaysAreSkipped() {
        val weekdays = 0b0011111
        // Friday, Thursday done; the weekend in between is not scheduled.
        val done = setOf(monday - 3, monday - 4)
        assertEquals(2, Streaks.current(habit(weekdays), done, monday))
    }

    @Test fun bestStreakFindsLongestRun() {
        val done = (monday - 10..monday - 6).toSet() + setOf(monday - 2, monday - 1)
        assertEquals(5, Streaks.best(habit(), done, monday))
    }

    @Test fun completionRateUsesOnlyScheduledDays() {
        val done = setOf(monday, monday - 1)
        assertEquals(0.5, Streaks.completionRate(habit(), done, monday - 3, monday), 1e-9)
    }
}
