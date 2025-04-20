package com.example.campusbites.domain.usecase.reservation

import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import javax.inject.Inject

class CreateReservationUseCase @Inject constructor(
    private val repository: ReservationRepository,
) {
    suspend operator fun invoke(reservationDomain: ReservationDomain): Boolean {
        return repository.createReservation(
            ReservationDTO(
                id = reservationDomain.id,
                date = reservationDomain.datetime,
                time = reservationDomain.time,
                numberComensals = reservationDomain.numberCommensals,
                isCompleted = reservationDomain.isCompleted,
                restaurant_id = reservationDomain.restaurantId,
                user_id = reservationDomain.userId
            )
        )
    }
}