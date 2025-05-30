package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.data.mapper.RestaurantMapper // Importar RestaurantMapper
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import javax.inject.Inject

class GetRestaurantByIdUseCase @Inject constructor(
    private val repository: RestaurantRepository,
    private val restaurantMapper: RestaurantMapper // Inyectar RestaurantMapper
) {
    suspend operator fun invoke(id: String): RestaurantDomain? {
        // RestaurantRepository.getRestaurant(id) devuelve RestaurantDTO
        val restaurantDTO = repository.getRestaurant(id) ?: return null

        // Usar el RestaurantMapper para convertir DTO a Domain
        // RestaurantMapper ya maneja correctamente el mapeo de tags con mapNotNull
        return restaurantMapper.mapDtoToDomain(restaurantDTO)
    }
}