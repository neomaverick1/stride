package com.neomaverick.stride.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.ui.HabitRow
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.components.Card
import com.neomaverick.stride.ui.components.SectionLabel
import com.neomaverick.stride.ui.theme.StrideColors

private val emojis = listOf("💧", "📚", "🧘", "🌙", "🤸", "🥗", "✍️", "🏃", "🎸", "💊", "🧹", "☀️")
private val palette = listOf(0xFF3BA7FF, 0xFFFFB347, 0xFFB388FF, 0xFF4DD0A7, 0xFFFF6E6E, 0xFFC6F432)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(vm: StrideViewModel) {
    val s by vm.state.collectAsState()
    var editing by remember { mutableStateOf<Habit?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Habits", style = MaterialTheme.typography.displaySmall) }
            item { SectionLabel("${s.rows.size} active · tap to edit") }
            items(s.rows, key = { it.habit.id }) { HabitStatsCard(it) { editing = it.habit } }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = Habit(name = "", emoji = emojis[0], color = palette[0], createdDay = Days.today()) },
            icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("New habit") },
            containerColor = StrideColors.Lime, contentColor = StrideColors.Background,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
        )
    }
    editing?.let { h ->
        ModalBottomSheet(onDismissRequest = { editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = StrideColors.Surface) {
            HabitEditor(h, onSave = { vm.saveHabit(it); editing = null }, onArchive = if (h.id != 0L) ({ vm.archive(h); editing = null }) else null)
        }
    }
}

@Composable
private fun HabitStatsCard(row: HabitRow, onClick: () -> Unit) {
    val color = Color(row.habit.color)
    Card(Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.habit.emoji, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(schedule(row.habit.days) + (row.habit.reminderMinutes?.let { " · ${time(it)}" } ?: ""), color = StrideColors.Muted, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${row.streak}", style = MaterialTheme.typography.headlineSmall, color = color)
                    Text("best ${row.best}", color = StrideColors.Muted, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                row.last7.forEachIndexed { i, d ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(when (d) {
                            true -> color; false -> Color(0x22FFFFFF); null -> Color(0x0AFFFFFF)
                        }))
                        Text(Days.shortNames[Days.weekdayIndex(Days.today() - 6 + i)], color = StrideColors.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitEditor(start: Habit, onSave: (Habit) -> Unit, onArchive: (() -> Unit)?) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(start.name) }
    var emoji by remember { mutableStateOf(start.emoji) }
    var color by remember { mutableStateOf(start.color) }
    var days by remember { mutableIntStateOf(start.days) }
    var reminder by remember { mutableStateOf(start.reminderMinutes) }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (start.id == 0L) "New habit" else "Edit habit", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SectionLabel("Icon")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(emojis) { e ->
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (e == emoji) Color(color).copy(alpha = 0.3f) else Color(0x14FFFFFF))
                    .clickable { emoji = e }, contentAlignment = Alignment.Center) { Text(e, fontSize = 22.sp) }
            }
        }
        SectionLabel("Color")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            palette.forEach { c ->
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                    .border(3.dp, if (c == color) Color.White else Color.Transparent, CircleShape).clickable { color = c })
            }
        }
        SectionLabel("Repeat on")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Days.shortNames.forEachIndexed { i, d ->
                val on = days and (1 shl i) != 0
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (on) Color(color) else Color(0x14FFFFFF))
                    .clickable { days = days xor (1 shl i) }, contentAlignment = Alignment.Center) {
                    Text(d, color = if (on) StrideColors.Background else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        TextButton(onClick = {
            val m = reminder ?: (9 * 60)
            TimePickerDialog(context, { _, h, min -> reminder = h * 60 + min }, m / 60, m % 60, false).show()
        }) {
            Icon(Icons.Rounded.Notifications, null); Spacer(Modifier.width(8.dp))
            Text(reminder?.let { "Reminder at ${time(it)}" } ?: "Add a reminder")
        }
        if (reminder != null) TextButton(onClick = { reminder = null }) { Text("Remove reminder", color = StrideColors.Muted) }
        Button(onClick = { onSave(start.copy(name = name.trim(), emoji = emoji, color = color, days = days, reminderMinutes = reminder)) },
            enabled = name.isNotBlank() && days != 0, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Lime, contentColor = StrideColors.Background)) { Text("Save") }
        onArchive?.let { TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Archive habit", color = StrideColors.Coral) } }
    }
}

fun schedule(days: Int): String = when (days) {
    0b1111111 -> "Every day"
    0b0011111 -> "Weekdays"
    0b1100000 -> "Weekends"
    else -> Days.shortNames.filterIndexed { i, _ -> days and (1 shl i) != 0 }.joinToString(" ")
}

fun time(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
}
