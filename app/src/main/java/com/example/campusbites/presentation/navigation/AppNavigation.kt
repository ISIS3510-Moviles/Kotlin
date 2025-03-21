package com.example.campusbites.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    NavGraph(
        navController = navController,
        authViewModel = authViewModel
    )
}
