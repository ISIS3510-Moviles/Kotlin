package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.repository.IngredientRepository
import javax.inject.Inject // Cambiado de jakarta.inject.Inject a javax.inject.Inject

class GetIngredientsUseCase @Inject constructor(
    private val ingredientRepository: IngredientRepository
) {
    suspend operator fun invoke(): List<IngredientDomain> {
        return ingredientRepository.getIngredients()
    }
}