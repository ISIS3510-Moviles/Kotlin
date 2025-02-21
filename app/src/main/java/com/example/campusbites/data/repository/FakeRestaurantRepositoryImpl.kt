package com.example.campusbites.data.repository

import com.example.campusbites.data.TestData
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.repository.RestaurantRepository

class FakeRestaurantRepositoryImpl : RestaurantRepository {
    override suspend fun getRestaurants(): List<Restaurant> {
        return TestData.restaurants
    }

    override suspend fun getRestaurantById(id: String): Restaurant? {
        return TestData.restaurants.find { it.id == id }
    }
}

