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


    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            viewModel.performSearch(query)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { newQuery ->
                    viewModel.updateSearchQuery(newQuery)
                                },
                onSearch = { newQuery -> viewModel.performSearch(newQuery) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Text(
                text = "Error: ${uiState.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedCategory) {
                    "Food" -> {
                        if (uiState.filteredProducts.isNotEmpty()) {
                            uiState.filteredProducts.forEach { food ->
                                ProductCard(
                                    product = food,
                                    onProductClick = { onFoodClick(food.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            Text("No products found matching '$query'", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    "Restaurants" -> {
                        if (uiState.filteredRestaurants.isNotEmpty()) {
                            uiState.filteredRestaurants.forEach { restaurant ->
                                RestaurantCard(
                                    restaurant = restaurant,
                                    onRestaurantClick = { onRestaurantClick(restaurant.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            Text("No restaurants found matching '$query'", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}