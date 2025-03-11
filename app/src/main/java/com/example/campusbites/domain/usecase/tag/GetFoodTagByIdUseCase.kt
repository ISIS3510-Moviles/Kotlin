package com.example.campusbites.domain.usecase.tag

import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.repository.FoodTagRepository
import javax.inject.Inject

class GetFoodTagByIdUseCase @Inject constructor(
    private val repository: FoodTagRepository
) {
    suspend operator fun invoke(id: String): FoodTagDomain {
        val foodTagDTO = repository.getFoodTagById(id)
        return FoodTagDomain(
            id = foodTagDTO.id,
            name = foodTagDTO.name,
            description = foodTagDTO.description
        )
    }
}