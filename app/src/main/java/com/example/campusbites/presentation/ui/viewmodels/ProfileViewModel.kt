// presentation/ui/viewmodels/ProfileViewModel.kt
package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.repository.AuthRepository // <-- Importa AuthRepository
import com.example.campusbites.domain.model.UserDomain // <-- Importa UserDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.io.IOException

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository // <-- Inyecta AuthRepository
) : ViewModel() {

    // Puedes exponer el currentUser del AuthRepository directamente a la UI
    val currentUser = authRepository.currentUser

    fun updateUserRole(newRole: String) { // No necesitas userId aquí, AuthRepository ya lo tiene
        viewModelScope.launch {
            try {
                val current = authRepository.getCurrentUser() // Obtener el usuario actual
                if (current != null) {
                    // Crear una copia del usuario con el rol actualizado
                    val updatedUser = current.copy(role = newRole)
                    // Llamar a updateCurrentUser en AuthRepository
                    authRepository.updateCurrentUser(updatedUser)
                    Log.d("ProfileViewModel", "User role updated locally to: $newRole")
                } else {
                    Log.w("ProfileViewModel", "Cannot update role, no current user found.")
                }
            } catch (e: IOException) {
                Log.e("ProfileViewModel", "Network error during role update process", e)
                // Aquí podrías mostrar un mensaje al usuario: "No hay internet, el cambio se sincronizará después."
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during role update process", e)
                // Aquí podrías mostrar un mensaje al usuario: "Error al actualizar el rol."
            }
        }
    }
}