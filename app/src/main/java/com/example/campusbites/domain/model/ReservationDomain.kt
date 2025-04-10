package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class ReservationDomain(
    val id: String,
    val datetime: String,
    val numberCommensals: Int,
    val isCompleted: Boolean,
    val userDomain: UserDomain? = null,
    val restaurantDomain: RestaurantDomain? = null
)
