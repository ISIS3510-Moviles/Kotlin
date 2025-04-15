package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.cache.InMemoryAlertCache
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.usecase.alert.CreateAlertUseCase
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase
import com.example.campusbites.domain.usecase.alert.UpdateAlertUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase,
    private val alertCache: InMemoryAlertCache,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _restaurants = MutableStateFlow<List<RestaurantDomain>>(emptyList())
    val restaurants: StateFlow<List<RestaurantDomain>> = _restaurants.asStateFlow()

    init {
        observeAlertCache()
        fetchRestaurants()
        triggerAlertNetworkFetchIfNeeded()
    }

    private fun observeAlertCache() {
        viewModelScope.launch {
            alertCache.alerts
                .catch { e ->
                    Log.e("AlertsViewModel", "Error observing alerts cache", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load cached alerts") }
                }
                .collect { cachedAlerts ->
                    Log.d("AlertsViewModel", "Alerts cache observed: ${cachedAlerts.size}")
                    _uiState.update {
                        it.copy(alerts = cachedAlerts, isLoading = it.isLoading)
                    }
                }
        }
    }

    private fun triggerAlertNetworkFetchIfNeeded() {
        if (alertCache.getAlerts().isNotEmpty()) {
            Log.d("AlertsViewModel", "Alert cache already populated. Skipping initial network fetch.")
            _uiState.update { it.copy(isLoading = false) }

            viewModelScope.launch { fetchAndCacheAlerts() }
            return
        }
        refreshAlerts()
    }

    private fun refreshAlerts() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            fetchAndCacheAlerts()
            _uiState.update { it.copy(isLoading = false) }
        }
    }


    private suspend fun fetchAndCacheAlerts(): List<AlertDomain>? {
        val currentUser = authRepository.currentUser.first()
        if (currentUser == null) {
            Log.w("AlertsViewModel", "Cannot fetch alerts without user.")
            _uiState.update { it.copy(errorMessage = "User not logged in", isLoading = false) }
            return null
        }
        return try {
            val alerts = getAlertsUseCase()
            alertCache.updateAlerts(alerts)
            Log.d("AlertsViewModel", "Fetched and cached ${alerts.size} alerts in memory.")
            alerts
        } catch (e: Exception) {
            Log.e("AlertsViewModel", "Error fetching/caching alerts: ${e.message}", e)
            _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error fetching alerts", isLoading = false) }
            null
        }
    }


    private fun fetchRestaurants() {
        viewModelScope.launch {
            try {
                val restaurants = getRestaurantsUseCase()
                _restaurants.value = restaurants
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error al cargar restaurantes"

                )}
            }
        }
    }

    fun upvote(alert: AlertDomain) {
        val originalAlerts = alertCache.getAlerts()
        val alertIndex = originalAlerts.indexOfFirst { it.id == alert.id }
        if (alertIndex == -1) return

        val optimisticallyUpdatedAlert = alert.copy(votes = alert.votes + 1)
        val newAlertList = originalAlerts.toMutableList().apply {
            set(alertIndex, optimisticallyUpdatedAlert)
        }
        alertCache.updateAlerts(newAlertList)


        viewModelScope.launch {
            try {
                updateAlertUseCase(alert.id, alert.votes + 1)

            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error upvoting alert ${alert.id} on server", e)

                alertCache.updateAlerts(originalAlerts)
                _uiState.update { it.copy(errorMessage = "Failed to sync upvote") }
            }
        }
    }

    fun downvote(alert: AlertDomain) {
        val originalAlerts = alertCache.getAlerts()
        val alertIndex = originalAlerts.indexOfFirst { it.id == alert.id }
        if (alertIndex == -1) return

        val optimisticallyUpdatedAlert = alert.copy(votes = alert.votes - 1)
        val newAlertList = originalAlerts.toMutableList().apply {
            set(alertIndex, optimisticallyUpdatedAlert)
        }
        alertCache.updateAlerts(newAlertList)


        viewModelScope.launch {
            try {
                updateAlertUseCase(alert.id, alert.votes - 1)

            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error downvoting alert ${alert.id} on server", e)

                alertCache.updateAlerts(originalAlerts)
                _uiState.update { it.copy(errorMessage = "Failed to sync downvote") }
            }
        }
    }

    fun createAlert(description: String, restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(errorMessage = "User not available to create alert", isLoading = false) }
                return@launch
            }

            try {
                createAlertUseCase(description, restaurantId, currentUser)

                fetchAndCacheAlerts()
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error creating alert", e)
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error al crear la alerta"
                )}
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

}

data class AlertsUiState(
    val alerts: List<AlertDomain> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)