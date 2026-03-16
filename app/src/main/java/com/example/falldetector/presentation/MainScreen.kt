package com.example.falldetector.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(viewModel: FallViewModel) {
    // collectAsStateWithLifecycle — bezpieczniejsze niż collectAsState,
    // zatrzymuje zbieranie gdy ekran jest niewidoczny (oszczędność baterii)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        HomeContent(fallCount = state.fallCount)

        AnimatedVisibility(
            visible = state.fallDetected,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FallAlertDialog(
                secondsLeft = state.secondsLeft,
                locationText = state.locationText,
                smsStatus = state.smsStatus,
                onDismiss = { viewModel.onDismiss() }
            )
        }
    }
}