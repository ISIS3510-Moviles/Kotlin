package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.data.dto.UpdateRestaurantDTO
import com.example.campusbites.domain.repository.RestaurantRepository
import javax.inject.Inject

class UpdateRestaurantUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {

    suspend operator fun invoke(restaurantId: String, updateRestaurantDTO: UpdateRestaurantDTO): Boolean {
        return repository.updateRestaurant(restaurantId, updateRestaurantDTO)
    }
}