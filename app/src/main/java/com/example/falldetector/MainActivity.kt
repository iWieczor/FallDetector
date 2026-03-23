package com.example.falldetector

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.falldetector.presentation.screen.FallAlertDialog
import com.example.falldetector.presentation.screen.MainScreen
import com.example.falldetector.presentation.screen.SettingsScreen
import com.example.falldetector.presentation.viewmodel.FallViewModel
import com.example.falldetector.presentation.viewmodel.SettingsViewModel
import com.example.falldetector.services.FallDetectorService

class MainActivity : ComponentActivity() {

    private val fallViewModel: FallViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        startForegroundService(Intent(this, FallDetectorService::class.java))

        setContent {
            val navController = rememberNavController()
            val fallState by fallViewModel.uiState.collectAsStateWithLifecycle()

            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            viewModel = fallViewModel,
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

                AnimatedVisibility(
                    visible = fallState.fallDetected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FallAlertDialog(
                        secondsLeft = fallState.secondsLeft,
                        locationText = fallState.locationText,
                        smsStatus = fallState.smsStatus,
                        onDismiss = { fallViewModel.onDismiss() }
                    )
                }
            }
        }
    }

}