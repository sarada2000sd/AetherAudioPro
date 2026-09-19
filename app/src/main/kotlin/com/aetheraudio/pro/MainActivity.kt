package com.aetheraudio.pro

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aetheraudio.pro.data.repository.SettingsRepository
import com.aetheraudio.pro.playback.PlayerController
import com.aetheraudio.pro.ui.navigation.AetherApp
import com.aetheraudio.pro.ui.theme.AetherAudioTheme

class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled reactively by the library screen's empty-state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PlayerController.connect(applicationContext)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settings = remember { SettingsRepository.get(this) }
            val followSystem by settings.followSystemTheme.collectAsState(initial = true)
            val darkPref by settings.darkTheme.collectAsState(initial = true)
            val amoled by settings.amoledTrueBlack.collectAsState(initial = false)
            val isDark = if (followSystem) isSystemInDarkTheme() else darkPref

            AetherAudioTheme(darkTheme = isDark, amoledTrueBlack = amoled) {
                Surface(modifier = Modifier, color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    AetherApp(widthSizeClass = windowSizeClass.widthSizeClass)
                }
            }
            LaunchedEffect(Unit) {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                requestPermission.launch(permission)
            }
        }
    }
}
