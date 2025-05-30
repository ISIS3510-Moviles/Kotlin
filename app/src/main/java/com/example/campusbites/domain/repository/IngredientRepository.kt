package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.IngredientDomain

interface IngredientRepository {
    suspend fun getIngredients(): List<IngredientDomain>
    suspend fun getIngredientById(id: String): IngredientDomain? // Nuevo
    suspend fun incrementIngredientClicks(ingredientId: String)
}