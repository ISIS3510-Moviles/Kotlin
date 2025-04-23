package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): UserRepository {

    override suspend fun getUserById(id: String): UserDTO {
        return apiService.getUserById(id)
    }

    override suspend fun createUser(user: UserDTO) {
        withContext(Dispatchers.IO) {  // Asegura ejecución en un hilo de fondo
            try {
                Log.d("API_CALL", "🚀 Enviando usuario: $user")

                val response = apiService.createUser(user)

                Log.d("API_CALL", "✅ Respuesta del servidor: ${response.code()} - ${response.message()}")

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e("API_CALL", "❌ Error en la solicitud: $errorBody")
                } else {
                    Log.d("API_CALL", "🎉 Usuario creado exitosamente")
                }
            } catch (e: CancellationException) {
                Log.e("API_CALL", "⚠️ La corutina fue cancelada antes de terminar: ${e.message}")
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Excepción al hacer la solicitud: ${e.message}")
            }
        }
    }

    override suspend fun getUsers(): List<UserDTO> {
        return apiService.getUsers()
    }

    override suspend fun updateUser(id: String, user: UserDTO): Boolean {
        return apiService.updateUser(id, user)
    }

    override suspend fun getUserByEmail(email: String): UserDTO {
        return apiService.getUserByEmail(email)
    }

}