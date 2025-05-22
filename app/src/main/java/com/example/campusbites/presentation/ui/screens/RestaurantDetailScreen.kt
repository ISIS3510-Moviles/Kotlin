package com.example.campusbites.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
// ... (otras importaciones)
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.FoodSection
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.RestaurantHeader
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.RestaurantTabs
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.BookTableSection
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.ArriveSection
import com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail.ReviewsSection
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.google.firebase.perf.ktx.performance
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun RestaurantDetailScreen(
    authViewModel: AuthViewModel,
    restaurantId: String,
    viewModel: RestaurantDetailViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit,
) {
    val screenLoadTrace = remember {
        Firebase.performance.newTrace("restaurant_detail_screen_load_time")
    }

    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Usar el lastSelectedTabIndex del uiState, que ya viene de DataStore
    var selectedTabIndex by remember(uiState.lastSelectedTabIndex) {
        mutableStateOf(uiState.lastSelectedTabIndex)
    }

    val onSaveRestaurantClick: (String) -> Unit = { rId -> // Renombrado para claridad
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
            authViewModel.updateUser(updatedUser) // Actualiza el estado en AuthViewModel
            // viewModel.onSaveClick(updatedUser) // El ViewModel de detalle no necesita saber sobre esto directamente
            // si la fuente de verdad del usuario es AuthViewModel
        }
    }

    LaunchedEffect(restaurantId) {
        screenLoadTrace.start()
        screenLoadTrace.putAttribute("restaurant_id", restaurantId)
        viewModel.loadRestaurantDetails(restaurantId)
    }

    // No es necesario este LaunchedEffect si uiState.restaurant es la fuente de verdad
    // LaunchedEffect(uiState.restaurant) {
    // if (uiState.restaurant != null) {
    // }
    // }

    uiState.restaurant?.let { restaurant ->
        Column(modifier = Modifier.fillMaxSize().padding(top= 30.dp, start = 16.dp, end = 16.dp)) {
            RestaurantHeader(
                restaurant = restaurant,
                onClick = onSaveRestaurantClick,
                suscribedRestaurantIds = authViewModel.user.value?.suscribedRestaurantIds ?: emptyList(),
                isLoading = uiState.isLoadingNetwork
            )
            Spacer(modifier = Modifier.height(16.dp))
            RestaurantTabs(selectedTabIndex) { index ->
                selectedTabIndex = index
                viewModel.saveSelectedTabIndex(index) // Guardar el tab seleccionado
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
                2 -> ArriveSection() // Asumiendo que ArriveSection usa el mismo ViewModel o no necesita uno específico para el tab
                3 -> ReviewsSection(
                    restaurantDetailViewModel = viewModel,
                    authViewModel = authViewModel
                )
            }
        }
        LaunchedEffect(Unit) { // Este se ejecutará solo una vez cuando la composición entre
            // screenLoadTrace.stop() // Mover esto a onDispose o al final de la carga real
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    DisposableEffect(Unit) {
        onDispose {
            screenLoadTrace.stop() // Asegura que el trace se detenga
        }
    }
}