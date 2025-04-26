package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.ReservationDomain
import kotlinx.coroutines.flow.Flow

interface LocalReservationRepository {
    suspend fun saveReservations(reservations: List<ReservationDomain>)
    suspend fun saveReservation(reservation: ReservationDomain)
    fun getReservationsForUser(userId: String): Flow<List<ReservationDomain>>
    suspend fun getReservationById(reservationId: String): ReservationDomain?
    suspend fun deleteReservationById(reservationId: String)
    suspend fun deleteAllReservationsForUser(userId: String)
}