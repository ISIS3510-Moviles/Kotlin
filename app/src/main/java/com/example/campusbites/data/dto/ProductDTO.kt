package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDTO(
    val id: String,
    val name: String,
    val description: String,
    val price: Float,
    val photo: String,
    val restaurant_id: String = "", // Valor por defecto: cadena vacía
    val rating: Float = 0.0f,
    val ingredientsIds: List<String> = emptyList(), // Valor por defecto: lista vacía
    val discountsIds: List<String> = emptyList(),
    val commentsIds: List<String> = emptyList(),
    val foodTagsIds: List<String> = emptyList(), // Valor por defecto: lista vacía
    val dietaryTagsIds: List<String> = emptyList() // Valor por defecto: lista vacía
)