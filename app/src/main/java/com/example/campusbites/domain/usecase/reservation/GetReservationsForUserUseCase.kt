package com.example.campusbites.domain.usecase.reservation

import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReservationsForUserUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    operator fun invoke(userId: String): Flow<List<ReservationDomain>> {
        return reservationRepository.getReservationsForUser(userId)
    }
}