package com.neomaverick.stride.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromType(t: WorkoutType): String = t.name
    @TypeConverter fun toType(s: String): WorkoutType = WorkoutType.valueOf(s)
}

@Database(entities = [Habit::class, HabitLog::class, Workout::class, StepDay::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class StrideDb : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun logs(): LogDao
    abstract fun workouts(): WorkoutDao
    abstract fun steps(): StepDao

    companion object {
        @Volatile private var instance: StrideDb? = null
        fun get(context: Context): StrideDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StrideDb::class.java, "stride.db")
                .build().also { instance = it }
        }
        /** Tests get a fresh database per test (Robolectric gives each test a new Application). */
        fun resetForTests() { instance?.close(); instance = null }
        fun inMemory(context: Context): StrideDb =
            Room.inMemoryDatabaseBuilder(context, StrideDb::class.java).allowMainThreadQueries().build()
    }
}
