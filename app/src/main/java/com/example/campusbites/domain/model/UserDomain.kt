package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

data class UserDomain(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val isPremium: Boolean,
    val badgesIds: List<String>,
    val schedulesIds: List<String>,
    val reservationsIds: List<String>,
    val institution: InstitutionDomain,
    val dietaryPreferencesTagIds: List<String>,
    val commentsIds: List<String>,
    val visitsIds: List<String>,
    val suscribedRestaurantIds: List<String>,
    val publishedAlertsIds: List<String>,
    val savedProducts: List<ProductDomain>,
)