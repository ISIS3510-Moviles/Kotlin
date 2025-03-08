package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.Restaurant

interface RestaurantRepository {
    suspend fun getRestaurants(): List<Restaurant>
    suspend fun getRestaurantById(id: String): Restaurant?
}

