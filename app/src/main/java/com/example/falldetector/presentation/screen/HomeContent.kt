package com.example.falldetector.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeContent(fallCount: Int) {
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
}