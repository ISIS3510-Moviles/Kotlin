package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDTO(
    val id: String,
    val name: String,
    val description: String,
    val price: Float,
    val photo: String,
    val restaurant_id: String,
    val rating: Float,
    val ingredientsIds: List<String>,
    val discountsIds: List<String> = emptyList(),
    val commentsIds: List<String> = emptyList(),
    val foodTagsIds: List<String>,
    val dietaryTagsIds: List<String>
)