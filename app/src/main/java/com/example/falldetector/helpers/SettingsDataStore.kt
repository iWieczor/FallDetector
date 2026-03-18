package com.example.falldetector.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.falldetector.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Tworzy jeden globalny DataStore dla całej aplikacji
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val EMERGENCY_NUMBER = stringPreferencesKey("emergency_number") //Duze litery bo to stala wskazujaca miejsce gdzie sa zmienne
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val FALL_THRESHOLD = floatPreferencesKey("fall_threshold")
    }

    // Flow automatycznie emituje nowe wartości gdy ustawienia się zmienią
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            emergencyNumber = prefs[EMERGENCY_NUMBER] ?: "48123456789",
            countdownSeconds = prefs[COUNTDOWN_SECONDS] ?: 10,
            fallThreshold = prefs[FALL_THRESHOLD] ?: 25.0f
        )
    }

    suspend fun saveEmergencyNumber(number: String) {
        context.dataStore.edit { prefs ->
            prefs[EMERGENCY_NUMBER] = number
        }
    }

    suspend fun saveCountdownSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[COUNTDOWN_SECONDS] = seconds
        }
    }

    suspend fun saveFallThreshold(threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[FALL_THRESHOLD] = threshold
        }
    }
}