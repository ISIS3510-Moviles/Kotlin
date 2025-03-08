package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.repository.RestaurantRepository
import javax.inject.Inject

class GetRestaurants @Inject constructor(
    private val repository: RestaurantRepository
) {
    suspend operator fun invoke(): List<Restaurant> {
        return repository.getRestaurants()
    }
}