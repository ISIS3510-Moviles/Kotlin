package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.FoodTagDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.repository.FoodTagRepository
import jakarta.inject.Inject

class FoodTagRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): FoodTagRepository {

    override suspend fun getFoodTags(): List<FoodTagDTO> {
        return apiService.getFoodTags()
    }

    override suspend fun getFoodTagById(id: String): FoodTagDTO {
        return apiService.getFoodTagById(id)
    }

}