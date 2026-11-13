package com.neomaverick.stride.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.ui.HabitRow
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.components.Card
import com.neomaverick.stride.ui.components.Pill
import com.neomaverick.stride.ui.components.ProgressRing
import com.neomaverick.stride.ui.components.SectionLabel
import com.neomaverick.stride.ui.theme.StrideColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(vm: StrideViewModel) {
    val s by vm.state.collectAsState()
    val report by vm.report.collectAsState()
    val date = Days.date(Days.today()).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column {
                Text(date.uppercase(), style = MaterialTheme.typography.labelMedium, color = StrideColors.Muted)
                Text("Keep moving.", style = MaterialTheme.typography.displaySmall)
            }
        }
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(s.progress, 118.dp, 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall)
                            Text("today", color = StrideColors.Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${s.due.count { it.doneToday }} of ${s.due.size} habits done", style = MaterialTheme.typography.titleMedium)
                        Pill("🔥 ${s.topStreak}-day best streak", StrideColors.Amber)
                        Pill("👟 ${"%,d".format(Locale.US, s.stepsToday)} steps", StrideColors.Teal)
                        report?.let { Pill("✦ Weekly score ${it.score}", StrideColors.Lime) }
                    }
                }
            }
        }
        item { SectionLabel("Today's habits", Modifier.padding(top = 4.dp)) }
        items(s.due, key = { it.habit.id }) { row -> HabitCheckRow(row) { vm.toggle(row.habit.id, !row.doneToday) } }
        if (s.loaded && s.due.isEmpty()) item {
            Card { Text("Nothing scheduled today. Add a habit in the Habits tab.", color = StrideColors.Muted) }
        }
    }
}

@Composable
fun HabitCheckRow(row: HabitRow, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val color = Color(row.habit.color)
    val fill by animateColorAsState(if (row.doneToday) color else Color.Transparent, label = "check")
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(role = Role.Checkbox) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress); onToggle()
        }.semantics { contentDescription = "${row.habit.name}, ${if (row.doneToday) "done" else "not done"}" }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Text(row.habit.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(row.habit.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                    row.last7.forEach { d ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(when (d) {
                            true -> color; false -> Color(0x33FFFFFF); null -> Color.Transparent
                        }))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("🔥 ${row.streak}", color = StrideColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.size(34.dp).clip(CircleShape).background(fill).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) {
                if (row.doneToday) Icon(Icons.Rounded.Check, null, tint = StrideColors.Background, modifier = Modifier.size(20.dp))
            }
        }
    }
}
