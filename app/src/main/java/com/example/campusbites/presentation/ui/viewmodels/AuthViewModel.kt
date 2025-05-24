package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

// Excepción personalizada para claridad
class InitialSetupNoNetworkException(message: String) : IOException(message)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    val user: StateFlow<UserDomain?> = authRepository.currentUser

    // Exponer el estado de red para que la UI pueda observarlo
    val isNetworkAvailable: StateFlow<Boolean> = connectivityMonitor.isNetworkAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Se mantiene activo 5s después del último observador
            initialValue = true // Asumir que hay red inicialmente, se actualizará rápido
        )

    init {
        Log.d("AuthViewModel_Instance", "ViewModel instance created/obtained: ${this.hashCode()} observing Repo: ${authRepository.hashCode()}")
    }

    fun updateUser(user: UserDomain?) {
        Log.d("AuthViewModel", "[${this.hashCode()}] Calling repo updateCurrentUser for user: ${user?.id}")
        authRepository.updateCurrentUser(user)
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "[${this.hashCode()}] Calling repo signOut")
                authRepository.signOut()
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "[${this.hashCode()}] Error during signOut: ${e.message}")
                withContext(Dispatchers.Main) { onComplete() } // Asegurar que onComplete se llame
            }
        }
    }

    fun checkOrCreateUser(
        userId: String,
        userName: String,
        userEmail: String,
        onSuccess: (UserDomain) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            if (!connectivityMonitor.isNetworkAvailable.first()) { // Comprobar estado de red actual
                Log.w("AuthViewModel", "No network connection for checkOrCreateUser.")
                withContext(Dispatchers.Main) {
                    onFailure(InitialSetupNoNetworkException("No network connection. Please connect to the internet to complete the initial setup."))
                }
                return@launch
            }

            try {
                Log.d("AuthViewModel", "[${this.hashCode()}] Calling repo performCheckOrCreateUser for email: $userEmail")
                val userResult = authRepository.performCheckOrCreateUser(userId, userName, userEmail)
                Log.d("AuthViewModel", "[${this.hashCode()}] Repo checkOrCreateUser success for ${userResult.id}.")
                withContext(Dispatchers.Main) {
                    onSuccess(userResult)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "[${this.hashCode()}] Repo checkOrCreateUser failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
}