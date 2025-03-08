package com.example.campusbites.domain.model

data class User(
    val identification: String,
    val name: String,
    val role: UserRole,
    val phoneNumber: String,
    val isPremium: Boolean,
    val publishedAlerts: List<Alert>  = emptyList(),
    val reservations: List<Reservation>  = emptyList(),
    val badges: List<Badge>  = emptyList(),
    val schedules: List<FoodSchedule>  = emptyList(),
    val institution: Institution,
    val dietaryPreferences: List<DietaryTag>  = emptyList(),
    val comments: List<Comment>  = emptyList(),
    val visits: List<Visit>  = emptyList(),
    val suscribedRestaurants: List<Restaurant>  = emptyList()
)