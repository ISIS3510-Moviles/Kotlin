package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.dto.UserUpdateDTO
import com.example.campusbites.domain.model.UserDomain

interface UserRepository {
    suspend fun getUserById(id: String): UserDTO
    suspend fun createUser(user: UserDTO)
    suspend fun getUsers(): List<UserDTO>
    suspend fun updateUser(id: String, user: UserUpdateDTO): Boolean
    suspend fun getUserByEmail(email: String): UserDTO
}