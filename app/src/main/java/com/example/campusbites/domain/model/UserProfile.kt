package com.example.campusbites.domain.model

import androidx.annotation.DrawableRes

data class UserProfile(
    val name: String,
    @DrawableRes val profileImage: Int,
    val role: String,
    // Preferencias
    val dietaryPreferences: List<String>,
    val favoriteFoodTypes: List<String>,
    val preferredPriceRange: String,
    // Historial de Actividad
    val recentlyVisitedRestaurants: List<String>,
    val comments: List<String>,
    val uploadedPhotos: List<String>,
    // Restaurantes Guardados y Alertas
    val favoriteRestaurants: List<String>,
    val notificationsEnabled: Boolean,
    // Configuración de Cuenta
    val contactInfo: String,
    val privacySettings: String,
    // Estadísticas Personales
    val visitsCount: Int,
    val averageRating: Double,
    val communityParticipationCount: Int
)