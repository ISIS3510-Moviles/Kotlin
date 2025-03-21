package com.example.campusbites.domain.usecase.user

import android.content.ContentValues.TAG
import android.util.Log
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import com.example.campusbites.domain.usecase.institution.GetInstitutionByIdUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import javax.inject.Inject

class GetUsersUseCase @Inject constructor(
    private val repository: UserRepository,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getInstitutionByIdUseCase: GetInstitutionByIdUseCase
) {
    suspend operator fun invoke(): List<UserDomain> {
        Log.d(TAG, "Fetching users from repository...")

        return try {
            val usersDTO = repository.getUsers()
            Log.d(TAG, "Fetched ${usersDTO.size} users from repository.")

            val usersDomain = usersDTO.map { userDTO ->
                Log.d(TAG, "Mapping userDTO: $userDTO")

                val institution = getInstitutionByIdUseCase(userDTO.institutionId)
                val savedProducts = userDTO.savedProductsIds.map { productId ->
                    val product = getProductByIdUseCase(productId)
                    Log.d(TAG, "Mapped productId: $productId to product: $product")
                    product
                }

                val userDomain = UserDomain(
                    id = userDTO.id,
                    name = userDTO.name,
                    phone = userDTO.phone,
                    email = userDTO.email,
                    role = userDTO.role,
                    isPremium = userDTO.isPremium,
                    badgesIds = userDTO.badgesIds,
                    schedulesIds = userDTO.schedulesIds,
                    reservationsIds = userDTO.reservationsIds,
                    institution = institution,
                    dietaryPreferencesTagIds = userDTO.dietaryPreferencesTagIds,
                    commentsIds = userDTO.commentsIds,
                    visitsIds = userDTO.visitsIds,
                    suscribedRestaurantIds = userDTO.suscribedRestaurantIds,
                    publishedAlertsIds = userDTO.publishedAlertsIds,
                    savedProducts = savedProducts
                )

                Log.d(TAG, "Mapped userDomain: $userDomain")
                userDomain
            }

            Log.d(TAG, "Successfully mapped all users to domain models.")
            usersDomain
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching or mapping users: ${e.localizedMessage}", e)
            emptyList()
        }
    }

}
