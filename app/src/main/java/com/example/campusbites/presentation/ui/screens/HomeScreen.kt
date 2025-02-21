package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.presentation.ui.components.RestaurantList
import com.example.campusbites.presentation.navigation.NavigationRoutes
import com.example.campusbites.presentation.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
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
                            contentDescription = "Perfil"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(NavigationRoutes.ALERTS_SCREEN) }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificaciones"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                else -> {
                    RestaurantList(
                        restaurants = uiState.restaurants,
                        onRestaurantClick = onRestaurantClick,
                        modifier = modifier.padding(innerPadding)
                    )
                }
            }
        }
    )
}

