package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodTagDomain(
    val id: String,
    val name: String,
    val description: String
)
