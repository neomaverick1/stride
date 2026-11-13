package com.neomaverick.stride.data

import kotlin.random.Random

/** Four weeks of realistic history so every screen has something to show on first launch. */
object DemoData {
    data class Bundle(val habits: List<Habit>, val logs: List<HabitLog>, val workouts: List<Workout>, val steps: List<StepDay>)

    fun build(today: Long, seed: Int = 7): Bundle {
        val rnd = Random(seed)
        val start = today - 27
        val habits = listOf(
            Habit(1, "Drink 2L water", "💧", 0xFF3BA7FF, createdDay = start, reminderMinutes = 9 * 60),
            Habit(2, "Read 20 pages", "📚", 0xFFFFB347, createdDay = start, reminderMinutes = 21 * 60),
            Habit(3, "Meditate 10 min", "🧘", 0xFFB388FF, createdDay = start, days = 0b0011111),
            Habit(4, "No phone in bed", "🌙", 0xFF4DD0A7, createdDay = start),
            Habit(5, "Stretch", "🤸", 0xFFFF6E6E, createdDay = start + 7, days = 0b1010101),
        )
        // How likely each habit is done; Mondays and Fridays are stronger, Sundays weaker.
        val base = mapOf(1L to 0.9, 2L to 0.7, 3L to 0.8, 4L to 0.45, 5L to 0.6)
        val steps = (start..today).map { d ->
            val wd = Days.weekdayIndex(d)
            val active = wd in listOf(0, 2, 4, 5)
            StepDay(d, (if (active) 9000 else 5500) + rnd.nextLong(-1500, 2500) + (d - start) * 40)
        }
        val stepsByDay = steps.associate { it.day to it.steps }
        val logs = buildList {
            for (h in habits) for (d in h.createdDay..today) {
                if (!h.isDueOn(d)) continue
                if (d == today && h.id > 2) continue // leave some of today open
                val wd = Days.weekdayIndex(d)
                var p = base.getValue(h.id) + when (wd) { 0, 4 -> 0.1; 6 -> -0.2; else -> 0.0 }
                if ((stepsByDay[d] ?: 0) >= 8000) p += 0.1
                p += (d - start) * 0.004 // improving over the month
                if (rnd.nextDouble() < p) add(HabitLog(h.id, d))
            }
        }
        val types = WorkoutType.entries
        val workouts = (start..today).filter { Days.weekdayIndex(it) in listOf(0, 2, 4, 5) }.mapIndexed { i, d ->
            val t = types[(i * 5 + rnd.nextInt(2)) % types.size]
            Workout(0, t, d, 20 + rnd.nextInt(5) * 10, 2 + rnd.nextInt(3))
        }
        return Bundle(habits, logs, workouts, steps)
    }
}
