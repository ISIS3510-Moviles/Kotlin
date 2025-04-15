package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase
import com.example.campusbites.domain.usecase.alert.CreateAlertUseCase
import com.example.campusbites.domain.usecase.alert.UpdateAlertUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _restaurants = MutableStateFlow<List<RestaurantDomain>>(emptyList())
    val restaurants: StateFlow<List<RestaurantDomain>> = _restaurants.asStateFlow()

    init {
        fetchAlerts()
        fetchRestaurants()
    }

    private fun fetchAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                Log.d("AlertsViewModel", "🚀 Antes de getAlertsUseCase")
                val alerts = getAlertsUseCase()
                Log.d("AlertsViewModel", "✅ Después de getAlertsUseCase")

                _uiState.value = _uiState.value.copy(alerts = alerts, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Error desconocido",
                    isLoading = false
                )
            }
        }
    }

    private fun fetchRestaurants() {
        viewModelScope.launch {
            try {
                val restaurants = getRestaurantsUseCase()
                _restaurants.value = restaurants
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Error al cargar restaurantes",
                    isLoading = false
                )
            }
        }
    }

    fun upvote(alert: AlertDomain) {
        viewModelScope.launch {
            updateAlertUseCase(alert.id, alert.votes + 1)
            fetchAlerts()
        }
    }

    fun downvote(alert: AlertDomain) {
        viewModelScope.launch {
            updateAlertUseCase(alert.id, alert.votes - 1)
            fetchAlerts()
        }
    }

    fun createAlert(description: String, restaurantId: String, authViewModel: AuthViewModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                var userid = ""
                if (authViewModel.user.value != null) {
                    userid = authViewModel.user.value!!.id
                }

                val user = getUserByIdUseCase(userid)
                createAlertUseCase(description, restaurantId, user)
                fetchAlerts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Error al crear la alerta",
                    isLoading = false
                )
            }
        }
    }

}

data class AlertsUiState(
    val alerts: List<AlertDomain> = emptyList(),
    val isLoading: Boolean = false,
    val user: UserDomain? = null,
    val errorMessage: String? = null
)