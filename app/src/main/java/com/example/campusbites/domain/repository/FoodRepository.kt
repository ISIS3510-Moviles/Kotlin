package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.Food

interface FoodRepository {
    suspend fun getFoods(): List<Food>
}