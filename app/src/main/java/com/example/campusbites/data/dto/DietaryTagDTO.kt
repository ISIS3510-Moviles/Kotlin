package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DietaryTagDTO(
    val id: String,
    val name: String,
    val description: String,
)
