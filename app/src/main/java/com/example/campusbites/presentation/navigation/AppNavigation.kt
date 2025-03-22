package com.example.campusbites.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    // Se obtiene el AuthViewModel en el ámbito de MainActivity


    NavGraph(
        navController = navController,
        authViewModel = authViewModel
    )
}
