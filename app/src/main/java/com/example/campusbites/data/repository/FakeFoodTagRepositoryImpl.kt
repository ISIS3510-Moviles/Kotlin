package com.example.campusbites.data.repository

import com.example.campusbites.data.TestData
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.repository.FoodTagRepository

class FakeFoodTagRepositoryImpl: FoodTagRepository {
    override suspend fun getFoodTags(): List<FoodTag> {
        return TestData.foodTags
    }
}