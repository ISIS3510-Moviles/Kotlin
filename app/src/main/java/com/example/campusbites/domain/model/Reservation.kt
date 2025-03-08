package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class Reservation(
    val id: String,
    val datetime: LocalDateTime,
    val numberCommensals: Int,
    val isCompleted: Boolean,
    val user: User,
    val restaurant: Restaurant
)
