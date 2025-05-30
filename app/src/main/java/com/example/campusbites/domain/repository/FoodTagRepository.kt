package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.FoodTagDomain

interface FoodTagRepository {
    suspend fun getFoodTags(): List<FoodTagDomain>
    suspend fun getFoodTagById(id: String): FoodTagDomain? // Puede ser null si no se encuentra
}