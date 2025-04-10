package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.repository.RestaurantRepository
import jakarta.inject.Inject

class ReservationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): ReservationRepository {
    override suspend fun getReservationById(id: String): ReservationDTO {
        return apiService.getReservationById(id)
    }
}