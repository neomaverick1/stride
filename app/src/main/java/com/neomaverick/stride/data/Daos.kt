package com.neomaverick.stride.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY id")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY id")
    suspend fun active(): List<Habit>

    @Query("SELECT * FROM habits")
    suspend fun all(): List<Habit>

    @Upsert suspend fun upsert(habit: Habit): Long

    @Query("UPDATE habits SET archived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits") suspend fun clear()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM habit_logs WHERE day >= :from")
    fun observeSince(from: Long): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE day >= :from")
    suspend fun since(from: Long): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY day DESC")
    suspend fun forHabit(habitId: Long): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(log: HabitLog)
    @Delete suspend fun delete(log: HabitLog)
    @Query("DELETE FROM habit_logs") suspend fun clear()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY day DESC, id DESC")
    fun observeAll(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE day >= :from")
    suspend fun since(from: Long): List<Workout>

    @Upsert suspend fun upsert(workout: Workout)
    @Delete suspend fun delete(workout: Workout)
    @Query("DELETE FROM workouts") suspend fun clear()
}

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE day >= :from ORDER BY day")
    fun observeSince(from: Long): Flow<List<StepDay>>

    @Query("SELECT * FROM steps WHERE day >= :from ORDER BY day")
    suspend fun since(from: Long): List<StepDay>

    @Upsert suspend fun upsertAll(days: List<StepDay>)
    @Query("DELETE FROM steps") suspend fun clear()
}
