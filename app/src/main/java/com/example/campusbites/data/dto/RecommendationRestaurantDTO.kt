package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationRestaurantDTO(
    val id: String,
    val name: String,
    val profilePhoto: String = "",
    val tags: List<String>,
    val rating: Double? = null,
    val comments: List<CommentDTO>,
    val reservations: List<ReservationDTO>,
    val subscribers: List<UserDTO>,
    val popularity: Double,
    val tag_text: String,
    val similarity: Double,
    val score: Double
)


