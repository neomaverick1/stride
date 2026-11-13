package com.neomaverick.stride.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.neomaverick.stride.MainActivity
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.data.StrideDb
import com.neomaverick.stride.domain.Streaks

/** Home-screen widget: today's progress and your longest live streak. Tapping opens the app. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = StrideDb.get(context)
        val today = Days.today()
        val habits = db.habits().active().filter { it.isDueOn(today) }
        val logs = db.logs().since(today - 400)
        val doneToday = logs.filter { it.day == today }.map { it.habitId }.toSet()
        val done = habits.count { it.id in doneToday }
        val byHabit = logs.groupBy { it.habitId }.mapValues { e -> e.value.map { it.day }.toSet() }
        val best = habits.maxOfOrNull { Streaks.current(it, byHabit[it.id].orEmpty(), today) } ?: 0
        provideContent {
            Column(
                GlanceModifier.fillMaxSize().background(Color(0xFF0E1525)).cornerRadius(20.dp).padding(14.dp)
                    .clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("STRIDE", style = TextStyle(color = ColorProvider(Color(0xFFC6F432)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.defaultWeight())
                    Text("🔥 $best", style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp))
                }
                Spacer(GlanceModifier.height(6.dp))
                Text("$done of ${habits.size} today", style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (habits.isEmpty()) 0f else done / habits.size.toFloat(),
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                    color = ColorProvider(Color(0xFFC6F432)), backgroundColor = ColorProvider(Color(0x33FFFFFF)),
                )
            }
        }
    }

    companion object { suspend fun refresh(context: Context) = TodayWidget().updateAll(context) }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = TodayWidget() }
