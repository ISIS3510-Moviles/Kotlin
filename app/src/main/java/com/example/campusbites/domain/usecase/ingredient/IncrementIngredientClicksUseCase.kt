package com.example.campusbites.domain.usecase.ingredient

import com.example.campusbites.domain.repository.IngredientRepository
import jakarta.inject.Inject

class IncrementIngredientClicksUseCase @Inject constructor (
    private val repository: IngredientRepository
)

{
    suspend operator fun invoke(ingredientId: String) {
        repository.incrementIngredientClicks(ingredientId)
    }
}
