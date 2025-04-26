package com.example.campusbites.domain.usecase.user

import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import com.example.campusbites.data.dto.UserDTO
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(id: String, user: UserDomain): Boolean {
        val userDTO = UserDTO(
            id = user.id,
            name = user.name,
            phone = user.phone,
            email = user.email,
            role = user.role,
            isPremium = user.isPremium,
            badgesIds = user.badgesIds,
            schedulesIds = user.schedulesIds,
            reservationsIds = user.reservationsDomain.map { it.id },
            institutionId = "dnjV9tmwbciWZQipfI9D",
            dietaryPreferencesTagIds = user.dietaryPreferencesTagIds,
            commentsIds = user.commentsIds,
            visitsIds = user.visitsIds,
            suscribedRestaurantIds = user.suscribedRestaurantIds,
            publishedAlertsIds = user.publishedAlertsIds,
            savedProductsIds = user.savedProducts.map { it.id },
        )

        userDTO.institutionId = user.institution?.id ?: "dnjV9tmwbciWZQipfI9D"

        return userRepository.updateUser(id, userDTO)
    }
}
