package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.preferences.UserSessionRepository
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.user.CreateUserUseCase
import com.example.campusbites.domain.usecase.user.GetUsersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.campusbites.domain.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val userSessionRepository: UserSessionRepository
) : AuthRepository {

    private val _currentUser = MutableStateFlow<UserDomain?>(null)
    override val currentUser: StateFlow<UserDomain?> = _currentUser.asStateFlow()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    init {
        Log.d("AuthRepository_Instance", "AuthRepositoryImpl Singleton instance created: ${this.hashCode()}")
        repositoryScope.launch {
            val initialUser = userSessionRepository.userSessionFlow.firstOrNull()
            if (initialUser != null && _currentUser.value == null) {
                Log.d("AuthRepository", "Restoring session from DataStore for user: ${initialUser.id}")
                _currentUser.value = initialUser
            } else if (initialUser == null) {
                Log.d("AuthRepository", "No session found in DataStore.")
            } else {
                Log.d("AuthRepository", "Session already loaded or being loaded.")
            }
        }
    }

    override suspend fun performCheckOrCreateUser(userId: String, userName: String, userEmail: String): UserDomain =
        withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "🔎 Buscando usuario con email: $userEmail")
                val existingUser = getUsersUseCase().find { it.email == userEmail }

                val userToSet = existingUser ?: UserDomain(
                    id = userId,
                    name = userName,
                    email = userEmail,
                    phone = "",
                    role = "user",
                    isPremium = false,
                    badgesIds = emptyList(),
                    schedulesIds = emptyList(),
                    reservationsIds = emptyList(),
                    institution = null,
                    dietaryPreferencesTagIds = emptyList(),
                    commentsIds = emptyList(),
                    visitsIds = emptyList(),
                    suscribedRestaurantIds = emptyList(),
                    publishedAlertsIds = emptyList(),
                    savedProducts = emptyList()
                ).also { newUser ->
                    Log.d("AuthRepository", "🚀 Creando usuario nuevo...")
                    createUserUseCase(newUser)
                }

                Log.d("AuthRepository", "✅ User obtained/created: ${userToSet.id}. Updating StateFlow and saving session.")
                _currentUser.value = userToSet
                userSessionRepository.saveUserSession(userToSet)

                userToSet
            } catch (e: Exception) {
                Log.e("AuthRepository", "❌ Error en performCheckOrCreateUser: ${e.message}", e)
                throw e
            }
        }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            Log.d("AuthRepository", "🗑️ Clearing user state in Repository and DataStore.")
            _currentUser.value = null
            userSessionRepository.clearUserSession()
        }
    }

    override fun updateCurrentUser(updatedUser: UserDomain?) {
        Log.d("AuthRepository", "🔄 Updating currentUser in repository with: ${updatedUser?.id}")
        _currentUser.value = updatedUser?.copy()
        if (updatedUser != null) {
            repositoryScope.launch {
                userSessionRepository.saveUserSession(updatedUser)
                Log.d("AuthRepository", "🔄 User session updated in DataStore for ID: ${updatedUser.id}")
            }
        } else {
            repositoryScope.launch {
                userSessionRepository.clearUserSession()
                Log.d("AuthRepository", "🔄 User is null, clearing session in DataStore.")
            }
        }
    }
}