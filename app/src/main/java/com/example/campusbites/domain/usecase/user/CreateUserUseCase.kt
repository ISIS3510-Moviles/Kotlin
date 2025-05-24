package com.example.campusbites.domain.usecase.user

import android.util.Log
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: UserDomain) {
        Log.d("CreateUserUseCase", "Creando usuario: $user")
         repository.createUser(user.toDTO())

    }
}

fun UserDomain.toDTO() =
    UserDTO(
        id = this.id,
        name = this.name,
        phone = this.phone,
        email = this.email,
        role = this.role,
        isPremium = this.isPremium,
        badgesIds = this.badgesIds,
        schedulesIds = this.schedulesIds,
        reservationsIds = this.reservationsDomain.map { it.id },
        institutionId = "dnjV9tmwbciWZQipfI9D",
        dietaryPreferencesTagIds = this.dietaryPreferencesTagIds,
        commentsIds = this.commentsIds,
        visitsIds = this.visitsIds,
        suscribedRestaurantIds = this.suscribedRestaurantIds,
        publishedAlertsIds = this.publishedAlertsIds,
        savedProductsIds = this.savedProducts.map { it.id },
        vendorRestaurantId = this.vendorRestaurantId.toString()
    )
