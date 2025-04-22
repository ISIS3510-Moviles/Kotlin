package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.data.dto.ReservationDTO

interface ReservationRepository {
    suspend fun getReservationById(id: String): ReservationDTO

    suspend fun createReservation(createReservationDTO: CreateReservationDTO): ReservationDTO

}