package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RestaurantDTO(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val routeIndications: String,
    val openingTime: String,
    val closingTime: String,
    val opensHolidays: Boolean,
    val opensWeekends: Boolean,
    val isActive: Boolean,
    val rating: Double,
    val address: String,
    val phone: String,
    val email: String,
    val overviewPhoto: String,
    val profilePhoto: String,
    val photos: List<String>,
    val foodTagsIds: List<String>,
    val dietaryTagsIds: List<String>,
    val alertsIds: List<String>  = emptyList(),
    val reservationsIds: List<String>  = emptyList(),
    val suscribersIds: List<String>  = emptyList(),
    val visitsIds: List<String>  = emptyList(),
    val commentsIds: List<String>  = emptyList(),
    val productsIds: List<String>  = emptyList(),
)
