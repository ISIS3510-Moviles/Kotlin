package com.example.campusbites.presentation.ui.screens

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.campusbites.R
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.presentation.navigation.NavigationRoutes
import com.example.campusbites.presentation.ui.components.IngredientGrid
import com.example.campusbites.presentation.ui.components.PopularIngredientsSection
import com.example.campusbites.presentation.ui.components.ProductListRow
import com.example.campusbites.presentation.ui.components.RestaurantListRow
import com.example.campusbites.presentation.ui.components.SearchBar
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    onRestaurantClick: (String) -> Unit,
    onIngredientClick: (IngredientDomain) -> Unit,
    onProductClick: (String) -> Unit,
    onSearch: (String) -> Unit,
    authViewModel: AuthViewModel
) {
    Log.d("UI", "HomeScreen recomposed")

    // Verificación para Android 13 (API 33) o superior donde se requiere permiso específico para notificaciones
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        // En versiones anteriores no se necesita permiso explícito para notificaciones
        null
    }

    // Estado para controlar si se debe mostrar el contenido principal después de manejar los permisos
    var showMainContent by remember { mutableStateOf(notificationPermission == null) }

    // Solo solicitar permiso de notificaciones en Android 13 o superior
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !showMainContent) {
        val notificationPermissionState = rememberPermissionState(permission = notificationPermission!!)

        // Verificar estado de permiso y mostrar la UI correspondiente
        when {
            notificationPermissionState.status.isGranted -> {
                // Permiso concedido, mostrar contenido principal
                showMainContent = true
            }
            notificationPermissionState.status.shouldShowRationale -> {
                // Mostrar explicación de por qué se necesita el permiso
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Notification permissions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Notifications are important for the proper functioning of the app. Without this permission, some features may not be available.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = { notificationPermissionState.launchPermissionRequest() },
                        modifier = Modifier.padding(top = 8.dp)) {
                        Text("Grant Permission")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showMainContent = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Continue without notifications")
                    }
                }
            }
            else -> {
                // Primera vez que se solicita el permiso
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Bienvenido a Campus Bites",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Para ofrecerte la mejor experiencia, necesitamos permiso para enviarte notificaciones sobre promociones, actualizaciones de pedidos y más.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = { notificationPermissionState.launchPermissionRequest() }) {
                        Text("Permitir notificaciones")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showMainContent = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Continuar sin notificaciones")
                    }
                }
            }
        }
    }

    // Mostrar el contenido principal si no se necesita permiso o si el usuario ya tomó una decisión
    if (showMainContent) {
        // Solicitar permiso de ubicación si es necesario
        val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

        if (!locationPermissionState.status.isGranted) {
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text= "Location permissions are required to display nearby restaurants.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Button(onClick = { locationPermissionState.launchPermissionRequest() },
                    modifier = Modifier.padding(top = 8.dp)) {
                    Text("Grant Permission")
                }
            }
        } else {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val uriHandler = LocalUriHandler.current
            val user by authViewModel.user.collectAsState()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = user?.name ?: "Bienvenido",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                user?.institution?.let {
                                    Text(
                                        text = it.name,
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
                            if (uiState.isLoadingNetwork) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }

                            if (user?.role == "vendor"){


                                IconButton(onClick = {
                                    navController.navigate(NavigationRoutes.VENDOR_SCREEN)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = "Vendor Section"
                                    )
                                }

                                IconButton(onClick = {
                                    navController.navigate(NavigationRoutes.VENDOR_RESERVATIONS)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = "Vendor Reservations"
                                    )
                                }
                            } else {
                                IconButton(onClick = { navController.navigate(NavigationRoutes.RESERVATIONS_SCREEN) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = stringResource(R.string.reservations)
                                    )
                                }
                            }

                            IconButton(onClick = {
                                Firebase.analytics.logEvent("community_updates_button_clicked") {
                                    param("timestamp", System.currentTimeMillis().toString())
                                    param("user_id", user?.id ?: "anonymous")
                                    param("user_institution", user?.institution?.name ?: "none")
                                }

                                Log.i("Analytics", "Community Updates button clicked")

                                navController.navigate(NavigationRoutes.ALERTS_SCREEN)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = stringResource(R.string.notifications)
                                )
                            }
                        }
                    )
                },
                content = { innerPadding ->
                    Box(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()) {
                        if (uiState.isLoadingInitial) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SearchBar(
                                    query = uiState.searchQuery,
                                    onQueryChange = viewModel::onSearchQueryChanged,
                                    onSearch = onSearch,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                if (user?.role == "analyst") {
                                    Button(
                                        onClick = { uriHandler.openUri("https://lookerstudio.google.com/u/0/reporting/4ed6b728-d031-424c-b123-63044acdb870/page/WcSEF") },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Dashboard")
                                    }
                                }

                                if (uiState.popularIngredients.isNotEmpty()) {
                                    PopularIngredientsSection(
                                        ingredients = uiState.popularIngredients,
                                        onIngredientClick = { ingredient ->
                                            viewModel.incrementIngredientClicks(ingredient.id)
                                            onIngredientClick(ingredient)
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }

                                if (uiState.ingredients.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "All Ingredients",
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )

                                        Text(
                                            text = "Explore our complete ingredient selection",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        IngredientGrid(
                                            ingredients = uiState.ingredients,
                                            onIngredientClick = onIngredientClick,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }

                                if (uiState.restaurants.isNotEmpty()) {
                                    RestaurantListRow(
                                        name = stringResource(R.string.near_to_you),
                                        description = stringResource(R.string.the_nearest_restaurants_waiting_for_you),
                                        restaurants = uiState.restaurants,
                                        onRestaurantClick = { restaurantId ->
                                            navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical= 8.dp)
                                    )
                                }

                                if (uiState.products.isNotEmpty()) {
                                    ProductListRow(
                                        name = "All Foods",
                                        description = "Discover all available foods",
                                        products = uiState.products,
                                        onProductClick = onProductClick,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical= 8.dp)
                                    )
                                }

                                user?.let { currentUser ->
                                    if (currentUser.savedProducts.isNotEmpty()) {
                                        ProductListRow(
                                            name = "Saved foods",
                                            description = "The ones according to your preferences",
                                            products = currentUser.savedProducts,
                                            onProductClick = onProductClick,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                if (uiState.recommendationRestaurants.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        RestaurantListRow("Suggested", "The ones according to your preferences", uiState.recommendationRestaurants, onRestaurantClick)
                                    }
                                }

                                if (uiState.errorMessage != null) {
                                    Text(
                                        text = "Error: ${uiState.errorMessage}",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}