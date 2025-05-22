package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateDTO(
    val id: String,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val role: String? = null,
    val isPremium: Boolean? = null,
    val badgesIds: List<String>? = null,
    val schedulesIds: List<String>? = null,
    val reservationsIds: List<String>? = null,
    var institutionId: String? = null,
    val dietaryPreferencesTagIds: List<String>? = null,
    val commentsIds: List<String>? = null,
    val visitsIds: List<String>? = null,
    val suscribedRestaurantIds: List<String>? = null,
    val publishedAlertsIds: List<String>? = null,
    val savedProductsIds: List<String>? = null,
)