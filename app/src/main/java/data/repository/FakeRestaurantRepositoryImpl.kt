package data.repository

import data.TestData
import domain.model.Restaurant
import domain.repository.RestaurantRepository

class FakeRestaurantRepositoryImpl : RestaurantRepository {
    override suspend fun getRestaurants(): List<Restaurant> {
        return TestData.restaurants
    }

    override suspend fun getRestaurantById(id: String): Restaurant? {
        return TestData.restaurants.find { it.id == id }
    }
}

