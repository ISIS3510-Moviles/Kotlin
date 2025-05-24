package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UpdateRestaurantDTO

interface RestaurantRepository {
    suspend fun getRestaurants(): List<RestaurantDTO>

    suspend fun getRestaurant(id: String): RestaurantDTO

    suspend fun searchRestaurants(query: String): List<RestaurantDTO>

    // New method to update restaurant comments
    suspend fun updateRestaurantComments(restaurantId: String, commentsIds: List<String>): Boolean
    suspend fun updateRestaurant(restaurantId: String, updateRestaurantDTO: UpdateRestaurantDTO): Boolean

}