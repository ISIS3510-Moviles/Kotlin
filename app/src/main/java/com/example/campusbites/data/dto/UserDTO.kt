package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: String = "mhb5GrYjKYb52x7Cub5yT7LlPIo1",
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val isPremium: Boolean,
    val badgesIds: List<String> = emptyList(),
    val schedulesIds: List<String> = emptyList(),
    val reservationsIds: List<String> = emptyList(),
    val institutionId: String,
    val dietaryPreferencesTagIds: List<String> = emptyList(),
    val commentsIds: List<String> = emptyList(),
    val visitsIds: List<String> = emptyList(),
    val suscribedRestaurantIds: List<String> = emptyList(),
    val publishedAlertsIds: List<String> = emptyList(),
    val savedProductsIds: List<String> = emptyList(),
)