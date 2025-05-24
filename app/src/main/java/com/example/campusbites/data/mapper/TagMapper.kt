// package com.example.campusbites.data.mapper
package com.example.campusbites.data.mapper

import com.example.campusbites.data.dto.DietaryTagDTO
import com.example.campusbites.data.dto.FoodTagDTO
import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.model.FoodTagDomain
import javax.inject.Inject

class TagMapper @Inject constructor() {
    fun mapFoodTagDtoToDomain(dto: FoodTagDTO): FoodTagDomain {
        return FoodTagDomain(
            id = dto.id,
            name = dto.name,
            description = dto.description
            // Icono no está en FoodTagDomain, si lo necesitas, añádelo a FoodTagDomain
        )
    }

    fun mapDietaryTagDtoToDomain(dto: DietaryTagDTO): DietaryTagDomain {
        return DietaryTagDomain(
            id = dto.id,
            name = dto.name,
            description = dto.description
        )
    }
}