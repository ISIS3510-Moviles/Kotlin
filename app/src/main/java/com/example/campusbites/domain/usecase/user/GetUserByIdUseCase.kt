package com.example.campusbites.domain.usecase.user

import android.util.Log
import com.example.campusbites.data.mapper.UserMapper // Importar UserMapper
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val repository: UserRepository,
    private val userMapper: UserMapper // Inyectar UserMapper
) {
    suspend operator fun invoke(id: String): UserDomain {

        val userDTO = repository.getUserById(id)

        return try {
            userMapper.mapDtoToDomain(userDTO)
        } catch (e: Exception) {

            Log.e("GetUserByIdUseCase", "Error mapping UserDTO for ID '$id'. Using fallback. Error: ${e.message}", e)
            userMapper.createFallbackUser(id) // Asumiendo que UserMapper tiene un método createFallbackUser
        }
    }
}