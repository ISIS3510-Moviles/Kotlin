package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.RecommendationRestaurantDTO

interface RecommendationRepository {
    suspend fun getRecommendations(userId: String, limit: Int): List<RecommendationRestaurantDTO>
}
