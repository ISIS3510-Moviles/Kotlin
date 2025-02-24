package com.example.campusbites.domain.model

import androidx.annotation.DrawableRes

data class Food (
    val id: String,

    @DrawableRes val photo: Int,

    val name: String,
    val description: String,
    val restaurantId: String,

    val meanTimeToGet: Int,
    val price: Int,
    val rating: Double,

    val tagNames: List<String>,
    val comments: List<Comment>,
)