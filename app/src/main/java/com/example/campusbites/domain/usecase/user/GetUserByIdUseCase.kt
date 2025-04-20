package com.example.campusbites.domain.usecase.user

import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import com.example.campusbites.domain.usecase.institution.GetInstitutionByIdUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.reservation.GetReservationByIdUseCase
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val repository: UserRepository,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getInstitutionByIdUseCase: GetInstitutionByIdUseCase,
    private val getReservationByIdUseCase: GetReservationByIdUseCase
) {
    suspend operator fun invoke(id: String): UserDomain {
        val userDTO = repository.getUserById(id)
        return UserDomain(
            id = userDTO.id,
            name = userDTO.name,
            phone = userDTO.phone,
            email = userDTO.email,
            role = userDTO.role,
            isPremium = userDTO.isPremium,
            badgesIds = userDTO.badgesIds,
            schedulesIds = userDTO.schedulesIds,
            reservationsDomain = userDTO.reservationsIds.map { reservationId -> getReservationByIdUseCase(reservationId) },
            institution = getInstitutionByIdUseCase(userDTO.institutionId),
            dietaryPreferencesTagIds = userDTO.dietaryPreferencesTagIds,
            commentsIds = userDTO.commentsIds,
            visitsIds = userDTO.visitsIds,
            suscribedRestaurantIds = userDTO.suscribedRestaurantIds,
            publishedAlertsIds = userDTO.publishedAlertsIds,
            savedProducts = userDTO.savedProductsIds.map { productId -> getProductByIdUseCase(productId) }
        )
    }
}