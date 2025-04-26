package com.example.campusbites.domain.usecase

import com.example.campusbites.domain.model.RecommendationCommentDomain
import com.example.campusbites.domain.model.RecommendationReservationDomain
import com.example.campusbites.domain.model.RecommendationRestaurantDomain
import com.example.campusbites.domain.model.RecommendationUserDomain
import com.example.campusbites.domain.repository.RecommendationRepository
import jakarta.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val repository: RecommendationRepository
) {
    suspend operator fun invoke(userId: String, limit: Int): List<RecommendationRestaurantDomain> {
        return repository.getRecommendations(userId, limit).map { dto ->
            RecommendationRestaurantDomain(
                id = dto.id,
                name = dto.name,
                tags = dto.tags,
                rating = dto.rating ?: 0.0,
                comments = dto.comments.map { comment ->
                    RecommendationCommentDomain(
                        id = comment.id,
                        message = comment.message,
                        rating = comment.rating,
                        likes = comment.likes ?: 0,
                        isVisible = comment.isVisible,
                        photos = comment.photos ?: emptyList(),
                        productId = comment.productId,
                        datetime = comment.datetime,
                        restaurantId = comment.restaurantId ?: "",
                        authorId = comment.authorId
                    )
                },
                reservations = dto.reservations.map { reservation ->
                    RecommendationReservationDomain(
                        id = reservation.id,
                        date = reservation.date,
                        time = reservation.time,
                        numberComensals = reservation.numberComensals,
                        isCompleted = reservation.isCompleted,
                        restaurantId = reservation.restaurant_id,
                        userId = reservation.user_id
                    )
                },
                subscribers = dto.subscribers.map { user ->
                    RecommendationUserDomain(
                        id = user.id,
                        name = user.name,
                        phone = user.phone,
                        email = user.email,
                        role = user.role,
                        isPremium = user.isPremium,
                        institutionId = user.institutionId,
                        savedProductsIds = user.savedProductsIds
                    )
                },
                popularity = dto.popularity,
                tagText = dto.tag_text,
                similarity = dto.similarity,
                score = dto.score
            )
        }
    }
}
