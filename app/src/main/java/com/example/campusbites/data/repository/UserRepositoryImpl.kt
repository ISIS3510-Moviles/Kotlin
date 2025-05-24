package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.dto.UserUpdateDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.UserRepository
import javax.inject.Inject // <-- CAMBIO AQUÍ: de jakarta.inject.Inject a javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import java.io.IOException // Importar IOException

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): UserRepository {

    override suspend fun getUserById(id: String): UserDTO {
        return withContext(Dispatchers.IO) { // Añadir withContext para consistencia
            try {
                apiService.getUserById(id)
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Excepción al obtener usuario por ID ($id): ${e.message}", e)
                throw e // Re-lanzar para que la capa superior maneje el error
            }
        }
    }

    override suspend fun createUser(user: UserDTO) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("API_CALL", "🚀 Enviando usuario: $user")

                val response = apiService.createUser(user)

                Log.d("API_CALL", "✅ Respuesta del servidor: ${response.code()} - ${response.message()}")

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e("API_CALL", "❌ Error en la solicitud: ${response.code()} - $errorBody")
                    throw IOException("Failed to create user: ${response.code()} - $errorBody")
                } else {
                    Log.d("API_CALL", "🎉 Usuario creado exitosamente")
                }
            } catch (e: CancellationException) {
                Log.e("API_CALL", "⚠️ La corutina fue cancelada antes de terminar: ${e.message}")
                throw e
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Excepción al hacer la solicitud: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun updateUser(id: String, user: UserUpdateDTO): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("API_CALL", "🔄 Actualizando usuario con ID: $id, datos: $user")
                apiService.updateUser(id, user) // Si hay error, Retrofit lanza excepción
                Log.d("API_CALL", "✅ Usuario actualizado exitosamente.")
                true
            } catch (e: CancellationException) {
                Log.e("API_CALL", "⚠️ La corutina de actualización fue cancelada: ${e.message}")
                throw e
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Excepción al actualizar usuario: ${e.message}", e)
                throw e // opcional: o retorna false si prefieres manejarlo en lugar de lanzarlo
            }
        }
    }


    override suspend fun getUserByEmail(email: String): UserDTO {
        return withContext(Dispatchers.IO) { // Añadir withContext para consistencia
            try {
                apiService.getUserByEmail(email)
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Excepción al obtener usuario por email ($email): ${e.message}", e)
                throw e // Re-lanzar para que la capa superior maneje el error
            }
        }
    }
}