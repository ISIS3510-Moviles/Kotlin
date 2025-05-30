package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.data.mapper.RestaurantMapper // Importar RestaurantMapper
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import javax.inject.Inject

class SearchRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository,
    private val restaurantMapper: RestaurantMapper // Inyectar RestaurantMapper
) {
    suspend operator fun invoke(query: String): List<RestaurantDomain> {
        // repository.searchRestaurants(query) devuelve List<RestaurantDTO>
        val dtos = repository.searchRestaurants(query)

        // Mapear cada RestaurantDTO a RestaurantDomain usando el mapper
        // El mapper se encarga de los tags nulables.
        return dtos.map { dto ->
            restaurantMapper.mapDtoToDomain(dto)
        }
    }
}