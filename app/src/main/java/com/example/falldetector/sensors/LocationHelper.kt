package com.example.falldetector.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(context: Context) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    // @SuppressLint bo sprawdzamy uprawnienia przed wywołaniem
    @SuppressLint("MissingPermission")
    suspend fun getLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cts = CancellationTokenSource()
        fusedClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location -> continuation.resume(location) }
            .addOnFailureListener { continuation.resume(null) }
        continuation.invokeOnCancellation { cts.cancel() }
    }
}
