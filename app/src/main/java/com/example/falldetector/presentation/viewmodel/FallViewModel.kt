package com.example.falldetector.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetector.helpers.SettingsDataStore
import com.example.falldetector.model.FallState
import com.example.falldetector.services.FallDetectorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FallViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(FallState())
    val uiState: StateFlow<FallState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            FallDetectorService.fallEvents.collect { onFallDetected() }
        }
        viewModelScope.launch {
            FallDetectorService.locationTextEvents.collect { text ->
                _uiState.update { it.copy(locationText = text) }
            }
        }
        viewModelScope.launch {
            FallDetectorService.smsStatusEvents.collect { text ->
                _uiState.update { it.copy(smsStatus = text) }
            }
        }
    }

    fun onFallDetected() {
        if (_uiState.value.fallDetected) return
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                fallDetected = true,
                fallCount = it.fallCount + 1,
                locationText = null,
                smsStatus = null,
            )
        }
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob = viewModelScope.launch {
            val seconds = dataStore.settingsFlow.first().countdownSeconds
            _uiState.update { it.copy(secondsLeft = seconds, totalSeconds = seconds) }

            repeat(seconds) { tick ->
                delay(1000L)
                _uiState.update { it.copy(secondsLeft = seconds - 1 - tick) }
            }
        }
    }

    fun onDismiss() {
        countdownJob?.cancel()
        FallDetectorService.dismissEvents.tryEmit(Unit)
        _uiState.update {
            it.copy(
                fallDetected = false,
                locationText = null,
                smsStatus = null,
            )
        }
    }
}
