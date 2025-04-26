package com.example.campusbites.domain.usecase.reservation

import android.util.Log
import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.domain.repository.AuthRepository // Importa AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull // Importa firstOrNull

class CreateReservationUseCase @Inject constructor(
    private val repository: ReservationRepository,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getReservationByIdUseCase: GetReservationByIdUseCase,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(reservationDomain: ReservationDomain): ReservationDomain {
        val reservation = repository.createReservation(reservationDomain)
        Log.d("CreateReservationUseCase", "Reservation created: ${reservation.id}")

        val createdReservationDomain = getReservationByIdUseCase(reservation.id)

        val user = authRepository.currentUser.firstOrNull()
        user?.let { currentUser ->
            val updatedReservations = currentUser.reservationsDomain + createdReservationDomain
            val updatedUser = currentUser.copy(reservationsDomain = updatedReservations)
            Log.d("CreateReservationUseCase", "User reservations updated: ${updatedUser.reservationsDomain}")
            updateUserUseCase(updatedUser.id, updatedUser)
        }

        return reservation
    }
}