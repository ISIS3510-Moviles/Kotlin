package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.RecommendationDTO
import com.example.campusbites.data.dto.RecommendationRestaurantDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.RecommendationRepository
import javax.inject.Inject

class RecommendationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RecommendationRepository {

    override suspend fun getRecommendations(userId: String, limit: Int): List<RecommendationRestaurantDTO> {
        val response = apiService.getRecommendations(RecommendationDTO(userId, limit))
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Error getting recommendations: ${response.code()} ${response.message()}")
        }
    }

}
