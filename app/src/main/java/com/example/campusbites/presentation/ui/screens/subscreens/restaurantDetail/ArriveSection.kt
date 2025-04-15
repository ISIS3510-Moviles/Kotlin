package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArriveSection() {
    val viewModel: RestaurantDetailViewModel = hiltViewModel()
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val restaurant = viewModel.uiState.collectAsState().value.restaurant

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    if (permissionState.status.isGranted) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                MapView(viewModel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            restaurant?.routeIndications?.let { indications ->
                Text(
                    text = "How to get there: $indications",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    } else {
        Text(text = "Location permission is required to display the map")
    }
}

@Composable
fun MapView(viewModel: RestaurantDetailViewModel) {
    val restaurant = viewModel.uiState.collectAsState().value.restaurant

    val defaultLocation = LatLng(4.603085, -74.065274)
    val targetLocation = restaurant?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation, 16f)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        cameraPositionState = cameraPositionState
    ) {
        restaurant?.let {
            Marker(
                state = rememberMarkerState(position = LatLng(it.latitude, it.longitude)),
                title = it.name,
                snippet = "Tap for more info"
            )
        }
    }
}
