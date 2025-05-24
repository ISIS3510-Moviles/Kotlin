package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateProductDTO(
    val name: String,
    val description: String,
    val price: Float,
    val photo: String, // URL de la imagen
    val restaurant_id: String,
    val rating: Float = 0.0f, // Rating inicial, puede ser 0 o no incluirse si el backend lo maneja
    val ingredientsIds: List<String> = emptyList(),
    val discountsIds: List<String> = emptyList(),
    val commentsIds: List<String> = emptyList(),
    val foodTagsIds: List<String> = emptyList(),
    val dietaryTagsIds: List<String> = emptyList()
)