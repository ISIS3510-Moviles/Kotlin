package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.FoodTagDTO
import com.example.campusbites.domain.model.FoodTagDomain

interface FoodTagRepository {
    suspend fun getFoodTags(): List<FoodTagDTO>
    suspend fun getFoodTagById(id: String): FoodTagDTO
}