package com.example.campusbites.domain.usecase.alert

import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AlertRepository
import java.time.Instant
import javax.inject.Inject

class CreateAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    suspend operator fun invoke(
        message: String,
        restaurantId: String,
        user: UserDomain
    ): Boolean {
        // Aquí se deberia obtener el usuario actual el usuario actual autenticado
        val currentUserId = user
//        val currentUserId = userRepository.getCurrentUserId()

        // Algunos valores por defecto para la alerta
        val currentDateTime = Instant.now().toString()
        val defaultIcon = "https://img.freepik.com/vector-gratis/diseno-logotipo-restaurante-dibujado-mano_23-2151269677.jpg?semt=ais_hybrid"

        return alertRepository.createAlert(
            datetime = currentDateTime,
            icon = defaultIcon,
            message = message,
            publisherId = currentUserId.id,
            restaurantId = restaurantId
        )
    }
}