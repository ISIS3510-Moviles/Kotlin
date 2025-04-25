package com.example.campusbites.domain.usecase.reservation

import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import javax.inject.Inject

class GetReservationByIdUseCase @Inject constructor(
    private val repository: ReservationRepository,
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase
) {
    suspend operator fun invoke(reservationId: String): ReservationDomain {
        val reservation = repository.getReservationById(reservationId)
        return ReservationDomain(
            id = reservation!!.id,
            datetime = reservation.datetime,
            time = reservation.time,
            numberCommensals = reservation.numberCommensals,
            isCompleted = reservation.isCompleted,
            restaurantId = reservation.restaurantId,
            userId = reservation.userId,
            hasBeenCancelled = reservation.hasBeenCancelled
        )
    }
}