package com.example.campusbites.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAlertDTO(
    @SerialName("date")
    val datetime: String?,
    val icon: String,
    val message: String,
    val votes: Int,
    val publisherId: String,
    val restaurantId: String
)
