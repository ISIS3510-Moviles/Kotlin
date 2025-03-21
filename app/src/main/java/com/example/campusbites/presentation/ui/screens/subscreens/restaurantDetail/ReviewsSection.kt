package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusbites.presentation.ui.components.CommentCard
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel

@Composable
fun ReviewsSection(
    restaurantDetailViewModel: RestaurantDetailViewModel = viewModel()
) {
    val uiState = restaurantDetailViewModel.uiState.collectAsState().value

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Reseñas", style = MaterialTheme.typography.displayMedium)

        LazyColumn {
            items(uiState.reviews) { comment ->
                CommentCard(comment = comment)
            }
        }
    }
}
