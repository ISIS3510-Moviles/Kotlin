package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.repository.IngredientRepository
import jakarta.inject.Inject

class GetIngredientsUseCase @Inject constructor(
    private val ingredientRepository: IngredientRepository
) {
    suspend operator fun invoke(): List<IngredientDomain> {
        val ingredientsDTO = ingredientRepository.getIngredients()
        return ingredientsDTO.map { ingredientDTO ->
            IngredientDomain(
                id = ingredientDTO.id,
                name = ingredientDTO.name,
                description = ingredientDTO.description,
                image = ingredientDTO.image,
                clicks = ingredientDTO.clicks
            )
        }
    }

}