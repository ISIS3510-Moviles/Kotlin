package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.usecase.user.UpdateUserRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log // Import Log for logging errors
import java.io.IOException // Import IOException for network errors

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val updateUserRoleUseCase: UpdateUserRoleUseCase
) : ViewModel() {

    fun updateUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                updateUserRoleUseCase(userId, newRole)
            } catch (e: IOException) {
                Log.e("ProfileViewModel", "Network error updating user role for $userId", e)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating user role for $userId", e)
            }
        }
    }
}