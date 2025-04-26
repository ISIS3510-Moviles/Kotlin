package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FoodTagDTO(
    val id: String,
    val name: String,
    val description: String,
    val icon: String
)
