package com.example.campusbites.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
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
    // Crear un trace personalizado para medir el tiempo de carga completo de la pantalla
    val screenLoadTrace = remember {
        Firebase.performance.newTrace("restaurant_detail_screen_load_time")
    }



    val uiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val onSaveRestaurantClick: (String) -> Unit = { restaurantId ->
        coroutineScope.launch {
            val currentUser = authViewModel.user.value

            if (currentUser == null) {
                Log.e("RestaurantHeader", "❌ Usuario no disponible")
                return@launch
            }

            val updatedIds = currentUser.suscribedRestaurantIds.toMutableList()
            val alreadySaved = updatedIds.contains(restaurantId)

            if (alreadySaved) {
                updatedIds.remove(restaurantId)
                Log.d("RestaurantHeader", "🗑️ Restaurante eliminado de favoritos")
            } else {
                updatedIds.add(restaurantId)
                Log.d("RestaurantHeader", "✅ Restaurante agregado a favoritos")
            }

            val updatedUser = currentUser.copy(suscribedRestaurantIds = updatedIds)
            authViewModel.updateUser(updatedUser)
            viewModel.onSaveClick(updatedUser)
        }
    }


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

        Column(modifier = Modifier.fillMaxSize().padding(top= 30.dp, start = 16.dp, end = 16.dp)) {
            RestaurantHeader(
                restaurant = restaurant,
                onClick = onSaveRestaurantClick,
                suscribedRestaurantIds = authViewModel.user.value?.suscribedRestaurantIds ?: emptyList(),
                isLoading = uiState.isLoadingNetwork
            )
            Spacer(modifier = Modifier.height(16.dp))
            RestaurantTabs(selectedTabIndex) { index -> selectedTabIndex = index }
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> FoodSection(popularProducts = uiState.popularProducts,
                    affordableProducts = uiState.under20Products,
                    onProductClick =  onProductClick
                )
                1 -> BookTableSection(authViewModel = authViewModel, restaurant = restaurant, restaurantDetailViewModel = viewModel)
                2 -> ArriveSection()
                3 -> ReviewsSection(
                    restaurantDetailViewModel = viewModel,
                    authViewModel = authViewModel
                )
            }
        }

        LaunchedEffect(Unit) {
            screenLoadTrace.stop()
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }

}