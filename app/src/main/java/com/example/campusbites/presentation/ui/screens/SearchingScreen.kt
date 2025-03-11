package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.presentation.ui.viewmodels.HomeViewModel

@Composable
fun SearchingScreen(
    query: String,
    onRestaurantClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val matchingRestaurants = uiState.restaurants.filter { restaurant ->
        restaurant.name.contains(query, ignoreCase = true) ||
                restaurant.description.contains(query, ignoreCase = true)
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Resultados para \"$query\"",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (matchingRestaurants.isEmpty()) {
            Text(
                text = "No se encontraron restaurantes",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            val bestMatch = matchingRestaurants.first()
            Text(
                text = "Mejor coincidencia:",
                style = MaterialTheme.typography.titleMedium
            )
            RestaurantSearchResultItem(
                restaurantDomain = bestMatch,
                onClick = { onRestaurantClick(bestMatch.id.toString()) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (matchingRestaurants.size > 1) {
                Text(
                    text = "Otros resultados:",
                    style = MaterialTheme.typography.titleMedium
                )
                matchingRestaurants.drop(1).forEach { restaurant ->
                    RestaurantSearchResultItem(
                        restaurantDomain = restaurant,
                        onClick = { onRestaurantClick(restaurant.id.toString()) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun RestaurantSearchResultItem(
    restaurantDomain: RestaurantDomain,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            /*
            Image(
                painter = painterResource(id = restaurant.profilePhoto.id.toInt()),
                contentDescription = "Restaurant Logo",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )

             */
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = restaurantDomain.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "⭐ ${restaurantDomain.rating} | ${restaurantDomain.description} km",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = restaurantDomain.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}