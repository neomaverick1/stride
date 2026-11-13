package com.neomaverick.stride.data

import com.neomaverick.stride.domain.CoachInput
import kotlinx.coroutines.flow.Flow

/** Single source of truth for the UI. Room is the local store; [CloudSync] (optional) backs it up. */
class Repository(private val db: StrideDb, private val sync: com.neomaverick.stride.sync.CloudSync) {
    val habits: Flow<List<Habit>> = db.habits().observeActive()
    val workouts: Flow<List<Workout>> = db.workouts().observeAll()
    fun logsSince(from: Long): Flow<List<HabitLog>> = db.logs().observeSince(from)
    fun stepsSince(from: Long): Flow<List<StepDay>> = db.steps().observeSince(from)

    suspend fun saveHabit(h: Habit): Long {
        val id = db.habits().upsert(h.copy(updatedAt = System.currentTimeMillis()))
        val saved = if (h.id == 0L) h.copy(id = id) else h
        sync.pushHabit(saved)
        return saved.id
    }

    suspend fun archiveHabit(id: Long) { db.habits().archive(id); sync.deleteHabit(id) }

    suspend fun toggle(habitId: Long, day: Long, done: Boolean) {
        val log = HabitLog(habitId, day)
        if (done) db.logs().insert(log) else db.logs().delete(log)
        sync.pushLog(log, done)
    }

    suspend fun saveWorkout(w: Workout) { db.workouts().upsert(w); sync.pushWorkout(w) }
    suspend fun deleteWorkout(w: Workout) = db.workouts().delete(w)
    suspend fun saveSteps(days: List<StepDay>) = db.steps().upsertAll(days)

    suspend fun coachInput(today: Long = Days.today()) = CoachInput(
        habits = db.habits().active(),
        logs = db.logs().since(today - 27),
        workouts = db.workouts().since(today - 27),
        steps = db.steps().since(today - 27),
        today = today,
    )

    suspend fun isEmpty() = db.habits().all().isEmpty()

    suspend fun loadDemo(today: Long = Days.today()) {
        db.logs().clear(); db.workouts().clear(); db.steps().clear(); db.habits().clear()
        val demo = DemoData.build(today)
        demo.habits.forEach { db.habits().upsert(it) }
        demo.logs.forEach { db.logs().insert(it) }
        demo.workouts.forEach { db.workouts().upsert(it) }
        db.steps().upsertAll(demo.steps)
    }
}
