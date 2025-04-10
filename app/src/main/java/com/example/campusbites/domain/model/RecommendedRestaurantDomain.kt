package com.example.campusbites.domain.model

data class RecommendationRestaurantDomain(
    val id: String,
    val name: String,
    val tags: List<String>,
    val rating: Double,
    val comments: List<RecommendationCommentDomain>,
    val reservations: List<RecommendationReservationDomain>,
    val subscribers: List<RecommendationUserDomain>,
    val popularity: Double,
    val tagText: String,
    val similarity: Double,
    val score: Double
)

data class RecommendationCommentDomain(
    val id: String,
    val message: String,
    val rating: Int,
    val likes: Int,
    val isVisible: Boolean,
    val photos: List<String>,
    val productId: String?,
    val datetime: String?,
    val restaurantId: String,
    val authorId: String
)

data class RecommendationReservationDomain(
    val id: String,
    val date: String,
    val time: String,
    val numberComensals: Int,
    val isCompleted: Boolean,
    val restaurantId: String,
    val userId: String
)

data class RecommendationUserDomain(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val isPremium: Boolean,
    val institutionId: String,
    val savedProductsIds: List<String>
)
