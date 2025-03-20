package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.campusbites.domain.model.RestaurantDomain

@Composable
fun RestaurantHeader(restaurant: RestaurantDomain) {
    Column(modifier = Modifier.padding(16.dp)) {
        Image(
            painter = rememberImagePainter(restaurant.profilePhoto),
            contentDescription = "Restaurant Image",
            modifier = Modifier.size(100.dp).padding(bottom = 8.dp)
        )

        Text(
            text = restaurant.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "${"CALCULAR DISTANCIA"} m",
            fontWeight = FontWeight.Bold,
            color = Color.Blue
        )

        Button(onClick = { /* TODO: Implement subscribe action */ }) {
            Text("Subscribe")
        }
    }
}