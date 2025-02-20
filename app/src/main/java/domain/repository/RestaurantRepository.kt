package domain.repository

import domain.model.Restaurant

interface RestaurantRepository {
    suspend fun getRestaurants(): List<Restaurant>
    suspend fun getRestaurantById(id: String): Restaurant?
}

