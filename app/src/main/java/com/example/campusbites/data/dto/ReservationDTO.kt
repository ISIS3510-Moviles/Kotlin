package com.example.campusbites.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReservationDTO (
    val id: String,
    val date: String,
    val time: String,
    val numberComensals: Int,
    val isCompleted: Boolean,
    val restaurant_id: String,
    val user_id: String,
)