package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.DietaryTagDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.repository.DietaryTagRepository
import jakarta.inject.Inject

class DietaryTagRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): DietaryTagRepository {

    override suspend fun getDietaryTags(): List<DietaryTagDTO> {
        return apiService.getDietaryTags()
    }

    override suspend fun getDietaryTagById(id: String): DietaryTagDTO {
        return apiService.getDietaryTagById(id)
    }
}