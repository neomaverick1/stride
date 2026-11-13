package com.neomaverick.stride

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.neomaverick.stride.ui.StrideRoot
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.theme.StrideTheme

class MainActivity : ComponentActivity() {
    private val vm: StrideViewModel by viewModels()
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        vm.premium.connect()
        setContent { StrideTheme { StrideRoot(vm) } }
    }

    override fun onResume() { super.onResume(); vm.syncSteps() }
}
