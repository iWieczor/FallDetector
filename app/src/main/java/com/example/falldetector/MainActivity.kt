package com.example.falldetector

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.falldetector.sensors.FallDetector
import com.example.falldetector.presentation.FallViewModel
import com.example.falldetector.presentation.MainScreen

class MainActivity : ComponentActivity() {

    // viewModels() tworzy ViewModel i automatycznie zarządza jego cyklem życia
    private val viewModel: FallViewModel by viewModels()
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

        // FallDetector wywołuje teraz metodę ViewModelu — nie manipuluje stanem bezpośrednio
        fallDetector = FallDetector(this) {
            viewModel.onFallDetected()
        }

        setContent {
            MainScreen(viewModel = viewModel)
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