package com.example.campusbites.presentation.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.campusbites.presentation.ui.screens.AlertCreateScreen
import com.example.campusbites.presentation.ui.screens.AlertsScreen
import com.example.campusbites.presentation.ui.screens.DraftAlertsScreen
import com.example.campusbites.presentation.ui.screens.EditRestaurantScreen
import com.example.campusbites.presentation.ui.screens.FoodDetailScreen
import com.example.campusbites.presentation.ui.screens.HomeScreen
import com.example.campusbites.presentation.ui.screens.ManageProductsScreen // Nuevo
import com.example.campusbites.presentation.ui.screens.ProductFormScreen // Nuevo
import com.example.campusbites.presentation.ui.screens.ProfileScreen
import com.example.campusbites.presentation.ui.screens.ReservationsScreen
import com.example.campusbites.presentation.ui.screens.ReservationsVendorScreen
import com.example.campusbites.presentation.ui.screens.SearchingScreen
import com.example.campusbites.presentation.ui.screens.RestaurantDetailScreen
import com.example.campusbites.presentation.ui.screens.VendorScreen
import com.example.campusbites.presentation.ui.viewmodels.AlertsViewModel
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

object NavigationRoutes {
    const val HOME_SCREEN = "home_screen"
    const val RESTAURANT_DETAIL = "restaurant_detail/{id}"
    const val PROFILE_SCREEN = "profile_screen"
    const val ALERTS_SCREEN = "alerts_screen"
    const val SEARCHING_SCREEN = "searching_screen/{query}"
    const val FOOD_DETAIL = "food_detail/{id}"
    const val RESERVATIONS_SCREEN = "reservations_screen"
    const val VENDOR_SCREEN = "vendor_section_screen" // Esta pantalla se convertirá en "ManageRestaurantScreen"
    const val VENDOR_RESERVATIONS = "vendor_reservations_screen"
    const val MANAGE_PRODUCTS_SCREEN = "manage_products_screen/{restaurantId}" // Nuevo
    const val PRODUCT_FORM_SCREEN = "product_form_screen/{restaurantId}?productId={productId}" // Nuevo, productId es opcional
    const val EDIT_RESTAURANT_SCREEN = "edit_restaurant_screen/{restaurantId}"

    fun createRestaurantDetailRoute(id: String) = "restaurant_detail/$id"
    fun createSearchingRoute(query: String) = "searching_screen/$query"
    fun createFoodDetailRoute(id: String) = "food_detail/$id"
    fun createManageProductsRoute(restaurantId: String) = "manage_products_screen/$restaurantId" // Nuevo
    fun createProductFormRoute(restaurantId: String, productId: String? = null): String { // Nuevo
        return if (productId != null) {
            "product_form_screen/$restaurantId?productId=$productId"
        } else {
            "product_form_screen/$restaurantId"
        }
    }
    fun createEditRestaurantRoute(restaurantId: String): String {
        return "edit_restaurant_screen/$restaurantId"
    }
}

@Composable
fun NavGraph(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val alertsViewModel: AlertsViewModel = hiltViewModel() // No es ideal obtenerlo aquí si solo se usa en una pantalla

    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.HOME_SCREEN
    ) {
        composable(NavigationRoutes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                onRestaurantClick = { restaurantId ->
                    navController.navigate(NavigationRoutes.createRestaurantDetailRoute(restaurantId))
                },
                onIngredientClick = { ingredient ->
                    navController.navigate(NavigationRoutes.createSearchingRoute(ingredient.name))
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

        composable(
            route = NavigationRoutes.RESTAURANT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val restaurantId = it.arguments?.getString("id") ?: ""
            RestaurantDetailScreen(
                restaurantId = restaurantId,
                authViewModel = authViewModel,
                onProductClick = { productId -> navController.navigate(NavigationRoutes.createFoodDetailRoute(productId)) }
            )
        }

        composable(NavigationRoutes.PROFILE_SCREEN) {
            ProfileScreen(
                navController = navController,
                authViewModel = authViewModel,
                onProductClick = { productId -> navController.navigate(NavigationRoutes.createFoodDetailRoute(productId)) }
            )
        }

        // Vendor Screen ahora lleva a ManageRestaurant, que tendrá el botón para productos
        composable(NavigationRoutes.VENDOR_SCREEN) {
            VendorScreen( // Esta pantalla será rediseñada
                navController = navController,
                authViewModel = authViewModel // Necesitará el authViewModel para obtener el ID del restaurante del vendor
            )
        }

        composable(NavigationRoutes.ALERTS_SCREEN) {
            AlertsScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel() // Mejor obtenerlo aquí
            )
        }

        composable("alert_create") {
            AlertCreateScreen(
                onBackClick = { navController.popBackStack() },
                onAlertCreated = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

        composable("draft_alerts") {
            DraftAlertsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

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
                viewModel = hiltViewModel()
            )
        }

        composable(
            route = NavigationRoutes.FOOD_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val foodId = it.arguments?.getString("id") ?: ""
            FoodDetailScreen(
                foodId = foodId,
                onBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                viewModel = hiltViewModel()
            )
        }

        composable(NavigationRoutes.RESERVATIONS_SCREEN) {
            ReservationsScreen(
                navController = navController,
                reservationsViewModel = hiltViewModel()
            )
        }

        composable(NavigationRoutes.VENDOR_RESERVATIONS) {
            ReservationsVendorScreen(viewModel = hiltViewModel())
        }

        composable(
            route = NavigationRoutes.EDIT_RESTAURANT_SCREEN,
            arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId")
            if (restaurantId != null) {
                EditRestaurantScreen(navController = navController, restaurantId = restaurantId)
            } else {
                Text("Error: Restaurant ID missing.")
            }
        }

        // --- Nuevas Rutas para Gestión de Productos ---
        composable(
            route = NavigationRoutes.MANAGE_PRODUCTS_SCREEN,
            arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
            ManageProductsScreen(
                restaurantId = restaurantId,
                navController = navController,
                viewModel = hiltViewModel()
            )
        }

        composable(
            route = NavigationRoutes.PRODUCT_FORM_SCREEN,
            arguments = listOf(
                navArgument("restaurantId") { type = NavType.StringType },
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
            val productId = backStackEntry.arguments?.getString("productId") // Puede ser null
            ProductFormScreen(
                restaurantId = restaurantId,
                productId = productId,
                navController = navController,
                viewModel = hiltViewModel()
            )
        }
        // --- Fin Nuevas Rutas ---
    }
}