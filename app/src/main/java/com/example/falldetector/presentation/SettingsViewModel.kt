package com.example.falldetector.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetector.helpers.SettingsDataStore
import com.example.falldetector.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application)

    // stateIn konwertuje Flow na StateFlow którego Compose może słuchać
    val settings: StateFlow<UserSettings> = dataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    fun saveEmergencyNumber(number: String) {
        viewModelScope.launch {
            dataStore.saveEmergencyNumber(number)
        }
    }

    fun saveCountdownSeconds(seconds: Int) {
        viewModelScope.launch {
            dataStore.saveCountdownSeconds(seconds)
        }
    }

    fun saveFallThreshold(threshold: Float) {
        viewModelScope.launch {
            dataStore.saveFallThreshold(threshold)
        }
    }
}