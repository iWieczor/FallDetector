package com.example.falldetector.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.falldetector.MainActivity
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
        private const val MONITORING_CHANNEL_ID = "fall_detector_channel"
        private const val ALERT_CHANNEL_ID = "fall_alert_channel"
        private const val MONITORING_NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2
    }

    private lateinit var fallDetector: FallDetector
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        fallDetector = FallDetector(this) {
            fallEvents.tryEmit(Unit)
            showFallAlert()
        }

        serviceScope.launch {
            SettingsDataStore(this@FallDetectorService).settingsFlow.collect { settings ->
                fallDetector.impactThreshold = settings.fallThreshold
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(MONITORING_NOTIFICATION_ID, createMonitoringNotification())
        fallDetector.start()
        return START_STICKY
    }

    override fun onDestroy() {
        fallDetector.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                "Monitorowanie",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alert upadku",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
        )
    }

    private fun createMonitoringNotification(): Notification {
        return NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
            .setContentTitle("Fall Detector")
            .setContentText("Monitorowanie aktywne")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun showFallAlert() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Wykryto upadek!")
            .setContentText("Naciśnij aby potwierdzić stan zdrowia")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(ALERT_NOTIFICATION_ID, notification)
    }
}
