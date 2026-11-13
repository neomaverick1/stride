package com.neomaverick.stride.domain

import com.neomaverick.stride.data.Habit

/** Streak maths, kept free of Android so it is unit-tested on the JVM. */
object Streaks {
    /**
     * Current streak: how many scheduled days in a row the habit was done, counting back from today.
     * Today only breaks the streak once it is over, so an unfinished today never shows 0.
     */
    fun current(habit: Habit, doneDays: Set<Long>, today: Long): Int {
        var day = today
        var streak = 0
        if (habit.isDueOn(today) && today !in doneDays) day--
        while (day >= habit.createdDay) {
            if (habit.isDueOn(day)) {
                if (day in doneDays) streak++ else break
            }
            day--
        }
        return streak
    }

    /** Longest run of consecutive scheduled days that were all done. */
    fun best(habit: Habit, doneDays: Set<Long>, today: Long): Int {
        var best = 0
        var run = 0
        for (day in habit.createdDay..today) {
            if (!habit.isDueOn(day)) continue
            if (day in doneDays) { run++; best = maxOf(best, run) } else if (day != today) run = 0
        }
        return best
    }

    /** Share of scheduled days done in [from, to], 0..1. */
    fun completionRate(habit: Habit, doneDays: Set<Long>, from: Long, to: Long): Double {
        val start = maxOf(from, habit.createdDay)
        val scheduled = (start..to).count { habit.isDueOn(it) }
        if (scheduled == 0) return 0.0
        return (start..to).count { habit.isDueOn(it) && it in doneDays }.toDouble() / scheduled
    }
}
