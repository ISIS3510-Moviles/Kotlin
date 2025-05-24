package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductDTO(
    val name: String? = null,
    val description: String? = null,
    val price: Float? = null,
    val photo: String? = null,
    // restaurant_id no debería cambiar, el rating lo maneja el backend generalmente
    val ingredientsIds: List<String>? = null,
    val discountsIds: List<String>? = null,
    val foodTagsIds: List<String>? = null,
    val dietaryTagsIds: List<String>? = null
)