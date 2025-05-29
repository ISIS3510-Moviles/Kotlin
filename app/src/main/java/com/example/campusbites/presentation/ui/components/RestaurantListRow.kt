package com.example.campusbites.presentation.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.RestaurantDomain
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch

@Composable
fun RestaurantListRow(
    name: String,
    description: String,
    restaurants: List<RestaurantDomain>,
    onRestaurantClick: (String) -> Unit, // Mantenemos la firma original para onRestaurantClick
    modifier: Modifier = Modifier,
    // entrySourcePrefix: String // Ya no es necesario, onRestaurantClick lo manejará en HomeScreen
) {
    val scope = rememberCoroutineScope()
    val crashlytics = remember { FirebaseCrashlytics.getInstance() }

    crashlytics.log("Rendering restaurant list: $name with ${restaurants.size} items")

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .wrapContentSize()
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        )

        if (restaurants.isEmpty()) {
            Text(
                "There are no restaurants available at the moment",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .wrapContentSize()
            ) {
                items(
                    items = restaurants,
                    key = { restaurant -> restaurant.id }
                ) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onRestaurantClick = {
                            scope.launch {
                                try {
                                    crashlytics.log("User clicked on restaurant: ${restaurant.id} from list: $name")
                                    onRestaurantClick(restaurant.id) // El HomeScreen se encargará de pasar el entrySource
                                } catch (e: Exception) {
                                    Log.e("Error", "Error when clicking restaurant", e)
                                    crashlytics.apply {
                                        log("Error when clicking restaurant")
                                        setCustomKey("restaurant_id", restaurant.id)
                                        setCustomKey("restaurant_name", restaurant.name)
                                        setCustomKey("error_location", "restaurant_click_handler")
                                        recordException(e)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// Componente para manejar errores generales de UI
@Composable
fun ErrorFallbackComponent(errorMessage: String) {
    val crashlytics = remember { FirebaseCrashlytics.getInstance() }
    crashlytics.log("Showing error fallback UI: $errorMessage")

    Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}