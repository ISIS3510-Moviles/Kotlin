package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.R
import com.example.campusbites.presentation.navigation.NavigationRoutes
import com.example.campusbites.presentation.ui.components.FoodListRow
import com.example.campusbites.presentation.ui.components.FoodTagGrid
import com.example.campusbites.presentation.ui.components.RestaurantListRow
import com.example.campusbites.presentation.ui.components.SearchBar
import com.example.campusbites.presentation.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    onRestaurantClick: (String) -> Unit, // Usado en el NavGraph
    onFoodTagClick: (String) -> Unit,
    onFoodClick: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Juan Pérez",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Universidad de Ejemplo",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(NavigationRoutes.PROFILE_SCREEN) }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(R.string.profile)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(NavigationRoutes.ALERTS_SCREEN) }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.notifications)
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = onSearch,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator()
                    }

                    else -> {
                        /*
                        FoodTagGrid(
                            foodTags = uiState.foodTags,
                            onFoodTagClick = onFoodTagClick,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                        */
                        RestaurantListRow(
                            name = stringResource(R.string.near_to_you),
                            description = stringResource(R.string.the_nearest_restaurants_waiting_for_you),
                            restaurants = uiState.restaurants,
                            onRestaurantClick = { restaurantId ->
                                navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                            },
                            modifier = Modifier.padding(4.dp)
                        )

                        /*
                        FoodListRow(
                            name = "Recommended foods",
                            description = "The ones according to your preferences",
                            foods = uiState.foods,
                            onFoodClick = { onFoodClick(it) },
                            modifier = Modifier
                                .padding(4.dp)
                        )
                        */
                    }
                }

            }
        }
    )
}

