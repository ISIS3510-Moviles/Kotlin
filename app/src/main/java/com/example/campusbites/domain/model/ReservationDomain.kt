package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class ReservationDomain(
    val id: String,
    val datetime: LocalDateTime,
    val numberCommensals: Int,
    val isCompleted: Boolean,
    val userDomain: UserDomain,
    val restaurantDomain: RestaurantDomain
)
