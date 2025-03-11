package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class Visit(
    val id: String,
    val dateTime: LocalDateTime,
    val vendor: RestaurantDomain,
    val visitor: UserDomain
)