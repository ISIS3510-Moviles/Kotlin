package presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost // Import correcto
import androidx.navigation.compose.composable // Import necesario
import androidx.navigation.compose.rememberNavController // Si necesitas crear el controlador aquí
import androidx.navigation.NavHostController
import presentation.ui.screens.HomeScreen
import presentation.ui.screens.RestaurantDetailScreen

object NavigationRoutes {
    const val HOME_SCREEN = "home_screen"
    const val RESTAURANT_DETAIL = "restaurant_detail/{id}" // Sin espacios en el nombre
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
                onRestaurantClick = { restaurantId ->
                    navController.navigate("restaurant_detail/$restaurantId")
                }
            )
        }

        // Restaurant Detail
        composable(NavigationRoutes.RESTAURANT_DETAIL) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("id").orEmpty()
            RestaurantDetailScreen(restaurantId = restaurantId)
        }
    }
}