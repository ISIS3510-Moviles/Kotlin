package com.example.campusbites.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.campusbites.R
import com.example.campusbites.data.TestData.sampleUser
import com.example.campusbites.domain.model.Alert
import com.example.campusbites.domain.model.Comment
import com.example.campusbites.domain.model.Food
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.model.User
import com.example.campusbites.domain.model.UserProfile

object TestData {
    val foodTags = listOf(
        FoodTag(
            name = "Pizza",
            icon = R.drawable.pizza_icon,
        ),
        FoodTag(
            name = "Burger",
            icon = R.drawable.burguer_icon,
            ),
        FoodTag(
            name = "Asian",
            icon = R.drawable.asian_icon,
        ),
        FoodTag(
            name = "Lunch",
            icon = R.drawable.lunch_icon,
        ),
        FoodTag(
            name = "Fast Food",
            icon = R.drawable.fast_food_icon,
        ),
        FoodTag(
            name = "Italian",
            icon = R.drawable.italian_icon,
        ),
        FoodTag(
            name = "Mexican",
            icon = R.drawable.mexican_icon,
        ),
        FoodTag(
            name = "BBC",
            icon = R.drawable.bbc_icon,
        ),
        FoodTag(
            name = "Coffe",
            icon = R.drawable.coffe_icon,
        )
    )

    val comments = listOf(
        Comment(
            id = "1",
            text = "Excelente servicio y comida",
            rating = 4.5
        ),
        Comment(
            id = "2",
            text = "Ambiente cómodo y agradable",
            rating = 4.2
        ),
        Comment(
            id = "3",
            text = "Comida deliciosa y ambiente agradable",
            rating = 4.8
        )
    )

    val food = listOf(
        Food(
            id = "1",
            name = "Pepperoni Pizza",
            description = "Pepperoni and cheese",
            photo = R.drawable.pizza_food,
            restaurantId = "1",
            meanTimeToGet = 15,
            price = 10000,
            rating = 4.5,
            tagNames = listOf("Pizza", "Lunch", "BBC"),
            comments = comments
        ),
        Food(
            id = "2",
            name = "Cheese Burger",
            description = "Cheese and beef",
            photo = R.drawable.hambur_food,
            restaurantId = "2",
            meanTimeToGet = 10,
            price = 8000,
            rating = 4.2,
            tagNames = listOf("Lunch", "Burger", "Fast Food", "BBC"),
            comments = comments
        ),
        Food(
            id = "3",
            name = "Artisanal beer",
            description = "The best of the best",
            photo = R.drawable.cerveza_food,
            restaurantId = "1",
            meanTimeToGet = 20,
            price = 4750,
            rating = 4.8,
            tagNames = listOf("BBC"),
            comments = comments
        ),
        Food(
            id = "4",
            name = "Canola Rolls",
            description = "Delicious rolls with cream",
            photo = R.drawable.canela_rolls_food,
            restaurantId = "2",
            meanTimeToGet = 15,
            price = 12000,
            rating = 4.3,
            tagNames = listOf("Asian", "Lunch", "BBC"),
            comments = comments
        )
    )

    val restaurants = listOf(
        Restaurant(
            id = "1",
            name = "La Pizzería Universitaria",
            description = "Pizzas artesanales a precio estudiantil",
            profilePhoto = R.drawable.pizzaprofile,
            overviewPhoto = R.drawable.pizzaoverview,
            rating = 4.5,
            distance = 0.8,
            comments = comments,
            tags = listOf("Pizza", "Lunch", "BBC")
        ),
        Restaurant(
            id = "2",
            name = "Burger Campus",
            description = "Hamburguesas gourmet con ingredientes premium",
            profilePhoto = R.drawable.hamburprofile,
            overviewPhoto = R.drawable.hamburoverview,
            rating = 4.2,
            distance = 1.2,
            comments = comments,
            tags = listOf("Lunch", "Burger", "Fast Food", "BBC")
        )
    )

    val Alerts = listOf(
        Alert(
            id = "0",
            imageRes = R.drawable.restaurant_logo,
            title = "Doña Blanca",
            message = "Thank you for reserving table 1 on Thursday at 11:00 am."
        ),
        Alert(
            id = "1",
            imageRes = R.drawable.restaurant_logo,
            title = "Doña Blanca",
            message = "We have changed our opening time. Now Doña Blanca opens at 5:00 am."
        )
    )

    val sampleUser = UserProfile(
        name = "María García",
        profileImage = R.drawable.profile_picture,
        role = "Estudiante",
        dietaryPreferences = listOf("Vegana", "Sin gluten"),
        favoriteFoodTypes = listOf("Desayuno", "Snacks"),
        preferredPriceRange = "$20000 - $30000",
        recentlyVisitedRestaurants = listOf("Cafetería Central", "La Esquina", "Rincón Saludable"),
        comments = listOf(
            "Excelente servicio y ambiente acogedor.",
            "Buen lugar para tomar un snack rápido."
        ),
        uploadedPhotos = listOf(
            "https://via.placeholder.com/100",
            "https://via.placeholder.com/100"
        ),
        favoriteRestaurants = listOf("Cafetería Central", "Rincón Saludable"),
        notificationsEnabled = true,
        contactInfo = "maria.garcia@universidad.edu",
        privacySettings = "Público",
        visitsCount = 25,
        averageRating = 4.5,
        communityParticipationCount = 10
    )

    val user = User(
        name = "Juan Pérez",
        university = "Universidad de Ejemplo"
    )

}
