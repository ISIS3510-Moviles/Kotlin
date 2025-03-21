package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.user.CreateUserUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.user.GetUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase
) : ViewModel() {

    private val _user = MutableStateFlow<UserDomain?>(null)
    val user: StateFlow<UserDomain?> = _user

    fun setUser(user: UserDomain?) {
        viewModelScope.launch {
            _user.value = user
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _user.value = null
        }
    }

    suspend fun getUserById(userId: String): UserDomain? {
        return getUserByIdUseCase(userId) // 🔹 Corrección: Llamar correctamente el caso de uso
    }

    fun checkOrCreateUser(
        userId: String,
        userName: String,
        userEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {  // 🔹 Se ejecuta en un hilo de fondo
            try {
                Log.d("API_CALL", "🔎 Buscando usuario con email: $userEmail")

                val existingUser = getUsersUseCase().find { it.email == userEmail }

                if (existingUser == null) {
                    Log.d("API_CALL", "🚀 Creando usuario nuevo...")
                    val newUser = UserDomain(
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
                    )

                    createUserUseCase(newUser) // 🔹 Crear usuario en la base de datos
                    setUser(newUser) // 🔹 Guardar en el ViewModel

                    Log.d("API_CALL", "✅ Usuario creado y guardado en ViewModel")
                } else {
                    Log.d("API_CALL", "✅ Usuario existente encontrado: ${existingUser.id}")
                    setUser(existingUser)
                }

                onSuccess()
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Error en checkOrCreateUser: ${e.message}")
                onFailure(e)
            }
        }
    }
}
