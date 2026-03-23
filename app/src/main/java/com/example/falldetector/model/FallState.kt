package com.example.falldetector.model

// Data class opisuje stan całej aplikacji w jednym miejscu.
// Compose przerysuje UI tylko gdy któreś pole się zmieni.
data class FallState(
    val fallDetected: Boolean = false,
    val fallCount: Int = 0,
    val locationText: String? = null,
    val smsStatus: String? = null,
    val secondsLeft: Int = 10,
)