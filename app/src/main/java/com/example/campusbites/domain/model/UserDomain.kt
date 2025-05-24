package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDomain(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    var role: String,
    val isPremium: Boolean,
    val badgesIds: List<String>,
    val schedulesIds: List<String>,
    val reservationsDomain: List<ReservationDomain>,
    val institution: InstitutionDomain?,
    val dietaryPreferencesTagIds: List<String>,
    val commentsIds: List<String>,
    val visitsIds: List<String>,
    val suscribedRestaurantIds: List<String>,
    val publishedAlertsIds: List<String>,
    val savedProducts: List<ProductDomain>,
    val vendorRestaurantId: String?
)