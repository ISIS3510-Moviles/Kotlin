package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.data.mapper.RestaurantMapper // Importar RestaurantMapper
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import javax.inject.Inject

class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository,
    private val restaurantMapper: RestaurantMapper // Inyectar RestaurantMapper
) {
    suspend operator fun invoke(): List<RestaurantDomain> {
        val restaurantsDTO = repository.getRestaurants() // Devuelve List<RestaurantDTO>

        // Mapear cada RestaurantDTO a RestaurantDomain usando el mapper
        // El mapper se encarga de los tags nulables.
        return restaurantsDTO.map { dto ->
            restaurantMapper.mapDtoToDomain(dto)
        }
    }
}