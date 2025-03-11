package com.example.campusbites.data.repository

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.example.campusbites.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepositoryImpl @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient
): LocationRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Si se obtuvo una ubicación válida, se reanuda la corrutina con ella
                continuation.resume(location)
            } else {
                // Si lastLocation es nulo, se solicita una actualización de la ubicación
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L) // intervalo en milisegundos
                    .setMinUpdateIntervalMillis(500L)
                    .setMaxUpdateDelayMillis(1000L)
                    .setMaxUpdates(1)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        fusedLocationProviderClient.removeLocationUpdates(this)
                        val loc = locationResult.lastLocation
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            continuation.resumeWithException(Exception("No se pudo obtener la ubicación"))
                        }
                    }
                }

                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // En caso de cancelación, se eliminan las actualizaciones de ubicación
                continuation.invokeOnCancellation {
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                }
            }
        }.addOnFailureListener { exception ->
            // En caso de error, se reanuda la corrutina con la excepción correspondiente
            continuation.resumeWithException(exception)
        }
    }

}