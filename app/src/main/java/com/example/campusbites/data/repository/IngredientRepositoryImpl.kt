package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.IngredientDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.IngredientRepository
import jakarta.inject.Inject

class IngredientRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : IngredientRepository {

    override suspend fun getIngredients(): List<IngredientDTO> {
        return apiService.getIngredients()
    }

}