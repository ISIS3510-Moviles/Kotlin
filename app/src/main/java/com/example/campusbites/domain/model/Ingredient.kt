package com.example.campusbites.domain.model

data class Ingredient(
    val id: Int,
    val name: String,
    val description: String,
    val products: List<Product>  = emptyList()
)
