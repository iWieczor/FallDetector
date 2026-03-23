package com.example.falldetector

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionRequest.launch(permissions.toTypedArray())

        startForegroundService(Intent(this, FallDetectorService::class.java))

        val activity = this
        setContent {
            val navController = rememberNavController()
            val fallState by fallViewModel.uiState.collectAsStateWithLifecycle()
            val notificationManager = getSystemService(NotificationManager::class.java)
            var showFullScreenIntentDialog by remember {
                mutableStateOf(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        !notificationManager.canUseFullScreenIntent()
                )
            }

            if (showFullScreenIntentDialog) {
                AlertDialog(
                    onDismissRequest = { showFullScreenIntentDialog = false },
                    title = { Text("Wymagane uprawnienie") },
                    text = { Text("Aby alert upadku mógł pojawić się gdy ekran jest wyłączony, wymagane jest uprawnienie 'Wyświetlanie na pierwszym planie'.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showFullScreenIntentDialog = false
                            activity.startActivity(
                                Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENTS").apply {
                                    data = "package:${activity.packageName}".toUri()
                                }
                            )
                        }) { Text("Przejdź do ustawień") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFullScreenIntentDialog = false }) {
                            Text("Pomiń")
                        }
                    }
                )
            }

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