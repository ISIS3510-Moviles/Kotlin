package com.example.campusbites.domain.usecase.alert

import android.util.Log
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AlertRepository
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class CreateAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase // ASUME QUE TIENES ESTE USECASE
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
            // Estrategia 1: Corrutina con un dispatcher específico (Dispatchers.IO)
            // Aunque getRestaurantByIdUseCase es suspend y probablemente ya use IO,
            // lo hacemos explícito aquí para la demostración.
            val restaurant = withContext(Dispatchers.IO) {
                Log.d("ThreadingStrategy", "[Strategy 1] CreateAlertUseCase: Fetching restaurant for icon on ${Thread.currentThread().name}")
                getRestaurantByIdUseCase(restaurantId) // Llama al use case
            }

            if (restaurant != null && restaurant.profilePhoto.isNotBlank()) {
                alertIconUrl = restaurant.profilePhoto
                Log.d("CreateAlertUseCase", "Usando profilePhoto del restaurante como icono: $alertIconUrl")
            } else {
                Log.w("CreateAlertUseCase", "Restaurante no encontrado o sin profilePhoto para ID: $restaurantId. Usando icono por defecto.")
            }
        } catch (e: Exception) {
            Log.e("CreateAlertUseCase", "Error al obtener restaurante $restaurantId para el icono. Usando icono por defecto.", e)
        }

        // alertRepository.createAlert es una función suspend. Si implica operaciones de red/DB,
        // el repositorio (o las bibliotecas que usa como Retrofit/Realm)
        // deberían manejar el cambio a un hilo de background apropiado.
        return alertRepository.createAlert(
            datetime = currentDateTime,
            icon = alertIconUrl, // Usar la URL obtenida (o la de fallback)
            message = message,
            publisherId = user.id,
            restaurantId = restaurantId
        )
    }
}