package com.example.campusbites.domain.usecase.reservation

import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import javax.inject.Inject

class CreateReservationUseCase @Inject constructor(
    private val repository: ReservationRepository,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getReservationByIdUseCase: GetReservationByIdUseCase,
) {
    suspend operator fun invoke(reservationDomain: ReservationDomain, authViewModel: AuthViewModel): ReservationDTO {
        val reservation = repository.createReservation(
            CreateReservationDTO(
                date = reservationDomain.datetime,
                time = reservationDomain.time,
                numberComensals = reservationDomain.numberCommensals,
                isCompleted = reservationDomain.isCompleted,
                restaurant_id = reservationDomain.restaurantId,
                user_id = reservationDomain.userId
            )
        )
        val createdReservationDomain = getReservationByIdUseCase(reservation.id)
        val user = getUserByIdUseCase(reservationDomain.userId)
        val updatedUser = user.copy(reservationsDomain = user.reservationsDomain + createdReservationDomain)
        updateUserUseCase(updatedUser.id, updatedUser)
        authViewModel.updateUser(updatedUser)
        return reservation
    }
}