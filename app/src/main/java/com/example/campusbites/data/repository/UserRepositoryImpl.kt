package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.UserRepository
import jakarta.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): UserRepository {

    override suspend fun getUserById(id: String): UserDTO {
        return apiService.getUserById(id)
    }

    override suspend fun createUser(user: UserDTO) {
        Log.d("API_CALL", "Enviando usuario: $user") // Log antes de la llamada
        val response = apiService.createUser(user)
        Log.d("API_CALL", "Respuesta del servidor: $response") // Log después de la llamada
    }

    override suspend fun getUsers(): List<UserDTO> {
        return apiService.getUsers()
    }

}