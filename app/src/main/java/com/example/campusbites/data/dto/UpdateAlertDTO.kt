package com.example.campusbites.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateAlertDTO (
    @SerialName("date")
    val datetime: String? = null,
    val icon: String? = null,
    val message: String? = null,
    val votes: Int? = null,
    val publisherId: String? = null,
    val restaurantId: String? = null
)