package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class Alert(
    val id: String,
    val datetime: LocalDateTime,
    val icon: Photo,
    val message: String,
    val votes: Int,
    val publisher: User,
    val restaurant: Restaurant
)
