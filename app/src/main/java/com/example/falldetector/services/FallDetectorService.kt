package com.example.falldetector.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.falldetector.MainActivity
import com.example.falldetector.R
import com.example.falldetector.helpers.SettingsDataStore
import com.example.falldetector.helpers.SmsHelper
import com.example.falldetector.model.UserSettings
import com.example.falldetector.sensors.FallDetector
import com.example.falldetector.sensors.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FallDetectorService : Service() {

    companion object {
        val fallEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val locationTextEvents = MutableSharedFlow<String?>(extraBufferCapacity = 1)
        val smsStatusEvents = MutableSharedFlow<String?>(extraBufferCapacity = 1)
        val dismissEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        private const val MONITORING_CHANNEL_ID = "fall_detector_channel"
        private const val ALERT_CHANNEL_ID = "fall_alert_channel"
        private const val MONITORING_NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2
    }

    private lateinit var fallDetector: FallDetector
    private lateinit var locationHelper: LocationHelper
    private val smsHelper by lazy { SmsHelper(this) }
    private val dataStore by lazy { SettingsDataStore(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var fallHandlingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        locationHelper = LocationHelper(this)

        fallDetector = FallDetector(this) {
            fallEvents.tryEmit(Unit)
            showFallAlert()
            handleFall()
        }

        serviceScope.launch {
            dataStore.settingsFlow.collect { settings ->
                fallDetector.impactThreshold = settings.fallThreshold
            }
        }

        serviceScope.launch {
            dismissEvents.collect {
                fallHandlingJob?.cancel()
            }
        }

        fallDetector.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(MONITORING_NOTIFICATION_ID, createMonitoringNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        fallDetector.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleFall() {
        fallHandlingJob?.cancel()
        fallHandlingJob = serviceScope.launch {
            val settings = dataStore.settingsFlow.first()
            delay(settings.countdownSeconds * 1000L)
            fetchLocationAndSendSms(settings)
        }
    }

    private suspend fun fetchLocationAndSendSms(settings: UserSettings) {

        val hasLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocation) {
            locationTextEvents.emit("Brak uprawnień do lokalizacji")
            smsStatusEvents.emit("SMS nie wysłany")
            return
        }

        val location = locationHelper.getLocation()

        if (location != null) {
            val locationText = "%.6f, %.6f".format(location.latitude, location.longitude)
            locationTextEvents.emit(locationText)

            if (hasSms) {
                smsHelper.sendFallAlert(
                    phoneNumber = settings.emergencyNumber,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                smsStatusEvents.emit("SMS wysłany na ${settings.emergencyNumber}")
            } else {
                smsStatusEvents.emit("Brak uprawnień do SMS")
            }
        } else {
            locationTextEvents.emit("Nie udało się pobrać lokalizacji")
            smsStatusEvents.emit("SMS nie wysłany — brak GPS")
        }
    }

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
            .setTimeoutAfter(10_000)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(ALERT_NOTIFICATION_ID, notification)
    }
}
