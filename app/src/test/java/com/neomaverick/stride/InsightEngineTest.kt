package com.neomaverick.stride

import com.neomaverick.stride.data.DemoData
import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.data.HabitLog
import com.neomaverick.stride.domain.CoachInput
import com.neomaverick.stride.domain.Insight
import com.neomaverick.stride.domain.InsightEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEngineTest {
    private val today = java.time.LocalDate.of(2026, 11, 13).toEpochDay()

    @Test fun demoHistoryProducesARankedReport() {
        val d = DemoData.build(today)
        val r = InsightEngine.analyze(CoachInput(d.habits, d.logs, d.workouts, d.steps, today))
        assertTrue(r.score in 0..100)
        assertTrue(r.insights.size >= 4)
        // Risks are shown first so the most useful advice is at the top.
        val firstNonRisk = r.insights.indexOfFirst { it.kind != Insight.Kind.RISK }
        assertTrue(r.insights.drop(firstNonRisk).none { it.kind == Insight.Kind.RISK })
    }

    @Test fun improvementIsReportedAsAWin() {
        val h = Habit(1, "Water", "💧", 0, createdDay = today - 20)
        val logs = (today - 6..today).map { HabitLog(1, it) } + listOf(HabitLog(1, today - 10))
        val r = InsightEngine.analyze(CoachInput(listOf(h), logs, emptyList(), emptyList(), today))
        assertEquals(1.0, r.completionThisWeek, 1e-9)
        assertTrue(r.insights.any { it.kind == Insight.Kind.WIN && it.title.startsWith("Up") })
    }

    @Test fun lowCompletionHabitIsFlagged() {
        val good = Habit(1, "Water", "💧", 0, createdDay = today - 20)
        val bad = Habit(2, "Stretch", "🤸", 0, createdDay = today - 20)
        val logs = (today - 13..today).map { HabitLog(1, it) } + listOf(HabitLog(2, today - 3))
        val r = InsightEngine.analyze(CoachInput(listOf(good, bad), logs, emptyList(), emptyList(), today))
        assertTrue(r.insights.any { it.kind == Insight.Kind.RISK && "Stretch" in it.title })
        assertTrue("Stretch" in r.focus)
    }

    @Test fun pearsonOfPerfectLineIsOne() {
        assertEquals(1.0, InsightEngine.pearson(listOf(1.0, 2.0, 3.0), listOf(2.0, 4.0, 6.0)), 1e-9)
    }

    @Test fun emptyDataGivesNoCrash() {
        val r = InsightEngine.analyze(CoachInput(emptyList(), emptyList(), emptyList(), emptyList(), today))
        assertEquals(0, r.score)
    }
}
