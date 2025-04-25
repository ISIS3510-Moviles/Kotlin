package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.domain.model.ReservationDomain
import kotlinx.coroutines.flow.Flow

interface ReservationRepository {
    fun getReservationsForUser(userId: String): Flow<List<ReservationDomain>>
    suspend fun getReservationById(id: String): ReservationDomain?
    suspend fun createReservation(reservation: CreateReservationDTO): ReservationDomain
    suspend fun cancelReservation(id: String): ReservationDomain
}