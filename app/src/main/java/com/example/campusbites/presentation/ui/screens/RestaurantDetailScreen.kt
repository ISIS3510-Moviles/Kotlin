package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.google.firebase.perf.ktx.performance
import com.google.firebase.ktx.Firebase

@Composable
fun RestaurantDetailScreen(
    restaurantId: String,
    viewModel: RestaurantDetailViewModel = hiltViewModel()
) {
    // Crear un trace personalizado para medir el tiempo de carga completo de la pantalla
    val screenLoadTrace = remember {
        Firebase.performance.newTrace("restaurant_detail_screen_load_time")
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(restaurantId) {
        screenLoadTrace.start()
        screenLoadTrace.putAttribute("restaurant_id", restaurantId)

        viewModel.loadRestaurantDetails(restaurantId)
    }

    LaunchedEffect(uiState.restaurant) {
        if (uiState.restaurant != null) {
        }
    }

    uiState.restaurant?.let { restaurant ->
        var selectedTabIndex by remember { mutableStateOf(0) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            RestaurantHeader(restaurant)
            Spacer(modifier = Modifier.height(16.dp))
            RestaurantTabs(selectedTabIndex) { index -> selectedTabIndex = index }
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> FoodSection(popularProducts = uiState.popularProducts,
                    affordableProducts = uiState.under20Products,
                    onProductClick = { productId -> /* Acción al hacer clic en un producto */ }
                )
                1 -> BookTableSection()
                2 -> ArriveSection()
                3 -> ReviewsSection()
            }
        }

        LaunchedEffect(Unit) {

            screenLoadTrace.stop()
        }
    } ?: Text("Loading...", modifier = Modifier.fillMaxSize()) // Mensaje de carga
}