package com.example.campusbites.domain.usecase.alert

import android.util.Log // Asegúrate de importar Log
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AlertRepository
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase // ASUME QUE TIENES ESTE USECASE
import java.time.Instant
import javax.inject.Inject

class CreateAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase
) {
    suspend operator fun invoke(
        message: String,
        restaurantId: String,
        user: UserDomain
    ): AlertDomain {
        val currentDateTime = Instant.now().toString()

        // Obtener la foto de perfil del restaurante para usarla como icono de la alerta
        var alertIconUrl = "https://img.freepik.com/vector-gratis/diseno-logotipo-restaurante-dibujado-mano_23-2151269677.jpg?semt=ais_hybrid" // Fallback default icon

        try {
            val restaurant = getRestaurantByIdUseCase(restaurantId) // Llama al use case
            if (restaurant != null && restaurant.profilePhoto.isNotBlank()) {
                alertIconUrl = restaurant.profilePhoto
                Log.d("CreateAlertUseCase", "Usando profilePhoto del restaurante como icono: $alertIconUrl")
            } else {
                Log.w("CreateAlertUseCase", "Restaurante no encontrado o sin profilePhoto para ID: $restaurantId. Usando icono por defecto.")
            }
        } catch (e: Exception) {
            Log.e("CreateAlertUseCase", "Error al obtener restaurante $restaurantId para el icono. Usando icono por defecto.", e)
        }

        return alertRepository.createAlert(
            datetime = currentDateTime,
            icon = alertIconUrl, // Usar la URL obtenida (o la de fallback)
            message = message,
            publisherId = user.id,
            restaurantId = restaurantId
        )
    }
}