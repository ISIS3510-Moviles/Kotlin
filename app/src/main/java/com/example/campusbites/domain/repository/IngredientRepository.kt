package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.IngredientDTO

interface IngredientRepository {
    suspend fun getIngredients(): List<IngredientDTO>
    suspend fun incrementIngredientClicks(ingredientId: String)
}