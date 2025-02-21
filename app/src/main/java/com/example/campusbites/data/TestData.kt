package com.example.campusbites.data

import com.example.campusbites.R
import com.example.campusbites.domain.model.Comment
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.model.User

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

    val user = User(
        name = "Juan Pérez",
        university = "Universidad de Ejemplo"
    )

}
