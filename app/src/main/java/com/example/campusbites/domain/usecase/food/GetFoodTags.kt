package com.example.campusbites.domain.usecase.food

import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.repository.FoodTagRepository
import javax.inject.Inject

class GetFoodTags @Inject constructor(
    private val repository: FoodTagRepository
) {
    suspend operator fun invoke(): List<FoodTag> {
        return repository.getFoodTags()
    }
}