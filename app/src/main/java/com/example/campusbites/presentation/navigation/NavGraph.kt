package com.example.campusbites.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.presentation.ui.screens.AlertCreateScreen
import com.example.campusbites.presentation.ui.screens.AlertsScreen
import com.example.campusbites.presentation.ui.screens.FoodDetailScreen
import com.example.campusbites.presentation.ui.screens.HomeScreen
import com.example.campusbites.presentation.ui.screens.ProfileScreen
import com.example.campusbites.presentation.ui.screens.SearchingScreen

import com.example.campusbites.presentation.ui.screens.RestaurantDetailScreen
import com.example.campusbites.presentation.ui.screens.SignInScreen
import com.example.campusbites.presentation.ui.viewmodels.AlertsViewModel
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

object NavigationRoutes {
    const val HOME_SCREEN = "home_screen"
    const val RESTAURANT_DETAIL = "restaurant_detail/{id}"
    const val PROFILE_SCREEN = "profile_screen"
    const val ALERTS_SCREEN = "alerts_screen"
    const val SEARCHING_SCREEN = "searching_screen/{query}"
    const val SIGNIN_SCREEN = "sign_in"
    const val FOOD_DETAIL = "food_detail/{id}"

    fun createRestaurantDetailRoute(id: String) = "restaurant_detail/$id"
    fun createSearchingRoute(query: String) = "searching_screen/$query"
    fun createFoodDetailRoute(id: String) = "food_detail/$id"
}

@Composable

fun NavGraph(navController: NavHostController, authViewModel: AuthViewModel) {
    val alertsViewModel: AlertsViewModel = hiltViewModel()
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.HOME_SCREEN
    ) {

        composable(NavigationRoutes.SIGNIN_SCREEN){
            SignInScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        // Home Screen
        composable(NavigationRoutes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                onRestaurantClick = { restaurantId ->
                    navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                },
                onIngredientClick = { ingredient ->
                    navController.navigate(NavigationRoutes.createSearchingRoute(ingredient))
                },
                onProductClick = { productId ->
                    navController.navigate(NavigationRoutes.createFoodDetailRoute(productId))
                },
                onSearch = { query ->
                    navController.navigate(NavigationRoutes.createSearchingRoute(query))
                },
                authViewModel = authViewModel
            )
        }

        // Restaurant Detail
        composable(
            route = NavigationRoutes.RESTAURANT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val restaurantId = it.arguments?.getString("id") ?: ""
            RestaurantDetailScreen(restaurantId = restaurantId)
        }

        // Profile Screen
        composable(NavigationRoutes.PROFILE_SCREEN) {
            ProfileScreen(
                navController = navController,
                authViewModel = authViewModel,
            )
        }

        // Alerts Screen
        composable(NavigationRoutes.ALERTS_SCREEN) {
            AlertsScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("alert_create") {
            val restaurants = alertsViewModel.restaurants.collectAsState()
            AlertCreateScreen(
                onBackClick = { navController.popBackStack() },
                onCreateClick = { description, restaurantId ->
                    alertsViewModel.createAlert(description, restaurantId)

                    navController.navigate(NavigationRoutes.HOME_SCREEN)
                },
                restaurants = restaurants.value
            )
        }

        // Searching Screen
        composable(
            route = NavigationRoutes.SEARCHING_SCREEN,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) {
            val query = it.arguments?.getString("query") ?: ""
            SearchingScreen(
                query = query,
                onRestaurantClick = { restaurantId ->
                    navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                },
                onFoodClick = { foodId ->
                    navController.navigate(NavigationRoutes.createFoodDetailRoute(foodId))
                },
            )
        }

        // Food Detail (única definición correcta)
        composable(
            route = NavigationRoutes.FOOD_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val foodId = it.arguments?.getString("id") ?: ""
            FoodDetailScreen(foodId = foodId, authViewModel = authViewModel)
        }
    }
}