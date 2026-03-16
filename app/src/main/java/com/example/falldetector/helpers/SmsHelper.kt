package com.example.falldetector.helpers

import android.telephony.SmsManager

class SmsHelper {

    fun sendFallAlert(phoneNumber: String, latitude: Double, longitude: Double) {
        val message = """
            Lokalizacja:
            https://maps.google.com/?q=$latitude,$longitude
        """.trimIndent()

        try {
            val smsManager = SmsManager.getDefault()
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
            e.printStackTrace()
        }
    }
}