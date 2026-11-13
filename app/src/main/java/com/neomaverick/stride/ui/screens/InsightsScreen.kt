package com.neomaverick.stride.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.domain.Insight
import com.neomaverick.stride.domain.InsightEngine
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.components.BarChart
import com.neomaverick.stride.ui.components.Card
import com.neomaverick.stride.ui.components.LineChart
import com.neomaverick.stride.ui.components.ProgressRing
import com.neomaverick.stride.ui.components.SectionLabel
import com.neomaverick.stride.ui.theme.StrideColors

@Composable
fun InsightsScreen(vm: StrideViewModel) {
    val report by vm.report.collectAsState()
    val pro by vm.premium.isPro.collectAsState()
    val state by vm.state.collectAsState()
    val steps by vm.steps.collectAsState()
    val today = Days.today()
    val r = report ?: return Box(Modifier.fillMaxSize())
    val shown = if (pro) r.insights else r.insights.take(2)
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text("YOUR WEEKLY COACH", style = MaterialTheme.typography.labelMedium, color = StrideColors.Lime)
                Text(r.headline, style = MaterialTheme.typography.displaySmall)
            }
        }
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(r.score / 100f, 96.dp, 10.dp) { Text("${r.score}", style = MaterialTheme.typography.headlineSmall) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Weekly score", style = MaterialTheme.typography.titleMedium)
                        Text("Habits ${pct(r.completionThisWeek)} · last week ${pct(r.completionLastWeek)}", color = StrideColors.Muted, fontSize = 13.sp)
                        Text("Built on your phone from 4 weeks of history.", color = StrideColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        item {
            Card {
                Column {
                    SectionLabel("Habits done, last 7 days")
                    Spacer(Modifier.size(10.dp))
                    val days = (today - 6..today).toList()
                    val logs by vm.logs7.collectAsState()
                    val habits = state.rows.map { it.habit }
                    val done = logs.groupBy { it.habitId }.mapValues { e -> e.value.map { it.day }.toSet() }
                    BarChart(days.map { d -> InsightEngine.rate(habits, done, listOf(d)).let { if (it.isNaN()) 0f else it.toFloat() } },
                        days.map { Days.shortNames[Days.weekdayIndex(it)] })
                }
            }
        }
        if (steps.size >= 2) item {
            Card {
                Column {
                    SectionLabel("Steps, last 4 weeks")
                    Spacer(Modifier.size(10.dp))
                    LineChart(steps.map { it.steps.toFloat() })
                }
            }
        }
        item { SectionLabel("What the coach noticed", Modifier.padding(top = 4.dp)) }
        items(shown) { InsightCard(it) }
        if (!pro && r.insights.size > shown.size) item { ProTeaser(r.insights.size - shown.size) { vm.premium.demoUnlock(true) } }
        if (pro) item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(StrideColors.Lime.copy(alpha = 0.22f), StrideColors.Teal.copy(alpha = 0.14f)))).padding(18.dp)) {
                Column {
                    Text("THIS WEEK'S FOCUS", style = MaterialTheme.typography.labelMedium, color = StrideColors.Lime)
                    Text(r.focus, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightCard(i: Insight) {
    val (color, mark) = when (i.kind) {
        Insight.Kind.WIN -> StrideColors.Lime to "▲"
        Insight.Kind.RISK -> StrideColors.Coral to "!"
        Insight.Kind.PATTERN -> StrideColors.Teal to "◆"
        Insight.Kind.TIP -> StrideColors.Amber to "✦"
    }
    Card {
        Row {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(mark, color = color)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(i.title, style = MaterialTheme.typography.titleMedium)
                Text(i.body, color = StrideColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ProTeaser(hidden: Int, onUnlock: () -> Unit) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = StrideColors.Lime)
                Spacer(Modifier.width(8.dp))
                Text("$hidden more insights + your focus plan", style = MaterialTheme.typography.titleMedium)
            }
            Text("Stride Pro unlocks the full weekly report. In this demo it's free.", color = StrideColors.Muted)
            Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Lime, contentColor = Color(0xFF0A1020))) {
                Text("Unlock Pro (demo)")
            }
        }
    }
}

private fun pct(v: Double) = if (v.isNaN()) "–" else "${(v * 100).toInt()}%"
