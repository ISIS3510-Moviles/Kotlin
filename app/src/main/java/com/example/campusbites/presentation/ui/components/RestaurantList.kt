package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.campusbites.domain.model.Restaurant

@Composable
fun RestaurantList(
    restaurants: List<Restaurant>,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(restaurants) { restaurant ->
            RestaurantCard(
                restaurant = restaurant,
                onRestaurantClick = onRestaurantClick
            )
        }
    }
}