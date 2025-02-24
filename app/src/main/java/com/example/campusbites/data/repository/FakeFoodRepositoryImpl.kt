package com.example.campusbites.data.repository

import com.example.campusbites.data.TestData
import com.example.campusbites.domain.model.Food
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.repository.FoodRepository
import com.example.campusbites.domain.repository.FoodTagRepository

class FakeFoodRepositoryImpl: FoodRepository {
    override suspend fun getFoods(): List<Food> {
        return TestData.food
    }
}