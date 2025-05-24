// File: com/example/campusbites/presentation/ui/viewmodels/EditRestaurantViewModel.kt (Nuevo archivo)

package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.dto.UpdateRestaurantDTO
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.restaurant.UpdateRestaurantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditRestaurantViewModel @Inject constructor(
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase,
    private val updateRestaurantUseCase: UpdateRestaurantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRestaurantUiState())
    val uiState: StateFlow<EditRestaurantUiState> = _uiState.asStateFlow()

    fun loadRestaurantForEdit(restaurantId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val restaurant = getRestaurantByIdUseCase(restaurantId)
                _uiState.update { currentState ->
                    currentState.copy(
                        restaurant = restaurant,
                        description = restaurant?.description ?: "",
                        latitude = restaurant?.latitude?.toString() ?: "",
                        longitude = restaurant?.longitude?.toString() ?: "",
                        locationDescription = restaurant?.address ?: "", // Asumiendo que address es locationDescription
                        mainPhotoLink = restaurant?.overviewPhoto ?: "",
                        homePhotoLink = restaurant?.profilePhoto ?: "",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("EditRestaurantVM", "Error loading restaurant $restaurantId: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load restaurant: ${e.localizedMessage}") }
            }
        }
    }

    fun updateRestaurant(restaurantId: String, updateDto: UpdateRestaurantDTO) {
        _uiState.update { it.copy(isUpdating = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            try {
                val success = updateRestaurantUseCase(restaurantId, updateDto)
                if (success) {
                    _uiState.update { it.copy(isUpdating = false, successMessage = "Restaurant updated successfully!") }
                    Log.d("EditRestaurantVM", "Restaurant $restaurantId updated successfully.")
                    // Opcional: Recargar el restaurante para mostrar los últimos datos
                    loadRestaurantForEdit(restaurantId)
                } else {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = "Failed to update restaurant. Check network or try again.") }
                    Log.e("EditRestaurantVM", "Failed to update restaurant $restaurantId.")
                }
            } catch (e: Exception) {
                Log.e("EditRestaurantVM", "Error updating restaurant $restaurantId: ${e.message}", e)
                _uiState.update { it.copy(isUpdating = false, errorMessage = "An error occurred: ${e.localizedMessage}") }
            }
        }
    }

    // Funciones para actualizar el estado de los campos de texto
    fun onDescriptionChange(newValue: String) { _uiState.update { it.copy(description = newValue) } }
    fun onLatitudeChange(newValue: String) { _uiState.update { it.copy(latitude = newValue) } }
    fun onLongitudeChange(newValue: String) { _uiState.update { it.copy(longitude = newValue) } }
    fun onLocationDescriptionChange(newValue: String) { _uiState.update { it.copy(locationDescription = newValue) } }
    fun onMainPhotoLinkChange(newValue: String) { _uiState.update { it.copy(mainPhotoLink = newValue) } }
    fun onHomePhotoLinkChange(newValue: String) { _uiState.update { it.copy(homePhotoLink = newValue) } }
}


data class EditRestaurantUiState(
    val restaurant: RestaurantDomain? = null,
    val description: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val locationDescription: String = "", // Asumiendo que esto mapea a 'address'
    val mainPhotoLink: String = "", // Asumiendo que esto mapea a 'overviewPhoto'
    val homePhotoLink: String = "", // Asumiendo que esto mapea a 'profilePhoto'
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)