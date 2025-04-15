package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class IngredientDomain(
    val id: String,
    val name: String,
    val description: String,
    val products: List<ProductDomain>  = emptyList(),
    val image: String
)
