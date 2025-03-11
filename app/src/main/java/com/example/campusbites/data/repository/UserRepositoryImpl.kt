package com.example.campusbites.data.repository

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
}