package com.neomaverick.stride.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.neomaverick.stride.BuildConfig
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.components.Card
import com.neomaverick.stride.ui.components.Pill
import com.neomaverick.stride.ui.theme.StrideColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: StrideViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pro by vm.premium.isPro.collectAsState()
    val healthLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.syncSteps() }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("You", style = MaterialTheme.typography.displaySmall)

        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stride Pro", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (pro) Pill("ACTIVE", StrideColors.Lime)
                }
                Text("Full weekly coach report, focus plan and every insight. A monthly subscription through Google Play.", color = StrideColors.Muted)
                if (!pro) Button(onClick = {
                    scope.launch { if (!vm.premium.buy(context as Activity)) vm.premium.demoUnlock(true) }
                }, colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Lime, contentColor = Color(0xFF0A1020))) { Text("Go Pro") }
                else OutlinedButton(onClick = { vm.premium.demoUnlock(false) }) { Text("Turn off demo Pro") }
            }
        }

        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Steps from Health Connect", style = MaterialTheme.typography.titleLarge)
                Text("Reads your daily steps from Health Connect (Samsung Health, Google Fit, Fitbit and others). Read-only.", color = StrideColors.Muted)
                if (vm.health.available()) Button(onClick = { healthLauncher.launch(vm.health.permissions) },
                    colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Teal, contentColor = Color(0xFF0A1020))) { Text("Connect") }
                else Text("Health Connect isn't on this phone, so the demo shows sample steps.", color = StrideColors.Muted, fontSize = 13.sp)
            }
        }

        Card {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cloud backup", style = MaterialTheme.typography.titleLarge)
                Text(if (BuildConfig.FIREBASE_PROJECT_ID.isBlank()) "Off in this demo build. Everything is stored on the phone and works offline."
                else "On: changes are backed up to Cloud Firestore.", color = StrideColors.Muted)
            }
        }

        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Demo data", style = MaterialTheme.typography.titleLarge)
                Text("Reload four weeks of sample habits, workouts and steps.", color = StrideColors.Muted)
                OutlinedButton(onClick = { vm.resetDemo() }) { Text("Reload demo data") }
            }
        }
        Row { Text("Stride ${BuildConfig.VERSION_NAME}", color = StrideColors.Muted, fontSize = 12.sp); Spacer(Modifier.width(1.dp)) }
    }
}
