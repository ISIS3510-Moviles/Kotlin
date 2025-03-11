package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FoodDetailScreen(
    foodId: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Food Detail Screen for Food ID: $foodId",
        modifier = Modifier
            .padding(20.dp)
    )
}