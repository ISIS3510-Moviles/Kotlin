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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getUsersUseCase: GetUsersUseCase
) : ViewModel() {

    private val _user = MutableStateFlow<UserDomain?>(null)
    val user: StateFlow<UserDomain?> = _user

    fun setUser(user: UserDomain?) {
        _user.value = user?.copy()
    }


    fun signOut() {
        _user.update { null }
    }

    fun checkOrCreateUser(
        userId: String,
        userName: String,
        userEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("API_CALL", "🔎 Buscando usuario con email: $userEmail")

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
                    Log.d("API_CALL", "🚀 Creando usuario nuevo...")
                    createUserUseCase(it)
                }

                withContext(Dispatchers.Main) {
                    _user.value = userToSet
                    Log.d("API_CALL", "✅ Usuario guardado en ViewModel: ${userToSet.id}")
                    Log.d("API_CALL", "✅ Usuario guardado en ViewModel: ${_user.value!!.name}")
                }

                onSuccess()
            } catch (e: Exception) {
                Log.e("API_CALL", "❌ Error en checkOrCreateUser: ${e.message}")
                onFailure(e)
            }
        }
    }

}
