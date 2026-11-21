package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.ThemePreferencesRepository
import com.example.domain.model.ThemePreference
import com.example.notifications.PondNotificationManager
import com.example.ui.navigation.DuckLandApp
import com.example.ui.theme.DuckLandTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val preferencesRepository = ThemePreferencesRepository(applicationContext)
        val notificationManager = PondNotificationManager(applicationContext).apply {
            createChannels()
        }

        setContent {
            val themePreference by preferencesRepository.themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)

            DuckLandTheme(themePreference = themePreference) {
                DuckLandApp(
                    preferencesRepository = preferencesRepository,
                    notificationManager = notificationManager
                )
            }
        }
    }
}
