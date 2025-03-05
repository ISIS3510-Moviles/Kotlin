package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class Restaurant(
    val id: Int,
    val name: String,
    val description: String,
    val rating: Int,
    val latitude: Double,
    val longitude: Double,
    val routeIndications: String,
    val openingTime: LocalDateTime,
    val closingTime: LocalDateTime,
    val openHolidays: Boolean,
    val openWeekends: Boolean,
    val estimatedWaitTime: Int,
    val isActive: Boolean,
    val overviewPhoto: Photo,
    val profilePhoto: Photo,
    val photos: List<Photo>  = emptyList(),
    val reservations: List<Reservation>  = emptyList(),
    val alerts: List<Alert>  = emptyList(),
    val subscribers: List<User>  = emptyList(),
    val visits: List<Visit>  = emptyList(),
    val comments: List<Comment>  = emptyList(),
    val products: List<Product>  = emptyList(),
    val tags: List<Tag>  = emptyList()
)
