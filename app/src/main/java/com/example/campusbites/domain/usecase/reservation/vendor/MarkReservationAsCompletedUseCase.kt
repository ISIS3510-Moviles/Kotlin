package com.example.campusbites.domain.usecase.reservation.vendor

import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import javax.inject.Inject

class MarkReservationAsCompletedUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(reservationId: String): Result<ReservationDomain> {
        return try {
            Result.success(reservationRepository.markReservationAsCompleted(reservationId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}