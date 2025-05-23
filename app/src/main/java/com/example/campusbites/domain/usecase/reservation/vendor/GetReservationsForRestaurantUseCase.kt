package com.example.campusbites.domain.usecase.reservation.vendor

import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReservationsForRestaurantUseCase @Inject constructor( // Renombrado para claridad si prefieres
    private val repository: ReservationRepository,
) {
    suspend operator fun invoke(restaurantId: String): Flow<List<ReservationDomain>> { // Parámetro es restaurantId
        return repository.getReservationsByRestaurantId(restaurantId)
    }
}