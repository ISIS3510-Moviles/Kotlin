package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusbites.presentation.ui.components.CommentCard
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

@Composable
fun ReviewsSection(
    restaurantDetailViewModel: RestaurantDetailViewModel = viewModel()
) {
    val uiState = restaurantDetailViewModel.uiState.collectAsState().value

    // Registrar un evento cada vez que se muestra la sección de reseñas
    LaunchedEffect(Unit) {
        Firebase.analytics.logEvent("restaurant_reviews_checked", null)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Reseñas", style = MaterialTheme.typography.displayMedium)

        LazyColumn {
            items(uiState.reviews) { comment ->
                CommentCard(comment = comment)
            }
        }
    }
}
