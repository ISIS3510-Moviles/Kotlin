package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val user: StateFlow<UserDomain?> = authRepository.currentUser

    init {
        Log.d("AuthViewModel_Instance", "ViewModel instance created/obtained: ${this.hashCode()} observing Repo: ${authRepository.hashCode()}")
    }

    fun updateUser(user: UserDomain?) {
        Log.d("AuthViewModel", "[${this.hashCode()}] Calling repo updateCurrentUser")
        authRepository.updateCurrentUser(user)
    }


    fun signOut(onComplete: () -> Unit) { // Añadimos callback por si la UI necesita reaccionar
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "[${this.hashCode()}] Calling repo signOut")
                authRepository.signOut() // Delega al repositorio
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "[${this.hashCode()}] Error during signOut: ${e.message}")
                // Podrías tener un callback de error también
                withContext(Dispatchers.Main) { onComplete() } // Llama igual para desbloquear UI? O manejar error.
            }
        }
    }
    fun checkOrCreateUser(
        userId: String,
        userName: String,
        userEmail: String,
        onSuccess: (UserDomain) -> Unit, // Pasamos el usuario por si la Activity lo necesita
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "[${this.hashCode()}] Calling repo performCheckOrCreateUser")
                val userResult = authRepository.performCheckOrCreateUser(userId, userName, userEmail)
                Log.d("AuthViewModel", "[${this.hashCode()}] Repo checkOrCreateUser success.")
                withContext(Dispatchers.Main) {
                    onSuccess(userResult)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "[${this.hashCode()}] Repo checkOrCreateUser failed: ${e.message}")
                // Llama al callback de fallo
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }

    // Ya no es necesario si no hay lógica extra
    // fun updateUser(user: UserDomain) { ... }
}