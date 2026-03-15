package com.example.falldetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var fallDetector: FallDetector
    private lateinit var locationHelper: LocationHelper
    private val smsHelper = SmsHelper()

    //##########################################
    //##########################################
    //##########################################
    // USTAWCIE DOBRY NUMER!!!!!!!!!!!!!!!!!!!!!
    //##########################################
    //##########################################
    //##########################################
    private val emergencyNumber = "48123456789"

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* obsługujemy w momencie potrzeby */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS
            )
        )

        var fallDetected by mutableStateOf(false)
        var fallCount by mutableStateOf(0)
        var locationText by mutableStateOf<String?>(null)
        var smsSent by mutableStateOf(false)        // ← nowe: czy SMS wysłany
        var smsStatus by mutableStateOf<String?>(null)  // ← nowe: info dla użytkownika

        locationHelper = LocationHelper(this)

        fallDetector = FallDetector(this) {
            runOnUiThread {
                fallDetected = true
                fallCount++
                locationText = null
                smsSent = false
                smsStatus = null
            }
        }

        setContent {
            FallDetectorApp(
                fallDetected = fallDetected,
                fallCount = fallCount,
                locationText = locationText,
                smsStatus = smsStatus,
                onDismiss = {
                    fallDetected = false
                    locationText = null
                    smsSent = false
                    smsStatus = null
                },
                onCountdownFinished = {
                    val hasLocationPermission = ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    val hasSmsPermission = ContextCompat.checkSelfPermission(
                        this, Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasLocationPermission) {
                        locationHelper.getLastLocation { location ->
                            runOnUiThread {
                                if (location != null) {
                                    locationText = "%.6f, %.6f".format(
                                        location.latitude,
                                        location.longitude
                                    )

                                    // Wyślij SMS jeśli mamy uprawnienie i jeszcze nie wysłano
                                    if (hasSmsPermission && !smsSent) {
                                        smsHelper.sendFallAlert(
                                            phoneNumber = emergencyNumber,
                                            latitude = location.latitude,
                                            longitude = location.longitude
                                        )
                                        smsSent = true
                                        smsStatus = "SMS wysłany na $emergencyNumber"
                                    } else if (!hasSmsPermission) {
                                        smsStatus = "Brak uprawnień do SMS"
                                    }

                                } else {
                                    locationText = "Nie udało się pobrać lokalizacji"
                                    smsStatus = "SMS nie wysłany — brak GPS"
                                }
                            }
                        }
                    } else {
                        locationText = "Brak uprawnień do lokalizacji"
                        smsStatus = "SMS nie wysłany — brak uprawnień GPS"
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        fallDetector.start()
    }

    override fun onPause() {
        super.onPause()
        fallDetector.stop()
    }
}

// ─────────────────────────────────────────────
// UI
// ─────────────────────────────────────────────

@Composable
fun FallDetectorApp(
    fallDetected: Boolean,
    fallCount: Int,
    locationText: String?,
    smsStatus: String?,
    onDismiss: () -> Unit,
    onCountdownFinished: () -> Unit
) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "📱", fontSize = 64.sp)
                Text(
                    text = "Fall Detector",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monitorowanie aktywne",
                    color = Color(0xFF4CAF50),
                    fontSize = 16.sp
                )
                if (fallCount > 0) {
                    Text(
                        text = "Wykryte upadki: $fallCount",
                        color = Color(0xFFFF9800),
                        fontSize = 14.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = fallDetected,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FallAlertDialog(
                    locationText = locationText,
                    smsStatus = smsStatus,
                    onDismiss = onDismiss,
                    onCountdownFinished = onCountdownFinished
                )
            }
        }
    }
}

@Composable
fun FallAlertDialog(
    locationText: String?,
    smsStatus: String?,
    onDismiss: () -> Unit,
    onCountdownFinished: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(10) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        onCountdownFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "⚠️", fontSize = 56.sp)
                Text(
                    text = "WYKRYTO UPADEK!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Czy wszystko w porządku?",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                LinearProgressIndicator(
                    progress = { secondsLeft / 10f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Text(
                    text = when {
                        secondsLeft > 0 -> "SMS zostanie wysłany za $secondsLeft s..."
                        locationText == null -> "Pobieranie lokalizacji..."
                        else -> "Gotowe"
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                if (locationText != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "📍 Twoja lokalizacja",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = locationText,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (smsStatus != null) {
                    Text(
                        text = smsStatus,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFB71C1C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Wszystko OK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}