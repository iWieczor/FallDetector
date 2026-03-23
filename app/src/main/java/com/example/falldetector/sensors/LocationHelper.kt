package com.example.falldetector.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    // @SuppressLint bo sprawdzamy uprawnienia przed wywołaniem
    @SuppressLint("MissingPermission")
    suspend fun getLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location -> continuation.resume(location) }
            .addOnFailureListener { continuation.resume(null) }
    }
}
