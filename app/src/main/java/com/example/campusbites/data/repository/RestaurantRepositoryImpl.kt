package com.example.campusbites.data.repository

import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.repository.RestaurantRepository
import jakarta.inject.Inject

class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): RestaurantRepository {

    override suspend fun getRestaurants(): List<Restaurant> {
        return apiService.getRestaurants()
    }
}