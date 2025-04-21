package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationRestaurantDTO(
    val id: String,
    val name: String,
    val profilePhoto: String = "",
    val tags: List<String>,
    val rating: Double? = null,
    val comments: List<CommentDTORecommendation>,
    val reservations: List<ReservationDTORecommendation>,
    val subscribers: List<UserDTO>,
    val popularity: Double,
    val tag_text: String,
    val similarity: Double,
    val score: Double
)
@Serializable
data class ReservationDTORecommendation(
    val id: String,
    val date: String,
    val time: String,
    val numberComensals: Int,
    val isCompleted: Boolean,
    val restaurant_id: String,
    val user_id: String
)

@Serializable
data class CommentDTORecommendation(
    val id: String,
    val message: String,
    val rating: Int,
    val likes: Int? = 0,
    val isVisible: Boolean,
    val photos: List<String>? = emptyList(),
    val productId: String? = null,
    val datetime: String? = null,
    val restaurantId: String? = null,
    val authorId: String,
    val authorName: String,
)


