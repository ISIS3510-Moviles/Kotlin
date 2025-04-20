package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.ReservationDTO

interface ReservationRepository {
    suspend fun getReservationById(id: String): ReservationDTO
}