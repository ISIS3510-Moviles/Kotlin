package com.example.campusbites.domain.usecase.reservation

import android.util.Log
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class CancelReservationUseCase @Inject constructor(
    private val repository: ReservationRepository,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getReservationByIdUseCase: GetReservationByIdUseCase,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(reservationId: String): ReservationDomain {
        val canceledReservation = repository.cancelReservation(reservationId)
        Log.d("CancelReservationUseCase", "Reservation canceled: $reservationId")

        val canceledReservationDomain = getReservationByIdUseCase(reservationId)

        val user = authRepository.currentUser.firstOrNull()
        user?.let { currentUser ->
            val updatedReservations = currentUser.reservationsDomain.map { reservation ->
                if (reservation.id == reservationId) {
                    canceledReservationDomain
                } else {
                    reservation
                }
            }

            val updatedUser = currentUser.copy(reservationsDomain = updatedReservations)
            updateUserUseCase(updatedUser.id, updatedUser)
        }

        return canceledReservation
    }
}