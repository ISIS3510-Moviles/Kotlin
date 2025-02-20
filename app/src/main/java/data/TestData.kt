package data

import androidx.annotation.DrawableRes
import com.example.campusbites.R
import domain.model.Comment
import domain.model.Restaurant

object TestData {
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
            tags = listOf("Lunch", "Pizza", "Fast Food", "BBC")
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

}
