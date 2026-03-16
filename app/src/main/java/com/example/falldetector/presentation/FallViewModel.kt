package com.example.falldetector.presentation

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetector.helpers.SettingsDataStore
import com.example.falldetector.helpers.SmsHelper
import com.example.falldetector.model.FallState
import com.example.falldetector.model.UserSettings
import com.example.falldetector.sensors.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FallViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application)
    private val settings = dataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    private val _uiState = MutableStateFlow(FallState())
    val uiState: StateFlow<FallState> = _uiState.asStateFlow()

    private val locationHelper = LocationHelper(application)
    private val smsHelper = SmsHelper()
    private var countdownJob: Job? = null

    fun onFallDetected() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                fallDetected = true,
                fallCount = it.fallCount + 1,
                locationText = null,
                smsStatus = null,
                secondsLeft = settings.value.countdownSeconds
            )
        }
        startCountdown()
    }

    private fun startCountdown() {
        val seconds = settings.value.countdownSeconds
        _uiState.update { it.copy(secondsLeft = seconds) }

        countdownJob = viewModelScope.launch {
            repeat(seconds) { tick ->
                delay(1000L)
                _uiState.update { it.copy(secondsLeft = seconds - 1 - tick) }
            }
            fetchLocationAndSendSms()
        }
    }

    fun onDismiss() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                fallDetected = false,
                locationText = null,
                smsStatus = null,
                secondsLeft = 10
            )
        }
    }

    private fun fetchLocationAndSendSms() {
        val context = getApplication<Application>()

        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocation) {
            _uiState.update {
                it.copy(
                    locationText = "Brak uprawnień do lokalizacji",
                    smsStatus = "SMS nie wysłany"
                )
            }
            return
        }

        locationHelper.getLastLocation { location ->
            // Czytamy numer w momencie wysyłki — zawsze aktualny
            val currentNumber = settings.value.emergencyNumber

            if (location != null) {
                val locationText = "%.6f, %.6f".format(
                    location.latitude,
                    location.longitude
                )
                _uiState.update { it.copy(locationText = locationText) }

                if (hasSms) {
                    smsHelper.sendFallAlert(
                        phoneNumber = currentNumber,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _uiState.update {
                        it.copy(smsStatus = "SMS wysłany na $currentNumber")
                    }
                } else {
                    _uiState.update {
                        it.copy(smsStatus = "Brak uprawnień do SMS")
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        locationText = "Nie udało się pobrać lokalizacji",
                        smsStatus = "SMS nie wysłany — brak GPS"
                    )
                }
            }
        }
    }
}