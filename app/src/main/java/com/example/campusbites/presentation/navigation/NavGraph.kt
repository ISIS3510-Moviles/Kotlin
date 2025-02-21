package com.example.campusbites.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.example.campusbites.presentation.ui.screens.AlertsScreen
import com.example.campusbites.presentation.ui.screens.HomeScreen
import com.example.campusbites.presentation.ui.screens.ProfileScreen
import com.example.campusbites.presentation.ui.screens.RestaurantDetailScreen

object NavigationRoutes {
    const val HOME_SCREEN = "home_screen"
    const val RESTAURANT_DETAIL = "restaurant_detail/{id}"
    const val PROFILE_SCREEN = "profile_screen"
    const val ALERTS_SCREEN = "alerts_screen"

    fun createRestaurantDetailRoute(id: String) = "restaurant_detail/$id"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.HOME_SCREEN
    ) {
        // Home Screen
        composable(NavigationRoutes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                onRestaurantClick = { restaurantId ->
                    navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                }
            )
        }
        // Restaurant Detail
        composable(
            route = NavigationRoutes.RESTAURANT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("id") ?: ""
            RestaurantDetailScreen(restaurantId = restaurantId)
        }

        // Profile Screen
        composable(NavigationRoutes.PROFILE_SCREEN) {
            ProfileScreen()
        }
        // Alerts Screen
        composable(NavigationRoutes.ALERTS_SCREEN) {
            AlertsScreen()
        }
    }
}
