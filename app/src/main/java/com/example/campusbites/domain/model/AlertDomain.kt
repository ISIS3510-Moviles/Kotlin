package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

data class AlertDomain(
    val id: String,
    val datetime: LocalDateTime,
    val icon: String,
    val message: String,
    val votes: Int,
    val publisher: UserDomain,
    val restaurantDomain: RestaurantDomain
)
