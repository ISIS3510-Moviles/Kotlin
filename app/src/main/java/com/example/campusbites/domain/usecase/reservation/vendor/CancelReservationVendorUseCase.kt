package com.example.campusbites.domain.usecase.reservation.vendor

import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import javax.inject.Inject

class CancelReservationVendorUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(reservationId: String): Result<ReservationDomain> {
        return try {
            Result.success(reservationRepository.cancelReservation(reservationId)) // Asume que el mismo repo método sirve
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
