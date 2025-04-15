package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.UserDomain
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserDomain?>
    suspend fun performCheckOrCreateUser(userId: String, userName: String, userEmail: String): UserDomain
    suspend fun signOut()
    fun updateCurrentUser(updatedUser: UserDomain?)

}