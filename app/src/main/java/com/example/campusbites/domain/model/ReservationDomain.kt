package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class ReservationDomain(
    val id: String,
    val datetime: String,
    val time: String,
    val numberCommensals: Int,
    val isCompleted: Boolean,
    val restaurantId: String,
    val userId: String,
    val hasBeenCancelled: Boolean
)
