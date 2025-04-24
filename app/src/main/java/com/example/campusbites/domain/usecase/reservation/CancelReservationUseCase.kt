package com.example.campusbites.domain.usecase.reservation

import android.util.Log
import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import javax.inject.Inject

class CancelReservationUseCase @Inject constructor(
    private val repository: ReservationRepository,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getReservationByIdUseCase: GetReservationByIdUseCase,
) {
    suspend operator fun invoke(reservationId: String, authViewModel: AuthViewModel): ReservationDTO {
        val canceledReservation = repository.cancelReservation(reservationId)
        Log.d("CancelReservationUseCase", "Reservation canceled: $reservationId")

        val canceledReservationDomain = getReservationByIdUseCase(reservationId)

        val user = authViewModel.user.value
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
            authViewModel.updateUser(updatedUser)
        }

        return canceledReservation
    }
}