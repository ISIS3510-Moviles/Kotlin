package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRestaurantDTO(
    val name: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val routeIndications: String? = null,
    val openingTime: String? = null,
    val closingTime: String? = null,
    val opensHolidays: Boolean? = null,
    val opensWeekends: Boolean? = null,
    val isActive: Boolean? = null,
    val rating: Double? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val overviewPhoto: String? = null,
    val profilePhoto: String? = null,
    val photos: List<String>? = null,
    val foodTagsIds: List<String>? = null,
    val dietaryTagsIds: List<String>? = null,
    val alertsIds: List<String>? = null,
    val reservationsIds: List<String>? = null,
    val suscribersIds: List<String>? = null,
    val visitsIds: List<String>? = null,
    val commentsIds: List<String>? = null,
    val productsIds: List<String>? = null,
)
