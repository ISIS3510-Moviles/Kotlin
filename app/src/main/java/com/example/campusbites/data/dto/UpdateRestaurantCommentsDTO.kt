package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRestaurantCommentsDTO(
    val commentsIds: List<String>
)