package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.cache.InMemoryAlertCache
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.DraftAlert
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.repository.DraftAlertRepository
import com.example.campusbites.domain.service.AlertNotificationService
import com.example.campusbites.domain.usecase.alert.CreateAlertUseCase
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase
import com.example.campusbites.domain.usecase.alert.UpdateAlertUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase,
    private val alertCache: InMemoryAlertCache,
    private val authRepository: AuthRepository,
    private val draftAlertRepository: DraftAlertRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val notificationService: AlertNotificationService,
    private val homeDataRepository: HomeDataRepository // Added HomeDataRepository
) : ViewModel() {

    // Use restaurants from the cache instead of fetching them directly
    val restaurants: StateFlow<List<RestaurantDomain>> = homeDataRepository.nearbyRestaurantsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _draftAlerts = MutableStateFlow<List<DraftAlert>>(emptyList())
    val draftAlerts = _draftAlerts.asStateFlow()

    data class AlertsUiState(
        val alerts: List<AlertDomain> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val latestDraftAlert: DraftAlert? = null
    )

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)

    // Combine network status with draft alerts to determine UI state
    val connectivityState = combine(
        connectivityMonitor.isNetworkAvailable,
        draftAlertRepository.getAllDraftAlerts()
    ) { isConnected, drafts ->
        _isNetworkAvailable.value = isConnected
        _draftAlerts.value = drafts

        // When connection returns and we have drafts, show notification
        if (isConnected && drafts.isNotEmpty() && notificationService.hasNotificationPermission()) {
            notificationService.showDraftAlertAvailableNotification(drafts.size)
        }

        ConnectivityUiState(
            isConnected = isConnected,
            hasDraftAlerts = drafts.isNotEmpty()
        )
    }.catch { e ->
        Log.e("AlertsViewModel", "Error monitoring connectivity", e)
        emit(ConnectivityUiState(isConnected = false, hasDraftAlerts = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectivityUiState(isConnected = false, hasDraftAlerts = false)
    )

    private var hasCheckedRestaurantsCache = false

    init {
        observeAlertCache()
        checkRestaurantsCacheAndFetchIfNeeded() // Added check for restaurants cache
        triggerAlertNetworkFetchIfNeeded()
        monitorDraftAlerts()
    }

    private fun checkRestaurantsCacheAndFetchIfNeeded() {
        if (hasCheckedRestaurantsCache) return

        viewModelScope.launch {
            val cachedRestaurants = homeDataRepository.nearbyRestaurantsFlow.first()
            if (cachedRestaurants.isEmpty() && connectivityMonitor.isNetworkAvailable.first()) {
                // Cache is empty and we have network - fetch restaurants
                try {
                    Log.d("AlertsViewModel", "No cached restaurants found, fetching from network")
                    val restaurants = getRestaurantsUseCase()
                    homeDataRepository.saveNearbyRestaurants(restaurants)
                    Log.d("AlertsViewModel", "Fetched and saved ${restaurants.size} restaurants to cache")
                } catch (e: Exception) {
                    Log.e("AlertsViewModel", "Error fetching restaurants: ${e.message}", e)
                    _uiState.update { it.copy(errorMessage = "Failed to load restaurants") }
                }
            }
            hasCheckedRestaurantsCache = true
        }
    }

    private fun monitorDraftAlerts() {
        viewModelScope.launch {
            draftAlertRepository.getAllDraftAlerts()
                .distinctUntilChanged()
                .collect { drafts ->
                    _draftAlerts.value = drafts
                }
        }
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

    fun refreshAlerts() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            fetchAndCacheAlerts()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshAlertsManually() {
        refreshAlerts()
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
            Log.d("AlertsViewModel", "Fetched ${alerts.size} alerts from server.")
            alertCache.updateAlerts(alerts)
            Log.d("AlertsViewModel", "Fetched and cached ${alerts.size} alerts in memory.")
            alerts
        } catch (e: Exception) {
            Log.e("AlertsViewModel", "Error fetching/caching alerts: ${e.message}", e)
            _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error fetching alerts", isLoading = false) }
            null
        }
    }

    fun sendDraftAlert(draftId: String, message: String, restaurantId: String) {
        viewModelScope.launch {
            if (!_isNetworkAvailable.value) {
                _uiState.update { it.copy(
                    errorMessage = "Cannot send draft alert without internet connection",
                    successMessage = null,
                    isLoading = false
                ) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(
                    errorMessage = "User not available to send draft alert",
                    isLoading = false,
                    successMessage = null
                ) }
                return@launch
            }

            try {
                Log.d("AlertsViewModel", "Sending draft alert with message: $message for restaurant: $restaurantId")
                // Primero enviar la alerta
                createAlertUseCase(message, restaurantId, currentUser)
                Log.d("AlertsViewModel", "Alert sent successfully")

                // Luego eliminar el borrador, pero no permitas que un error aquí afecte el resultado exitoso
                try {
                    Log.d("AlertsViewModel", "Deleting draft with id: $draftId")
                    draftAlertRepository.deleteDraftAlert(draftId)
                    Log.d("AlertsViewModel", "Draft deleted successfully")
                } catch (e: Exception) {
                    // Sólo registramos el error, no informamos al usuario ya que la alerta se envió correctamente
                    Log.e("AlertsViewModel", "Failed to delete draft, but alert was sent: $draftId", e)
                }

                // Actualizar lista de alertas
                fetchAndCacheAlerts()

                // Informar al usuario del éxito
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Alert sent successfully",
                    errorMessage = null
                )}
            } catch (e: Exception) {
                // Error al enviar la alerta
                Log.e("AlertsViewModel", "Error sending draft alert: ${e.message}", e)
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error sending draft alert",
                    isLoading = false,
                    successMessage = null
                )}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun deleteDraftAlert(draftId: String) {
        viewModelScope.launch {
            try {
                Log.d("AlertsViewModel", "Attempting to delete draft with ID: $draftId")
                _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

                draftAlertRepository.deleteDraftAlert(draftId)

                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Draft alert deleted",
                    errorMessage = null
                )}
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error deleting draft alert: $draftId", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete draft: ${e.localizedMessage ?: "Unknown error"}",
                    successMessage = null
                )}
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
                if (_isNetworkAvailable.value) {
                    // Online - send directly
                    createAlertUseCase(description, restaurantId, currentUser)
                    fetchAndCacheAlerts()
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Alert created successfully"
                    )}
                } else {
                    // Offline - save as draft
                    val restaurantName = restaurants.value.find { it.id == restaurantId }?.name ?: "Unknown Restaurant"
                    draftAlertRepository.saveDraftAlert(description, restaurantId, restaurantName)
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Alert saved as draft. Will be sent when you're back online."
                    )}
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error creating alert", e)
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error creating alert",
                    isLoading = false
                )}
            }
        }
    }

    fun getLatestDraftAlert() {
        viewModelScope.launch {
            val latestDraft = draftAlertRepository.getLatestDraftAlert()
            if (latestDraft != null) {
                _uiState.update { it.copy(latestDraftAlert = latestDraft) }
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
}

data class ConnectivityUiState(
    val isConnected: Boolean,
    val hasDraftAlerts: Boolean
)