package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.domain.model.RestaurantDomain

interface RestaurantRepository {
    suspend fun getRestaurants(): List<RestaurantDTO>
    suspend fun getRestaurant(id: String): RestaurantDTO
}

