package com.neomaverick.stride.domain

import com.neomaverick.stride.data.Days
import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.data.HabitLog
import com.neomaverick.stride.data.StepDay
import com.neomaverick.stride.data.Workout
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Insight(val kind: Kind, val title: String, val body: String) {
    enum class Kind { WIN, PATTERN, RISK, TIP }
}

data class WeeklyReport(
    val score: Int,
    val headline: String,
    val completionThisWeek: Double,
    val completionLastWeek: Double,
    val insights: List<Insight>,
    val focus: String,
)

/** Everything the coach looks at. Plain data, so any [InsightProvider] can use it. */
data class CoachInput(
    val habits: List<Habit>,
    val logs: List<HabitLog>,
    val workouts: List<Workout>,
    val steps: List<StepDay>,
    val today: Long,
)

/** Where weekly reports come from. The default runs fully on the phone; an LLM-backed provider can slot in here. */
fun interface InsightProvider {
    suspend fun weekly(input: CoachInput): WeeklyReport
}

/**
 * The on-device coach: turns 4 weeks of history into a ranked, plain-language weekly report.
 * No network, no account, no cost, and it is deterministic, so it is fully unit-tested.
 */
object InsightEngine : InsightProvider {

    override suspend fun weekly(input: CoachInput): WeeklyReport = analyze(input)

    fun analyze(input: CoachInput): WeeklyReport {
        val today = input.today
        val thisWeek = (today - 6)..today
        val lastWeek = (today - 13)..(today - 7)
        val done = input.logs.groupBy { it.habitId }.mapValues { e -> e.value.map { it.day }.toSet() }

        val rateThis = rate(input.habits, done, thisWeek)
        val rateLast = rate(input.habits, done, lastWeek)
        val insights = mutableListOf<Insight>()

        // 1. Week over week.
        val delta = if (rateThis.isNaN() || rateLast.isNaN()) 0 else ((rateThis - rateLast) * 100).roundToInt()
        if (input.habits.isNotEmpty()) insights += when {
            delta >= 5 -> Insight(Insight.Kind.WIN, "Up $delta points on last week",
                "You finished ${pct(rateThis)} of your scheduled habits, up from ${pct(rateLast)}. Keep the same routine.")
            delta <= -5 -> Insight(Insight.Kind.RISK, "Down ${-delta} points on last week",
                "Completion slipped from ${pct(rateLast)} to ${pct(rateThis)}. One small win today stops the slide.")
            else -> Insight(Insight.Kind.PATTERN, "Steady week",
                "You held at ${pct(rateThis)}, within a few points of last week.")
        }

        // 2. Best and weakest weekday over the last 4 weeks.
        val window = (today - 27)..today
        val byWeekday = (0..6).associateWith { wd -> rate(input.habits, done, window.filter { Days.weekdayIndex(it) == wd }) }
            .filterValues { !it.isNaN() }
        if (byWeekday.size >= 3) {
            val best = byWeekday.maxBy { it.value }
            val worst = byWeekday.minBy { it.value }
            if (best.value - worst.value >= 0.2) insights += Insight(Insight.Kind.PATTERN,
                "${dayName(best.key)}s are your strongest day",
                "You complete ${pct(best.value)} of habits on ${dayName(best.key)}s but ${pct(worst.value)} on ${dayName(worst.key)}s.")
        }

        // 3. Star habit and the one at risk.
        val perHabit = input.habits.map { h -> h to Streaks.completionRate(h, done[h.id].orEmpty(), today - 13, today) }
        perHabit.maxByOrNull { it.second }?.takeIf { it.second >= 0.7 }?.let { (h, r) ->
            val streak = Streaks.current(h, done[h.id].orEmpty(), today)
            insights += Insight(Insight.Kind.WIN, "${h.emoji} ${h.name} is locked in",
                "${pct(r)} over two weeks" + if (streak >= 3) ", on a $streak-day streak." else ".")
        }
        val atRisk = perHabit.filter { it.second < 0.5 }.minByOrNull { it.second }
        atRisk?.let { (h, r) ->
            insights += Insight(Insight.Kind.RISK, "${h.emoji} ${h.name} needs attention",
                "Only ${pct(r)} over two weeks." + if (h.reminderMinutes == null) " A reminder at a set time usually helps." else " Try moving it to an easier time.")
        }

        // 4. Movement: steps trend and its link to habits.
        val steps = input.steps.associate { it.day to it.steps }
        val stepsThis = thisWeek.mapNotNull { steps[it] }
        val stepsLast = lastWeek.mapNotNull { steps[it] }
        if (stepsThis.size >= 3) {
            val avgThis = stepsThis.average()
            val avgLast = stepsLast.takeIf { it.isNotEmpty() }?.average()
            val trend = avgLast?.let { ((avgThis - it) / it * 100).roundToInt() }
            insights += Insight(if ((trend ?: 0) >= 0) Insight.Kind.WIN else Insight.Kind.PATTERN,
                "${fmt(avgThis.toLong())} steps a day",
                if (trend == null) "Your daily average this week." else "That is ${if (trend >= 0) "up" else "down"} ${abs(trend)}% on last week.")
        }
        correlation(input, done, window, steps)?.let { insights += it }

        // 5. Training load.
        val wThis = input.workouts.filter { it.day in thisWeek }
        if (wThis.isNotEmpty()) {
            val minutes = wThis.sumOf { it.minutes }
            val top = wThis.groupBy { it.type }.maxBy { it.value.size }.key
            insights += Insight(Insight.Kind.WIN, "$minutes active minutes",
                "${wThis.size} workouts, ${wThis.sumOf { it.calories }} kcal. Most often: ${top.emoji} ${top.label}.")
        }

        val score = score(rateThis, stepsThis.average().takeIf { !it.isNaN() }, wThis.sumOf { it.minutes })
        val focus = focusFor(atRisk?.first, byWeekday)
        val ranked = insights.sortedBy { listOf(Insight.Kind.RISK, Insight.Kind.WIN, Insight.Kind.PATTERN, Insight.Kind.TIP).indexOf(it.kind) }
        return WeeklyReport(score, headline(score, delta), rateThis, rateLast, ranked, focus)
    }

    /** Share of scheduled habit-days done on [days] (NaN when nothing was scheduled). */
    fun rate(habits: List<Habit>, done: Map<Long, Set<Long>>, days: Iterable<Long>): Double {
        var scheduled = 0
        var completed = 0
        for (h in habits) for (d in days) {
            if (d < h.createdDay || !h.isDueOn(d)) continue
            scheduled++
            if (d in done[h.id].orEmpty()) completed++
        }
        return if (scheduled == 0) Double.NaN else completed.toDouble() / scheduled
    }

    private fun correlation(input: CoachInput, done: Map<Long, Set<Long>>, window: LongRange, steps: Map<Long, Long>): Insight? {
        val pairs = window.filter { it < input.today && it in steps }.map { d ->
            steps.getValue(d).toDouble() to rate(input.habits, done, listOf(d))
        }.filter { !it.second.isNaN() }
        if (pairs.size < 10) return null
        val r = pearson(pairs.map { it.first }, pairs.map { it.second })
        if (abs(r) < 0.3) return null
        val active = pairs.filter { it.first >= 8000 }.map { it.second }
        val quiet = pairs.filter { it.first < 8000 }.map { it.second }
        if (active.size < 3 || quiet.size < 3) return null
        return Insight(Insight.Kind.PATTERN, "Moving more goes with doing more",
            "On days with 8,000+ steps you finish ${pct(active.average())} of habits, against ${pct(quiet.average())} on quieter days.")
    }

    fun pearson(x: List<Double>, y: List<Double>): Double {
        val mx = x.average(); val my = y.average()
        var num = 0.0; var dx = 0.0; var dy = 0.0
        for (i in x.indices) { val a = x[i] - mx; val b = y[i] - my; num += a * b; dx += a * a; dy += b * b }
        return if (dx == 0.0 || dy == 0.0) 0.0 else num / sqrt(dx * dy)
    }

    /** 0-100: habits weigh most, then movement, then training. */
    fun score(rate: Double, avgSteps: Double?, activeMinutes: Int): Int {
        val h = if (rate.isNaN()) 0.0 else rate
        val s = ((avgSteps ?: 0.0) / 10000.0).coerceAtMost(1.0)
        val w = (activeMinutes / 150.0).coerceAtMost(1.0)
        return ((h * 0.6 + s * 0.25 + w * 0.15) * 100).roundToInt()
    }

    private fun headline(score: Int, delta: Int) = when {
        score >= 85 -> "Outstanding week"
        score >= 70 -> if (delta > 0) "Strong and climbing" else "Strong week"
        score >= 50 -> "Solid base to build on"
        else -> "A reset week. Start small"
    }

    private fun focusFor(atRisk: Habit?, byWeekday: Map<Int, Double>): String {
        if (atRisk == null) return "Keep every streak alive this week and add one new habit if it feels easy."
        val best = byWeekday.maxByOrNull { it.value }?.key
        return "Focus on ${atRisk.emoji} ${atRisk.name}: aim for 3 check-ins" +
            (best?.let { ", starting on ${dayName(it)}, your strongest day." } ?: ".")
    }

    private fun dayName(index: Int) = java.time.DayOfWeek.of(index + 1).getDisplayName(TextStyle.FULL, Locale.US)
    private fun pct(v: Double) = if (v.isNaN()) "0%" else "${(v * 100).roundToInt()}%"
    private fun fmt(n: Long) = "%,d".format(Locale.US, n)
}
