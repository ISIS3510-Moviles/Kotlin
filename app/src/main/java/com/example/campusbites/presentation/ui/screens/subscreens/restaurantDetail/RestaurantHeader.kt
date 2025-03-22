package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import android.annotation.SuppressLint
import android.location.Location
import android.Manifest
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.campusbites.domain.model.RestaurantDomain
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission") // Se maneja correctamente el permiso
@Composable
fun RestaurantHeader(restaurant: RestaurantDomain) {
    val context = LocalContext.current
    var userDistance by remember { mutableStateOf<Double?>(null) }
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coroutineScope = rememberCoroutineScope()

    // Pedir permiso si aún no se ha concedido
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    if (permissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                Log.d("RestaurantHeader", "Checking last known location...")

                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            Log.d("RestaurantHeader", "Last known location found: ${location.latitude}, ${location.longitude}")
                            userDistance = calculateDistance(location, restaurant)
                        } else {
                            Log.d("RestaurantHeader", "Last known location is null, requesting update...")
                            requestLocationUpdate(fusedLocationProviderClient) { updatedLocation ->
                                userDistance = calculateDistance(updatedLocation, restaurant)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("RestaurantHeader", "Error getting last location: ${exception.localizedMessage}")
                        requestLocationUpdate(fusedLocationProviderClient) { updatedLocation ->
                            userDistance = calculateDistance(updatedLocation, restaurant)
                        }
                    }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Image(
            painter = rememberAsyncImagePainter(restaurant.profilePhoto),
            contentDescription = "Restaurant Image",
            modifier = Modifier.size(100.dp).padding(bottom = 8.dp)
        )

        Text(
            text = restaurant.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (permissionState.status.isGranted) {
            Text(
                text = userDistance?.let { "${"%.2f".format(it)} m" } ?: "Calculating distance...",
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
        } else {
            Text(
                text = "Location permission required to show distance",
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }

        Button(onClick = { /* TODO: Implement subscribe action */ }) {
            Text("Subscribe")
        }
    }
}

@SuppressLint("MissingPermission")
fun requestLocationUpdate(
    fusedLocationProviderClient: FusedLocationProviderClient,
    onLocationReceived: (Location) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            if (lastLocation != null) {
                Log.d("RestaurantHeader", "Updated location received: ${lastLocation.latitude}, ${lastLocation.longitude}")
                onLocationReceived(lastLocation)
            } else {
                Log.e("RestaurantHeader", "Failed to get updated location")
            }
            fusedLocationProviderClient.removeLocationUpdates(this) // Detener actualizaciones
        }
    }
    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
}

fun calculateDistance(userLocation: Location, restaurant: RestaurantDomain): Double {
    val restaurantLocation = Location("").apply {
        latitude = restaurant.latitude
        longitude = restaurant.longitude
    }
    return userLocation.distanceTo(restaurantLocation).toDouble()
}
