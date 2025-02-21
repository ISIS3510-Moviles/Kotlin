package com.example.campusbites.presentation.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RestaurantDetailScreen(
    restaurantId : String,
    modifier: Modifier = Modifier
){
    Text(
        text = "Restaurant Screen restaurant $restaurantId",
        style = MaterialTheme.typography.headlineMedium

    )
}