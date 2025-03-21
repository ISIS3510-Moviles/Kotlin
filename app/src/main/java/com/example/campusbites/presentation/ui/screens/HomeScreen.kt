package com.example.campusbites.presentation.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.R
import com.example.campusbites.presentation.navigation.NavigationRoutes
import com.example.campusbites.presentation.ui.components.IngredientGrid
import com.example.campusbites.presentation.ui.components.ProductListRow
import com.example.campusbites.presentation.ui.components.RestaurantListRow
import com.example.campusbites.presentation.ui.components.SearchBar
import com.example.campusbites.presentation.ui.viewmodels.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    onRestaurantClick: (String) -> Unit, // Usado en el NavGraph
    onIngredientClick: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    // Estado para el permiso de ubicación
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    // Si el permiso no está concedido, se muestra una UI para solicitarlo
    if (!locationPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Se requieren permisos de ubicación para mostrar restaurantes cercanos")
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Conceder Permiso")
            }
        }
    } else {

        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            uiState.user?.let {
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = it.institution.name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate(NavigationRoutes.PROFILE_SCREEN) }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = stringResource(R.string.profile)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(NavigationRoutes.ALERTS_SCREEN) }) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.notifications)
                            )
                        }
                    }
                )
            },
            content = { innerPadding ->
                Column(
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onSearch = onSearch,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    when {
                        uiState.isLoading -> {
                            Column (
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        else -> {

                            IngredientGrid(
                                ingredients = uiState.ingredients,
                                onIngredientClick = onIngredientClick,
                                modifier = Modifier
                                    .padding(4.dp)
                            )

                            RestaurantListRow(
                                name = stringResource(R.string.near_to_you),
                                description = stringResource(R.string.the_nearest_restaurants_waiting_for_you),
                                restaurants = uiState.restaurants,
                                onRestaurantClick = { restaurantId ->
                                    navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                                },
                                modifier = Modifier.padding(8.dp)
                            )

                            uiState.user?.let { user ->
                                ProductListRow(
                                    name = "Saved foods",
                                    description = "The ones according to your preferences",
                                    products = user.savedProducts,
                                    onProductClick = { onProductClick(it) },
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            // Botón para simular un crash y probar Crashlytics
                            Button(
                                onClick = { throw RuntimeException("Crasheo intencional para Crashlytics") },
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(text = "Forzar Crash")
                            }
                        }
                    }

                }
            }
        )
    }

}