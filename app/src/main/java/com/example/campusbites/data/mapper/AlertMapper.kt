package com.example.campusbites.data.mapper

import android.util.Log
import com.example.campusbites.data.dto.AlertDTO
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class AlertMapper @Inject constructor() {
    fun mapDtoToDomain(
        dto: AlertDTO,
        publisher: UserDomain,
        restaurant: RestaurantDomain
    ): AlertDomain {
        // El repositorio es responsable de asegurar que dto.id no sea null antes de llamar a este mapper.
        val alertId = dto.id ?: throw IllegalStateException("AlertDTO.id cannot be null when mapping to AlertDomain. This should have been filtered by the repository.")


        val parsedDateTime = dto.datetime?.let {
            try {
                Instant.parse(it).atZone(ZoneOffset.UTC).toLocalDateTime()
            } catch (e: Exception) {
                Log.e("AlertMapper", "Failed to parse datetime: $it for alert ${dto.id}. Using current time.", e)
                LocalDateTime.now(ZoneOffset.UTC)
            }
        } ?: run {
            Log.w("AlertMapper", "AlertDTO datetime is null for alert ${dto.id}. Using current time.")
            LocalDateTime.now(ZoneOffset.UTC)
        }

        return AlertDomain(
            id = alertId, // Usar la variable verificada
            datetime = parsedDateTime,
            icon = dto.icon,
            message = dto.message,
            votes = dto.votes,
            publisher = publisher,
            restaurantDomain = restaurant
        )
    }
}