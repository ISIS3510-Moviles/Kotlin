// package com.example.campusbites.data.mapper
package com.example.campusbites.data.mapper

import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.domain.model.ReservationDomain
import javax.inject.Inject

class ReservationMapper @Inject constructor() {
    fun mapDtoToDomain(dto: ReservationDTO): ReservationDomain {
        return ReservationDomain(
            id = dto.id,
            datetime = dto.date, // DTO usa 'date'
            time = dto.time,
            numberCommensals = dto.numberComensals,
            isCompleted = dto.isCompleted,
            restaurantId = dto.restaurant_id,
            userId = dto.user_id,
            hasBeenCancelled = dto.hasBeenCancelled
        )
    }

    fun mapDomainToDto(domain: ReservationDomain): CreateReservationDTO { // Asumiendo que necesitas un CreateReservationDTO
        return CreateReservationDTO(
            date = domain.datetime,
            time = domain.time,
            numberComensals = domain.numberCommensals,
            isCompleted = domain.isCompleted,
            restaurant_id = domain.restaurantId,
            user_id = domain.userId
        )
    }
}