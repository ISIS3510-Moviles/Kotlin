// package com.example.campusbites.data.mapper
package com.example.campusbites.data.mapper

import android.util.Log
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.domain.model.InstitutionDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.institution.GetInstitutionByIdUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.reservation.GetReservationByIdUseCase
import javax.inject.Inject

class UserMapper @Inject constructor(
    private val getReservationByIdUseCase: GetReservationByIdUseCase,
    private val getInstitutionByIdUseCase: GetInstitutionByIdUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val institutionMapper: InstitutionMapper
) {
    suspend fun mapDtoToDomain(dto: UserDTO): UserDomain {
        val institutionDomain: InstitutionDomain? = if (dto.institutionId.isNotBlank()) {
            try {
                getInstitutionByIdUseCase(dto.institutionId)
            } catch (e: Exception) {
                Log.e("UserMapper", "Error fetching institution domain for ID: '${dto.institutionId}'. User: ${dto.id}. Exception: ${e.javaClass.simpleName}, Message: ${e.message}", e)
                institutionMapper.createDefault(dto.institutionId)
            }
        } else {
            null
        }

        return UserDomain(
            id = dto.id,
            name = dto.name,
            phone = dto.phone,
            email = dto.email,
            role = dto.role,
            isPremium = dto.isPremium,
            badgesIds = dto.badgesIds,
            schedulesIds = dto.schedulesIds,
            reservationsDomain = dto.reservationsIds.mapNotNull { reservationId ->
                try {
                    getReservationByIdUseCase(reservationId)
                } catch (e: Exception) {
                    Log.e("UserMapper", "Error fetching reservation $reservationId for user ${dto.id}. Exception: ${e.javaClass.simpleName}, Message: ${e.message}", e)
                    null
                }
            },
            institution = institutionDomain,
            dietaryPreferencesTagIds = dto.dietaryPreferencesTagIds,
            commentsIds = dto.commentsIds,
            visitsIds = dto.visitsIds,
            suscribedRestaurantIds = dto.suscribedRestaurantIds,
            publishedAlertsIds = dto.publishedAlertsIds,
            savedProducts = dto.savedProductsIds.mapNotNull { productId ->
                try {
                    getProductByIdUseCase(productId)
                } catch (e: Exception) {
                    Log.e("UserMapper", "Error fetching saved product $productId for user ${dto.id}", e)
                    null
                }
            }
        )
    }

    fun mapDtoToDomainFallback(dto: UserDTO, institutionName: String = "Institución Desconocida"): UserDomain {
        // Fallback simple sin llamadas a use cases, para cuando no se puede obtener toda la info
        return UserDomain(
            id = dto.id,
            name = dto.name,
            phone = dto.phone,
            email = dto.email,
            role = dto.role,
            isPremium = dto.isPremium,
            badgesIds = dto.badgesIds,
            schedulesIds = dto.schedulesIds,
            reservationsDomain = emptyList(),
            institution = institutionMapper.createDefault(dto.institutionId, institutionName),
            dietaryPreferencesTagIds = dto.dietaryPreferencesTagIds,
            commentsIds = dto.commentsIds,
            visitsIds = dto.visitsIds,
            suscribedRestaurantIds = dto.suscribedRestaurantIds,
            publishedAlertsIds = dto.publishedAlertsIds,
            savedProducts = emptyList()
        )
    }

    fun createFallbackUser(publisherId: String): UserDomain {
        return UserDomain(
            id = publisherId,
            name = "Usuario Desconocido",
            phone = "",
            email = "desconocido@example.com",
            role = "user",
            isPremium = false,
            badgesIds = emptyList(),
            schedulesIds = emptyList(),
            reservationsDomain = emptyList(),
            institution = institutionMapper.createDefault("unknown_institution_for_$publisherId"),
            dietaryPreferencesTagIds = emptyList(),
            commentsIds = emptyList(),
            visitsIds = emptyList(),
            suscribedRestaurantIds = emptyList(),
            publishedAlertsIds = emptyList(),
            savedProducts = emptyList()
        )
    }
}