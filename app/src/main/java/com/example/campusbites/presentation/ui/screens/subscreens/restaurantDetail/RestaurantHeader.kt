package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import android.annotation.SuppressLint
import android.location.Location
import android.Manifest
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun RestaurantHeader(
    restaurant: RestaurantDomain?,
    onClick: (String) -> Unit,
    suscribedRestaurantIds: List<String>,
    isLoading: Boolean
) {
    val context = LocalContext.current
    var userDistance by remember { mutableStateOf<Double?>(null) }
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            requestLocationUpdates(fusedLocationProviderClient) { location ->
                restaurant?.let {
                    userDistance = calculateDistance(location, it)
                }
            }
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        Card(
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 8.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(restaurant.profilePhoto),
                contentDescription = "Restaurant Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (permissionState.status.isGranted) {
                Text(
                    text = userDistance?.let { "${"%.2f".format(it)} m" }
                        ?: "Calculating distance...",
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

            SubscribeButton(
                restaurantId = restaurant.id,
                onClick = onClick,
                suscribedRestaurantIds = suscribedRestaurantIds
            )
        }

    }
}

@SuppressLint("MissingPermission")
fun requestLocationUpdates(
    fusedLocationProviderClient: FusedLocationProviderClient,
    onLocationReceived: (Location) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                Log.d("RestaurantHeader", "Updated location: ${it.latitude}, ${it.longitude}")
                onLocationReceived(it)
            }
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

@Composable
fun SubscribeButton(
    restaurantId: String,
    suscribedRestaurantIds: List<String>,
    onClick: (String) -> Unit
) {
    var isSubscribed by remember {
        mutableStateOf(suscribedRestaurantIds.contains(restaurantId))
    }

    FilledTonalButton(
        onClick = {
            onClick(restaurantId)
            isSubscribed = !isSubscribed
        }
    ) {
        Icon(
            imageVector = if (isSubscribed) Icons.Filled.Star else Icons.Default.Star,
            contentDescription = if (isSubscribed) "Unsubscribe Icon" else "Subscribe Icon",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isSubscribed) "Unsubscribe" else "Subscribe")
    }
}