package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.FoodTag

interface FoodTagRepository {
    suspend fun getFoodTags(): List<FoodTag>
}