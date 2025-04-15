package com.example.campusbites.data.repository

import android.util.Log
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

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getUsersUseCase: GetUsersUseCase
    // Puedes inyectar aquí otras dependencias si el repo las necesita (ej. SharedPreferences, Firebase Auth)
) : AuthRepository {

    // --- La ÚNICA fuente de verdad para el estado del usuario ---
    private val _currentUser = MutableStateFlow<UserDomain?>(null)
    override val currentUser: StateFlow<UserDomain?> = _currentUser.asStateFlow()
    // ---

    init {
        Log.d("AuthRepository_Instance", "AuthRepositoryImpl Singleton instance created: ${this.hashCode()}")
        // Podrías intentar cargar el usuario desde caché/local storage aquí si lo implementas
    }

    override suspend fun performCheckOrCreateUser(userId: String, userName: String, userEmail: String): UserDomain =
        withContext(Dispatchers.IO) { // Ejecuta en hilo IO
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
                ).also {
                    Log.d("AuthRepository", "🚀 Creando usuario nuevo...")
                    createUserUseCase(it)
                }

                Log.d("AuthRepository", "✅ Usuario obtenido/creado: ${userToSet.id}. Actualizando StateFlow.")
                _currentUser.value = userToSet // <-- Actualiza el StateFlow compartido
                userToSet // Devuelve el usuario
            } catch (e: Exception) {
                Log.e("AuthRepository", "❌ Error en performCheckOrCreateUser: ${e.message}")
                throw e // Relanza la excepción para que el ViewModel la maneje
            }
        }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            Log.d("AuthRepository", "🗑️ Limpiando estado del usuario en Repositorio.")
            _currentUser.value = null // Limpia el estado
        }
    }

    override fun updateCurrentUser(updatedUser: UserDomain?) {
        Log.d("AuthRepository", "🔄 Actualizando currentUser en repositorio con: ${updatedUser?.id}")
        _currentUser.value = updatedUser?.copy()
    }
}