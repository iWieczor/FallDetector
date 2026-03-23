package com.example.falldetector

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.falldetector.presentation.viewmodel.FallViewModel
import com.example.falldetector.presentation.screen.MainScreen
import com.example.falldetector.presentation.screen.SettingsScreen
import com.example.falldetector.presentation.viewmodel.SettingsViewModel
import com.example.falldetector.sensors.FallDetector
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val fallViewModel: FallViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private lateinit var fallDetector: FallDetector

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS
            )
        )

        fallDetector = FallDetector(this) { fallViewModel.onFallDetected() }

        // Obserwuj zmiany w settingsach i po zmianie wysyla nowy prod do FallDetectora
        lifecycleScope.launch {
            settingsViewModel.settings.collect { settings ->
                fallDetector.impactThreshold = settings.fallThreshold
            }
        }

        setContent {
            // NavController zarządza nawigacją między ekranami
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    MainScreen(
                        viewModel = fallViewModel,
                        settingsViewModel = settingsViewModel,  // dodaj
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fallDetector.start()
    }

    override fun onPause() {
        super.onPause()
        fallDetector.stop()
    }
}