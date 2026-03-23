package com.example.falldetector.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class FallDetector(
    context: Context,
    private val onFallDetected: () -> Unit
) : SensorEventListener {

    // Progi detekcji trzeba sprawdzić eksperymentalnie, jako prototyp moze byc jak jest w komentarzach
    companion object {
        private const val FREE_FALL_THRESHOLD = 3.0f   // 3m/s²
        private const val FALL_TIME_WINDOW = 1000L     // 1000ms
        private const val MIN_FALL_DURATION = 200L     // min. 200ms
    }

    var impactThreshold: Float = 25.0f

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var freeFallDetectedAt: Long = 0
    private var isWaitingForImpact = false

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Oblicz całkowite przyspieszenie (wektor wypadkowy)
        val totalAcceleration = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()

        when {
            // FAZA 1: Wykrycie swobodnego spadku
            totalAcceleration < FREE_FALL_THRESHOLD && !isWaitingForImpact -> {
                freeFallDetectedAt = now
                isWaitingForImpact = true
            }

            // FAZA 2: Wykrycie uderzenia po spadku (w oknie czasowym)
            isWaitingForImpact && totalAcceleration > impactThreshold -> {
                val timeSinceFall = now - freeFallDetectedAt
                if (timeSinceFall in MIN_FALL_DURATION..FALL_TIME_WINDOW) {
                    isWaitingForImpact = false
                    onFallDetected()
                }
            }

            // Reset jeśli minął czas okna
            isWaitingForImpact && (now - freeFallDetectedAt) > FALL_TIME_WINDOW -> {
                isWaitingForImpact = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nie potrzebujemy tej metody, ale musimy ją zaimplementować
    }
}