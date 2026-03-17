package com.example.falldetector.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.falldetector.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Lokalne stany pól tekstowych — aktualizują się gdy użytkownik pisze
    var numberInput by remember(settings.emergencyNumber) {
        mutableStateOf(settings.emergencyNumber)
    }
    var countdownInput by remember(settings.countdownSeconds) {
        mutableStateOf(settings.countdownSeconds.toString())
    }
    var thresholdSlider by remember(settings.fallThreshold) {
        mutableFloatStateOf(settings.fallThreshold)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ustawienia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF1A1A2E)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Numer alarmowy ──────────────────────────
            SettingsSection(title = "Numer alarmowy") {
                OutlinedTextField(
                    value = numberInput,
                    onValueChange = { numberInput = it },
                    label = { Text("Numer telefonu", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("np. 48512345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColors()
                )
                Text(
                    text = "Format międzynarodowy bez '+', np. 48512345678",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Button(
                    onClick = { viewModel.saveEmergencyNumber(numberInput) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zapisz numer")
                }
            }

            // ── Czas odliczania ─────────────────────────
            SettingsSection(title = "Czas odliczania") {
                OutlinedTextField(
                    value = countdownInput,
                    onValueChange = { countdownInput = it },
                    label = { Text("Sekundy", color = Color.White.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColors()
                )
                Text(
                    text = "Zalecane: od 5 do 30 sekund",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Button(
                    onClick = {
                        val seconds = countdownInput.toIntOrNull()
                        if (seconds != null && seconds in 5..30) {
                            viewModel.saveCountdownSeconds(seconds)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zapisz czas")
                }
            }

            // ── Próg czułości ───────────────────────────
            SettingsSection(title = "Czułość detekcji upadku") {
                Text(
                    text = "Próg uderzenia: ${"%.1f".format(thresholdSlider)} m/s²",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = thresholdSlider,
                    onValueChange = { thresholdSlider = it },
                    onValueChangeFinished = { viewModel.saveFallThreshold(thresholdSlider) },
                    valueRange = 15f..40f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4CAF50),
                        activeTrackColor = Color(0xFF4CAF50),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Wysoka czułość\n(15 m/s²)",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Niska czułość\n(40 m/s²)",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Pomocniczy Composable dla sekcji ustawień
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = Color(0xFF4CAF50),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        content()
    }
}

// Kolory dla OutlinedTextField żeby pasowały do ciemnego motywu
@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF4CAF50),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = Color(0xFF4CAF50)
)