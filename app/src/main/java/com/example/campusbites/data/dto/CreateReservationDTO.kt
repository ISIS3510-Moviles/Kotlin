package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateReservationDTO (
    val date: String,
    val time: String,
    val numberComensals: Int,
    val isCompleted: Boolean,
    val restaurant_id: String,
    val user_id: String,
)