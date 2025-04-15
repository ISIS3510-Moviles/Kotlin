package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductDomain(
    val id: String,
    val name: String,
    val description: String,
    val price: Float,
    val photo: String,
    val restaurantId: String,
    val rating: Float,
    val ingredientsIds: List<String>,
    val discountsIds: List<String>,
    val commentsIds: List<String>,
    val foodTags: List<FoodTagDomain>,
    val dietaryTags: List<DietaryTagDomain>
)