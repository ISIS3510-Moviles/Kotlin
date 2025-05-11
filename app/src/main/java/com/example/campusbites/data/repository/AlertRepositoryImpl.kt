package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.AlertDTO
import com.example.campusbites.data.dto.CreateAlertDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UpdateAlertDTO
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.local.LocalAlertDataSource
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.InstitutionDomain
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AlertRepository
import com.example.campusbites.domain.usecase.institution.GetInstitutionByIdUseCase
import com.example.campusbites.domain.usecase.reservation.GetReservationByIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val localAlertDataSource: LocalAlertDataSource,
    private val getReservationByIdUseCase: GetReservationByIdUseCase,
    private val getInstitutionByIdUseCase: GetInstitutionByIdUseCase
) : AlertRepository {




    private suspend fun mapDtoToDomain(
        dto: AlertDTO,

    ): AlertDomain {
        val parsedDateTime = dto.datetime?.let {
            try {
                Instant.parse(it).atZone(ZoneOffset.UTC).toLocalDateTime()
            } catch (e: Exception) {
                Log.e("AlertRepoImpl", "Failed to parse datetime: $it for alert ${dto.id}. Using current time.", e)
                LocalDateTime.now(ZoneOffset.UTC)
            }
        } ?: run {
            Log.w("AlertRepoImpl", "AlertDTO datetime is null for alert ${dto.id}. Using current time.")
            LocalDateTime.now(ZoneOffset.UTC)
        }

        val userDTO: UserDTO = try {
            Log.d("AlertRepoImpl_Debug", "Fetching UserDTO for publisherId: ${dto.publisherId} on alert ${dto.id}")
            // this.apiService.getUserById(dto.publisherId) // Si es método de clase
            apiService.getUserById(dto.publisherId) // Asumiendo que apiService es un miembro de la clase
        } catch (e: Exception) {
            Log.e("AlertRepoImpl", "Failed to fetch UserDTO for publisherId: ${dto.publisherId} on alert ${dto.id}. Using default User. Error: ${e.message}", e)
            UserDTO(
                id = dto.publisherId,
                name = "Usuario Desconocido",
                phone = "",
                email = "desconocido@example.com",
                role = "user",
                isPremium = false,
                institutionId = "unknown_institution_for_${dto.publisherId}",
                savedProductsIds = emptyList()
            )
        }

        Log.d("AlertRepoImpl_Debug", "User ID from DTO: ${userDTO.id}, Institution ID from UserDTO: '${userDTO.institutionId}' for alert ${dto.id}")

        val institutionDomain: InstitutionDomain? = if (userDTO.institutionId.isNotBlank() && userDTO.institutionId != "unknown_institution_for_${dto.publisherId}") {
            Log.d("AlertRepoImpl_Debug", "Attempting to fetch institution with ID: '${userDTO.institutionId}' for alert ${dto.id}")
            try {
                getInstitutionByIdUseCase(userDTO.institutionId)
            } catch (e: Exception) {
                Log.e("AlertRepoImpl", "Error fetching institution domain for ID: '${userDTO.institutionId}'. User: ${userDTO.id}, Alert: ${dto.id}. Exception: ${e.javaClass.simpleName}, Message: ${e.message}", e)
                null
            }
        } else {
            if (userDTO.institutionId == "unknown_institution_for_${dto.publisherId}") {
                Log.w("AlertRepoImpl_Debug", "User ${userDTO.id} (alert ${dto.id}) is using default institutionId. Skipping real institution fetch.")
            } else {
                Log.w("AlertRepoImpl_Debug", "User ${userDTO.id} (alert ${dto.id}) has blank institutionId. Skipping institution fetch.")
            }
            null
        }

        val publisher = UserDomain(
            id = userDTO.id,
            name = userDTO.name,
            phone = userDTO.phone,
            email = userDTO.email,
            role = userDTO.role,
            isPremium = userDTO.isPremium,
            badgesIds = userDTO.badgesIds,
            schedulesIds = userDTO.schedulesIds,
            reservationsDomain = userDTO.reservationsIds.mapNotNull { reservationId ->
                try {
                    getReservationByIdUseCase(reservationId)
                } catch (e: Exception) {
                    Log.e("AlertRepoImpl", "Error fetching reservation $reservationId for user ${userDTO.id}, alert ${dto.id}. Exception: ${e.javaClass.simpleName}, Message: ${e.message}", e)
                    null
                }
            },
            institution = institutionDomain ?: InstitutionDomain(
                id = userDTO.institutionId,
                name = if (institutionDomain != null) institutionDomain.name else "Institución desconocida",
                description = if (institutionDomain != null) institutionDomain.description else "",
                members = if (institutionDomain != null) institutionDomain.members else emptyList(),
                buildings = if (institutionDomain != null) institutionDomain.buildings else emptyList()
            ),
            dietaryPreferencesTagIds = userDTO.dietaryPreferencesTagIds,
            commentsIds = userDTO.commentsIds,
            visitsIds = userDTO.visitsIds,
            suscribedRestaurantIds = userDTO.suscribedRestaurantIds,
            publishedAlertsIds = userDTO.publishedAlertsIds,

            savedProducts = userDTO.savedProductsIds.mapNotNull { productId ->
                try {
                    null
                } catch (e: Exception) {
                    Log.e("AlertRepoImpl", "Error fetching saved product $productId for user ${userDTO.id}", e)
                    null
                }
            }
        )

        val restaurantDTO: RestaurantDTO? = try {
            Log.d("AlertRepoImpl_Debug", "Attempting to fetch restaurant with ID: '${dto.restaurantId}' for alert '${dto.id}'")
            if (dto.restaurantId.isNotBlank()) {
                apiService.getRestaurant(dto.restaurantId)
            } else {
                Log.w("AlertRepoImpl_Debug", "Restaurant ID is blank for alert '${dto.id}'. Skipping restaurant fetch.")
                null
            }
        } catch (e: Exception) {
            Log.e("AlertRepoImpl", "Error fetching restaurant ${dto.restaurantId} for alert '${dto.id}'. Exception: ${e.javaClass.simpleName}, Message: ${e.message}", e)
            null
        }

        val restaurant = restaurantDTO?.let {
            RestaurantDomain(
                id = it.id,
                name = it.name,
                description = it.description,
                latitude = it.latitude ?: 0.0, // RestaurantDTO podría tenerlos nullables
                longitude = it.longitude ?: 0.0,
                routeIndications = it.routeIndications,
                openingTime = it.openingTime,
                closingTime = it.closingTime,
                opensHolidays = it.opensHolidays ?: false,
                opensWeekends = it.opensWeekends ?: false,
                isActive = it.isActive ?: false,
                rating = it.rating ?: 0.0,
                address = it.address,
                phone = it.phone,
                email = it.email,
                overviewPhoto = it.overviewPhoto,
                profilePhoto = it.profilePhoto,
                photos = it.photos ?: emptyList(),
                foodTags = emptyList(), // Mapear si es necesario
                dietaryTags = emptyList(), // Mapear si es necesario
                alertsIds = it.alertsIds ?: emptyList(),
                reservationsIds = it.reservationsIds ?: emptyList(),
                suscribersIds = it.suscribersIds ?: emptyList(),
                visitsIds = it.visitsIds ?: emptyList(),
                commentsIds = it.commentsIds ?: emptyList(),
                productsIds = it.productsIds ?: emptyList()
            )
        } ?: RestaurantDomain(
            id = dto.restaurantId.ifBlank { "unknown_restaurant_${dto.id}" },
            name = "Restaurante desconocido",
            description = "", latitude = 0.0, longitude = 0.0, routeIndications = "",
            openingTime = "", closingTime = "", opensHolidays = false, opensWeekends = false,
            isActive = false, rating = 0.0, address = "", phone = "", email = "",
            overviewPhoto = "", profilePhoto = "", photos = emptyList(), foodTags = emptyList(),
            dietaryTags = emptyList(), alertsIds = emptyList(), reservationsIds = emptyList(),
            suscribersIds = emptyList(), visitsIds = emptyList(), commentsIds = emptyList(),
            productsIds = emptyList()
        )

        return AlertDomain(
            id = dto.id,
            datetime = parsedDateTime,
            icon = dto.icon,
            message = dto.message,
            votes = dto.votes,
            publisher = publisher,
            restaurantDomain = restaurant
        )
    }

    override suspend fun fetchAndSaveRemoteAlerts(): List<AlertDomain> {
        return try {
            val dtos: List<AlertDTO> = apiService.getAlerts()
            Log.d("AlertRepoImpl_Debug", "Fetched ${dtos.size} alert DTOs from API.")
            val domainAlerts = dtos.mapNotNull { dto ->
                try {
                    Log.d("AlertRepoImpl_Debug", "Mapping DTO for alert ID: ${dto.id}")
                    mapDtoToDomain(dto)
                } catch (e: Exception) {
                    Log.e("AlertRepoImpl", "Failed to map DTO for alert ID: ${dto.id}. Skipping. Error: ${e.message}", e)
                    null
                }
            }
            if (dtos.isNotEmpty() && domainAlerts.size < dtos.size) {
                Log.w("AlertRepoImpl_Debug", "${dtos.size - domainAlerts.size} alert DTOs failed to map to Domain objects and were skipped.")
            }
            localAlertDataSource.deleteAllAlerts()
            localAlertDataSource.saveAlerts(domainAlerts)
            Log.d("AlertRepositoryImpl", "Fetched and saved ${domainAlerts.size} alerts to Realm.")
            domainAlerts
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error fetching remote alerts: ${e.message}", e)
            throw e
        }
    }

    override fun getLocalAlertsFlow(): Flow<List<AlertDomain>> {
        return localAlertDataSource.getAlertsFlow()
    }

    override suspend fun getAlertById(id: String): AlertDomain? {
        return localAlertDataSource.getAlertsFlow().firstOrNull()?.find { it.id == id }
    }

    override suspend fun updateAlertVotes(id: String, votes: Int): Boolean {
        val updateDto = UpdateAlertDTO(votes = votes)
        return try {
            val success = apiService.updateAlertVotes(id, updateDto)
            if (success) {
                val alertToUpdate = getAlertById(id)
                alertToUpdate?.let {
                    val updatedAlert = it.copy(votes = votes)
                    localAlertDataSource.updateAlert(updatedAlert)
                } ?: Log.w("AlertRepositoryImpl", "Alert $id not found locally to update votes after API success.")
            }
            success
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error updating alert votes for $id: ${e.message}", e)
            false
        }
    }

    override suspend fun createAlert(
        datetime: String,
        icon: String,
        message: String,
        publisherId: String,
        restaurantId: String
    ): AlertDomain {
        val createAlertDTO = CreateAlertDTO(
            datetime = datetime,
            icon = icon,
            message = message,
            publisherId = publisherId,
            restaurantId = restaurantId,
            votes = 0
        )
        try {
            Log.d("AlertRepoImpl_Debug", "Creating alert with restaurantId: $restaurantId, publisherId: $publisherId")
            val createdAlertDTO = apiService.createAlert(createAlertDTO)
            Log.d("AlertRepoImpl_Debug", "Alert DTO created successfully, ID: ${createdAlertDTO.id}. Now mapping to domain.")
            val domainAlert = mapDtoToDomain(createdAlertDTO)
            Log.d("AlertRepoImpl_Debug", "Alert mapped to domain. Saving to local data source.")
            localAlertDataSource.updateAlert(domainAlert)
            Log.d("AlertRepoImpl_Debug", "Alert saved locally. ID: ${domainAlert.id}")
            return domainAlert
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error creating alert: ${e.message}", e)
            throw e
        }
    }
}