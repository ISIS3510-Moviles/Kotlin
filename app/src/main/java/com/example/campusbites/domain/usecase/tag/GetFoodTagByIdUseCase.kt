package com.example.campusbites.domain.usecase.tag

import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.repository.FoodTagRepository
import javax.inject.Inject

class GetFoodTagByIdUseCase @Inject constructor(
    private val repository: FoodTagRepository
) {
    suspend operator fun invoke(id: String): FoodTagDomain? { // Devuelve nullable
        return repository.getFoodTagById(id)
    }
}