package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import jakarta.inject.Inject

class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): RestaurantRepository {

    override suspend fun getRestaurants(): List<RestaurantDTO> {
        return apiService.getRestaurants()
    }

    override suspend fun getRestaurantById(id: String): RestaurantDTO? {
        return getRestaurants().find { it.id == id }
    }
}