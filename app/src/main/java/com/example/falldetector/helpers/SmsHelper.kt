package com.example.falldetector.helpers

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

class SmsHelper(private val context: Context) {

    fun sendFallAlert(phoneNumber: String, latitude: Double, longitude: Double) {
        val message = """
            Lokalizacja:
            https://maps.google.com/?q=$latitude,$longitude
        """.trimIndent()

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            // divideMessage obsługuje SMS dłuższy niż 160 znaków
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                null,
                null
            )
        } catch (e: Exception) {
            Log.e("SmsHelper", "Błąd wysyłania SMS na $phoneNumber", e)
        }
    }
}