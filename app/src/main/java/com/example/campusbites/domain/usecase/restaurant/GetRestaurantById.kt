package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.repository.RestaurantRepository

class GetRestaurantById(
    private val repository: RestaurantRepository
) {
    suspend operator fun invoke(id: String): Restaurant? {
        return repository.getRestaurantById(id)
    }
}