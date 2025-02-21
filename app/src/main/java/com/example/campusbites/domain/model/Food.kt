package com.example.campusbites.domain.model

data class Food (
    val name: String,
    val description: String,
    val price: Int,
    val restaurantId: String,
    val tagNames: List<String>
)