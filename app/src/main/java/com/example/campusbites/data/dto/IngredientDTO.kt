package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class IngredientDTO (
    val id: String,
    val name: String,
    val description: String,
    val productsIds: List<String> = emptyList(),
    val image: String
)