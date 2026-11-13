package com.neomaverick.stride.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.data.Workout
import com.neomaverick.stride.data.WorkoutType
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.components.BarChart
import com.neomaverick.stride.ui.components.Card
import com.neomaverick.stride.ui.components.SectionLabel
import com.neomaverick.stride.ui.theme.StrideColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(vm: StrideViewModel) {
    val all by vm.workouts.collectAsState()
    var adding by remember { mutableStateOf(false) }
    val today = Days.today()
    val week = all.filter { it.day > today - 7 }
    val perDay = (today - 6..today).map { d -> all.filter { it.day == d }.sumOf { it.minutes } }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Workouts", style = MaterialTheme.typography.displaySmall) }
            item {
                Card {
                    Column {
                        Row {
                            Stat("${week.sumOf { it.minutes }}", "active min", Modifier.weight(1f))
                            Stat("${week.sumOf { it.calories }}", "kcal", Modifier.weight(1f))
                            Stat("${week.size}", "sessions", Modifier.weight(1f))
                        }
                        Spacer(Modifier.size(14.dp))
                        val max = perDay.max().coerceAtLeast(30).toFloat()
                        BarChart(perDay.map { it / max }, (today - 6..today).map { Days.shortNames[Days.weekdayIndex(it)] })
                    }
                }
            }
            item { SectionLabel("History") }
            items(all.take(40), key = { it.id }) { WorkoutRow(it) }
        }
        ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Log workout") },
            containerColor = StrideColors.Lime, contentColor = StrideColors.Background, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp))
    }
    if (adding) ModalBottomSheet(onDismissRequest = { adding = false }, sheetState = rememberModalBottomSheetState(true), containerColor = StrideColors.Surface) {
        WorkoutEditor { vm.logWorkout(it); adding = false }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) = Column(modifier) {
    Text(value, style = MaterialTheme.typography.headlineSmall)
    Text(label, color = StrideColors.Muted, fontSize = 12.sp)
}

@Composable
private fun WorkoutRow(w: Workout) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.type.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(w.type.label, style = MaterialTheme.typography.titleMedium)
                Text(Days.date(w.day).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)) + " · effort ${w.effort}/5", color = StrideColors.Muted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${w.minutes} min", style = MaterialTheme.typography.titleMedium)
                Text("${w.calories} kcal", color = StrideColors.Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WorkoutEditor(onSave: (Workout) -> Unit) {
    var type by remember { mutableStateOf(WorkoutType.RUN) }
    var minutes by remember { mutableFloatStateOf(30f) }
    var effort by remember { mutableFloatStateOf(3f) }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Log workout", style = MaterialTheme.typography.headlineSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WorkoutType.entries) { t ->
                Text("${t.emoji} ${t.label}", Modifier.clip(RoundedCornerShape(50))
                    .background(if (t == type) StrideColors.Lime else Color(0x14FFFFFF)).clickable { type = t }
                    .padding(horizontal = 14.dp, vertical = 9.dp), color = if (t == type) StrideColors.Background else Color.White)
            }
        }
        SectionLabel("Duration · ${minutes.toInt()} min")
        Slider(minutes, { minutes = it }, valueRange = 5f..120f, steps = 22, colors = SliderDefaults.colors(thumbColor = StrideColors.Lime, activeTrackColor = StrideColors.Lime))
        SectionLabel("Effort · ${effort.toInt()} / 5")
        Slider(effort, { effort = it }, valueRange = 1f..5f, steps = 3, colors = SliderDefaults.colors(thumbColor = StrideColors.Teal, activeTrackColor = StrideColors.Teal))
        val preview = Workout(0, type, Days.today(), minutes.toInt(), effort.toInt())
        Text("≈ ${preview.calories} kcal", color = StrideColors.Muted)
        Button(onClick = { onSave(preview) }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Lime, contentColor = StrideColors.Background)) { Text("Save workout") }
    }
}
