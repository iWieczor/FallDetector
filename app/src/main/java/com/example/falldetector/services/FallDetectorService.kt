package com.example.falldetector.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.falldetector.R
import com.example.falldetector.helpers.SettingsDataStore
import com.example.falldetector.sensors.FallDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FallDetectorService : Service() {

    companion object {
        val fallEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        private const val CHANNEL_ID = "fall_detector_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var fallDetector: FallDetector
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        fallDetector = FallDetector(this) {
            fallEvents.tryEmit(Unit)
        }

        serviceScope.launch {
            SettingsDataStore(this@FallDetectorService).settingsFlow.collect { settings ->
                fallDetector.impactThreshold = settings.fallThreshold
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        fallDetector.start()
        return START_STICKY
    }

    override fun onDestroy() {
        fallDetector.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Fall Detector",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fall Detector")
            .setContentText("Monitorowanie aktywne")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
