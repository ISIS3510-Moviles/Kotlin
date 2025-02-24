package com.example.campusbites.domain.usecase.food

import com.example.campusbites.domain.model.Food
import com.example.campusbites.domain.repository.FoodRepository
import jakarta.inject.Inject

class GetFoods @Inject constructor(
    private val repository: FoodRepository
){
    suspend operator fun invoke(): List<Food> {
        return repository.getFoods()
    }
}