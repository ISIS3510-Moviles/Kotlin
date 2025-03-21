package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.user.CreateUserUseCase
import com.example.campusbites.domain.usecase.user.GetUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getUsersUseCase: GetUsersUseCase
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

    fun checkOrCreateUser(
        userId: String,
        userName: String,
        userEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existingUser = getUsersUseCase().find { it.email == userEmail }

                if (existingUser == null) {
                    // Crear nuevo usuario si no existe
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
                    createUserUseCase(newUser)
                    setUser(newUser) // Guardar el usuario en el ViewModel
                } else {
                    // Si ya existe, solo actualizar en caso de que haya cambios en el email
                    if (existingUser.email != userEmail) {
                        val updatedUser = existingUser.copy(email = userEmail)
                        createUserUseCase(updatedUser)
                        setUser(updatedUser) // Guardar el usuario actualizado en el ViewModel
                    } else {
                        setUser(existingUser) // Guardar el usuario existente
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
}
