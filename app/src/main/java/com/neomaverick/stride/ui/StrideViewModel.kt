package com.neomaverick.stride.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neomaverick.stride.StrideApp
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.data.HabitLog
import com.neomaverick.stride.data.StepDay
import com.neomaverick.stride.data.Workout
import com.neomaverick.stride.domain.InsightEngine
import com.neomaverick.stride.domain.InsightProvider
import com.neomaverick.stride.domain.Streaks
import com.neomaverick.stride.domain.WeeklyReport
import com.neomaverick.stride.reminders.Reminders
import com.neomaverick.stride.widget.TodayWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitRow(val habit: Habit, val doneToday: Boolean, val streak: Int, val best: Int, val last7: List<Boolean?>)

data class TodayState(
    val rows: List<HabitRow> = emptyList(),
    val stepsToday: Long = 0,
    val loaded: Boolean = false,
) {
    val due get() = rows.filter { it.habit.isDueOn(Days.today()) }
    val progress get() = if (due.isEmpty()) 0f else due.count { it.doneToday } / due.size.toFloat()
    val topStreak get() = rows.maxOfOrNull { it.streak } ?: 0
}

class StrideViewModel(app: Application, private val coach: InsightProvider = InsightEngine) : AndroidViewModel(app) {
    private val strideApp = app as StrideApp
    private val repo = strideApp.repo
    val premium = strideApp.premium
    val health = strideApp.health
    private val today = Days.today()

    val workouts: StateFlow<List<Workout>> = repo.workouts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val steps: StateFlow<List<StepDay>> = repo.stepsSince(today - 27).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<TodayState> = combine(repo.habits, repo.logsSince(today - 400), steps) { habits, logs, st ->
        build(habits, logs, st)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TodayState())

    private val _report = MutableStateFlow<WeeklyReport?>(null)
    val report: StateFlow<WeeklyReport?> = _report
    val logs7: StateFlow<List<HabitLog>> = repo.logsSince(today - 6).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            if (repo.isEmpty()) repo.loadDemo()
            refreshReport()
        }
    }

    private fun build(habits: List<Habit>, logs: List<HabitLog>, st: List<StepDay>): TodayState {
        val byHabit = logs.groupBy { it.habitId }.mapValues { e -> e.value.map { it.day }.toSet() }
        val rows = habits.map { h ->
            val done = byHabit[h.id].orEmpty()
            HabitRow(h, today in done, Streaks.current(h, done, today), Streaks.best(h, done, today),
                (today - 6..today).map { d -> if (!h.isDueOn(d) || d < h.createdDay) null else d in done })
        }
        return TodayState(rows, st.firstOrNull { it.day == today }?.steps ?: 0, loaded = true)
    }

    fun toggle(habitId: Long, done: Boolean) = viewModelScope.launch {
        repo.toggle(habitId, today, done)
        TodayWidget.refresh(getApplication())
        refreshReport()
    }

    fun saveHabit(h: Habit) = viewModelScope.launch {
        val id = repo.saveHabit(h)
        Reminders.schedule(getApplication(), h.copy(id = id))
        TodayWidget.refresh(getApplication())
    }

    fun archive(h: Habit) = viewModelScope.launch {
        repo.archiveHabit(h.id); Reminders.cancel(getApplication(), h.id); TodayWidget.refresh(getApplication())
    }

    fun logWorkout(w: Workout) = viewModelScope.launch { repo.saveWorkout(w); refreshReport() }
    fun deleteWorkout(w: Workout) = viewModelScope.launch { repo.deleteWorkout(w); refreshReport() }

    fun syncSteps() = viewModelScope.launch {
        runCatching { if (health.granted()) repo.saveSteps(health.read()) }
        refreshReport()
    }

    fun resetDemo() = viewModelScope.launch { repo.loadDemo(); refreshReport(); TodayWidget.refresh(getApplication()) }

    fun refreshReport() = viewModelScope.launch { _report.value = coach.weekly(repo.coachInput(today)) }
}
