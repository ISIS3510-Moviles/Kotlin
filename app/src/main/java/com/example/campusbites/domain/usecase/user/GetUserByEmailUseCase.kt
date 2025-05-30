package com.example.campusbites.domain.usecase.user

import android.util.Log
import com.example.campusbites.data.mapper.UserMapper // Importar UserMapper
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import javax.inject.Inject

class GetUserByEmailUseCase @Inject constructor(
    private val repository: UserRepository,
    private val userMapper: UserMapper // Inyectar UserMapper
) {
    suspend operator fun invoke(email: String): UserDomain? {
        return try {
            val userDTO = repository.getUserByEmail(email) // Esto devuelve UserDTO

            userMapper.mapDtoToDomain(userDTO)
        } catch (e: Exception) {
            // Maneja errores de red, respuestas 404 (usuario no encontrado), etc.
            Log.e("GetUserByEmailUseCase", "Error fetching user by email '$email': ${e.message}", e)
            null // Devuelve null si hay cualquier error o el usuario no se encuentra.
        }
    }
}