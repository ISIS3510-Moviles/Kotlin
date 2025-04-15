package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.presentation.ui.components.RestaurantCard
import com.example.campusbites.presentation.ui.components.ProductCard
import androidx.compose.ui.graphics.Color
import com.example.campusbites.presentation.ui.viewmodels.SearchingScreenViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.campusbites.presentation.ui.components.CustomIcons
import com.example.campusbites.presentation.ui.components.SearchBar

@Composable
fun SearchingScreen(
    query: String,
    modifier: Modifier = Modifier,
    onRestaurantClick: (String) -> Unit,
    onFoodClick: (String) -> Unit,
    viewModel: SearchingScreenViewModel = hiltViewModel(),
) {


    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("Food") }
    val matchingRestaurants = uiState.filteredRestaurants
    val matchingFoods = uiState.filteredProducts

    LaunchedEffect(query) {
        viewModel.onSearchQueryChanged(query)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Search bar con icono de filtro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSearch = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Food",
                color = if (selectedCategory == "Food") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.clickable { selectedCategory = "Food" }
            )
            Text(
                text = "Restaurants",
                color = if (selectedCategory == "Restaurants") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.clickable { selectedCategory = "Restaurants" }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show list based on category selection
        when (selectedCategory) {
            "Food" -> {
                if (matchingFoods.isNotEmpty()) {
                    matchingFoods.forEach { food ->
                        ProductCard(
                            product = food,
                            onProductClick = { onFoodClick(food.id.toString()) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text("No se encontraron productos", style = MaterialTheme.typography.bodyLarge)
                }
            }
            "Restaurants" -> {
                if (matchingRestaurants.isNotEmpty()) {
                    matchingRestaurants.forEach { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            onRestaurantClick = { onRestaurantClick(restaurant.id.toString()) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text("No se encontraron restaurantes", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}


