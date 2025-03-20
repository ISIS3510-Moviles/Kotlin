package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.RestaurantDTO

interface RestaurantRepository {
    suspend fun getRestaurants(): List<RestaurantDTO>

    suspend fun getRestaurant(id: String): RestaurantDTO
}
