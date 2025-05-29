package com.example.campusbites.presentation.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.FoodSection
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.RestaurantHeader
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.RestaurantTabs
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.BookTableSection
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.ArriveSection
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.ReviewsSection
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    navController: NavHostController, // Añadido para navegación
    restaurantId: String,
    entrySource: String, // Añadido para saber cómo se llegó aquí
    authViewModel: AuthViewModel,
    viewModel: RestaurantDetailViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit,
) {
    val screenLoadTrace = remember {
        Firebase.performance.newTrace("restaurant_detail_screen_load_time")
    }

    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val analytics = Firebase.analytics

    var selectedTabIndex by remember(uiState.lastSelectedTabIndex) {
        mutableStateOf(uiState.lastSelectedTabIndex)
    }

    // Para registrar el evento de entrada y el tiempo
    val entryTimestamp = remember { System.currentTimeMillis() }
    var hasLoggedAbandonment by remember { mutableStateOf(false) }


    val onSaveRestaurantClick: (String) -> Unit = { rId ->
        coroutineScope.launch {
            val currentUser = authViewModel.user.value
            if (currentUser == null) {
                Log.e("RestaurantDetailScreen", "❌ Usuario no disponible para guardar restaurante")
                return@launch
            }
            val updatedIds = currentUser.suscribedRestaurantIds.toMutableList()
            val alreadySaved = updatedIds.contains(rId)
            if (alreadySaved) {
                updatedIds.remove(rId)
            } else {
                updatedIds.add(rId)
            }
            val updatedUser = currentUser.copy(suscribedRestaurantIds = updatedIds)
            authViewModel.updateUser(updatedUser)
            viewModel.onSaveClick(updatedUser)
        }
    }

    LaunchedEffect(restaurantId, entrySource) { // Se ejecuta cuando restaurantId o entrySource cambian
        screenLoadTrace.start()
        screenLoadTrace.putAttribute("restaurant_id", restaurantId)
        screenLoadTrace.putAttribute("entry_source", entrySource) // Añadir entrySource al trace
        viewModel.loadRestaurantDetails(restaurantId)

        // Registrar evento de entrada
        analytics.logEvent("restaurant_detail_entry") {
            param("restaurant_id", restaurantId)
            param("entry_source", entrySource)
            param("timestamp_entry", entryTimestamp)
            param("user_id", authViewModel.user.value?.id ?: "unknown")
        }
        Log.d("Analytics", "Logged restaurant_detail_entry: $restaurantId, source: $entrySource")
    }

    fun logAbandonment() {
        if (!hasLoggedAbandonment) {
            val abandonmentTimestamp = System.currentTimeMillis()
            val durationOnScreenMs = abandonmentTimestamp - entryTimestamp
            analytics.logEvent("restaurant_detail_abandonment") {
                param("restaurant_id", restaurantId)
                param("entry_source", entrySource) // Usar el entrySource recordado
                param("timestamp_abandonment", abandonmentTimestamp)
                param("duration_on_screen_ms", durationOnScreenMs)
                param("user_id", authViewModel.user.value?.id ?: "unknown")
            }
            hasLoggedAbandonment = true
            Log.d("Analytics", "Logged restaurant_detail_abandonment: $restaurantId, source: $entrySource, duration: $durationOnScreenMs ms")
        }
    }

    // Manejar el botón de retroceso del sistema
    BackHandler {
        logAbandonment()
        navController.popBackStack()
    }

    // Este DisposableEffect es un seguro para cuando el Composable se va por otras razones
    DisposableEffect(Unit) {
        onDispose {
            logAbandonment() // Intenta registrar si aún no se ha hecho
            screenLoadTrace.stop()
        }
    }

    Scaffold( // Añadido Scaffold para la TopAppBar
        topBar = {
            TopAppBar(
                title = { Text(uiState.restaurant?.name ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        logAbandonment()
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        uiState.restaurant?.let { restaurant ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Usar paddingValues del Scaffold
                    .padding(horizontal = 16.dp) // Padding horizontal general
            ) {
                // RestaurantHeader ya no necesita estar en un padding adicional si Column lo tiene
                RestaurantHeader(
                    restaurant = restaurant,
                    onClick = onSaveRestaurantClick,
                    suscribedRestaurantIds = authViewModel.user.value?.suscribedRestaurantIds ?: emptyList(),
                    isLoading = uiState.isLoadingNetwork
                )
                Spacer(modifier = Modifier.height(16.dp))
                RestaurantTabs(selectedTabIndex) { index ->
                    selectedTabIndex = index
                    viewModel.saveSelectedTabIndex(index)
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTabIndex) {
                    0 -> FoodSection(
                        popularProducts = uiState.popularProducts,
                        affordableProducts = uiState.under20Products,
                        onProductClick = onProductClick
                    )
                    1 -> BookTableSection(
                        authViewModel = authViewModel,
                        restaurant = restaurant,
                        restaurantDetailViewModel = viewModel
                    )
                    2 -> ArriveSection()
                    3 -> ReviewsSection(
                        restaurantDetailViewModel = viewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Usar paddingValues también aquí
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}