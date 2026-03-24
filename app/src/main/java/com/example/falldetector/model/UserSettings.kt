package com.example.falldetector.model

data class UserSettings(
    val emergencyNumber: String = "",
    val countdownSeconds: Int = 10,
    val fallThreshold: Float = 25.0f  // próg uderzenia w m/s²
)