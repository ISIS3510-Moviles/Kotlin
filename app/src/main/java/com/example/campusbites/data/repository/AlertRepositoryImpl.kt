package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.AlertDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UpdateAlertDTO
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.InstitutionDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AlertRepository
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AlertRepository {

    private suspend fun mapDtoToDomain(dto: AlertDTO): AlertDomain {
        // Parsea la fecha usando ISO_OFFSET_DATE_TIME (por ejemplo, "2025-03-10T12:00:00Z")
        val parsedDateTime = OffsetDateTime
            .parse(dto.datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toLocalDateTime()

        val userDTO: UserDTO = apiService.getUserById(dto.publisherId)
        val publisher = UserDomain(
            id = userDTO.id,
            name = userDTO.name,
            phone = userDTO.phone,
            email = userDTO.email,
            role = userDTO.role,
            isPremium = userDTO.isPremium,
            badgesIds = userDTO.badgesIds,
            schedulesIds = userDTO.schedulesIds,
            reservationsIds = userDTO.reservationsIds,
            institution = InstitutionDomain(
                id = userDTO.institutionId,
                name = "Institución desconocida",
                description = "",
                members = emptyList(),
                buildings = emptyList()
            ),
            dietaryPreferencesTagIds = userDTO.dietaryPreferencesTagIds,
            commentsIds = userDTO.commentsIds,
            visitsIds = userDTO.visitsIds,
            suscribedRestaurantIds = userDTO.suscribedRestaurantIds,
            publishedAlertsIds = userDTO.publishedAlertsIds,
            savedProducts = emptyList()
        )

        val restaurants: List<RestaurantDTO> = apiService.getRestaurants()
        val restaurantDTO: RestaurantDTO? = restaurants.firstOrNull { it.id == dto.restaurantId }

        val restaurant = restaurantDTO?.let {
            RestaurantDomain(
                id = it.id,
                name = it.name,
                description = it.description,
                latitude = it.latitude,
                longitude = it.longitude,
                routeIndications = it.routeIndications,
                openingTime = it.openingTime,
                closingTime = it.closingTime,
                opensHolidays = it.opensHolidays,
                opensWeekends = it.opensWeekends,
                isActive = it.isActive,
                rating = it.rating,
                address = it.address,
                phone = it.phone,
                email = it.email,
                overviewPhoto = it.overviewPhoto,
                profilePhoto = it.profilePhoto,
                photos = it.photos,
                foodTags = emptyList(),
                dietaryTags = emptyList(),
                alertsIds = it.alertsIds,
                reservationsIds = it.reservationsIds,
                suscribersIds = it.suscribersIds,
                visitsIds = it.visitsIds,
                commentsIds = it.commentsIds,
                productsIds = it.productsIds
            )
        } ?: RestaurantDomain(
            id = dto.restaurantId,
            name = "Restaurante desconocido",
            description = "",
            latitude = 0.0,
            longitude = 0.0,
            routeIndications = "",
            openingTime = "",
            closingTime = "",
            opensHolidays = false,
            opensWeekends = false,
            isActive = false,
            rating = 0.0,
            address = "",
            phone = "",
            email = "",
            overviewPhoto = "",
            profilePhoto = "",
            photos = emptyList(),
            foodTags = emptyList(),
            dietaryTags = emptyList(),
            alertsIds = emptyList(),
            reservationsIds = emptyList(),
            suscribersIds = emptyList(),
            visitsIds = emptyList(),
            commentsIds = emptyList(),
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

    override suspend fun getAlerts(): List<AlertDomain> {
        val dtos: List<AlertDTO> = apiService.getAlerts()
        return dtos.map { dto -> mapDtoToDomain(dto) }
    }

    override suspend fun updateAlertVotes(alertId: String, newVotes: Int) {
        // Se crea un DTO parcial para la actualización utilizando solo el campo votes.
        val updateDto = UpdateAlertDTO(votes = newVotes)
        // Se invoca la función del ApiService para actualizar la alerta.
        apiService.updateAlertVotes(alertId, updateDto)
    }
}
