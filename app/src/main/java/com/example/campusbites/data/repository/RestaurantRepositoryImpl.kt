package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.RestaurantRepository
import jakarta.inject.Inject

class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): RestaurantRepository {

    override suspend fun getRestaurants(): List<RestaurantDTO> {
        return apiService.getRestaurants()
    }

    override suspend fun getRestaurant(id: String): RestaurantDTO {
        return apiService.getRestaurant(id)
    }

    override suspend fun searchRestaurants(query: String): List<RestaurantDTO> {
        return try {
            val response = apiService.searchRestaurants(query)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("RestaurantRepository", "Error HTTP: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepository", "Excepción en searchRestaurants", e)
            emptyList()
        }
    }


}