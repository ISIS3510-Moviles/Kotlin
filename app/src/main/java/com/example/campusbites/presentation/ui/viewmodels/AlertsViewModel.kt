package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// ELIMINAR: import com.example.campusbites.data.cache.InMemoryAlertCache
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.DraftAlert
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.repository.DraftAlertRepository
import com.example.campusbites.domain.service.AlertNotificationService
import com.example.campusbites.domain.usecase.alert.CreateAlertUseCase
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase // Puede renombrarse a FetchRemoteAlertsUseCase
import com.example.campusbites.domain.usecase.alert.ObserveLocalAlertsUseCase // NUEVO
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
    private val fetchRemoteAlertsUseCase: GetAlertsUseCase,
    private val observeLocalAlertsUseCase: ObserveLocalAlertsUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase,
    private val authRepository: AuthRepository,
    private val draftAlertRepository: DraftAlertRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val notificationService: AlertNotificationService,
    private val homeDataRepository: HomeDataRepository
) : ViewModel() {

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
        val isLoading: Boolean = true, // Inicia en true para la carga inicial
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val latestDraftAlert: DraftAlert? = null,
        val noAlertsMessageVisible: Boolean = false
    )

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)

    val connectivityState = combine(
        connectivityMonitor.isNetworkAvailable,
        draftAlertRepository.getAllDraftAlerts()
    ) { isConnected, drafts ->
        _isNetworkAvailable.value = isConnected
        _draftAlerts.value = drafts
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
    private var initialAlertFetchAttempted = false

    init {
        observeLocalAlerts()
        checkRestaurantsCacheAndFetchIfNeeded()
        triggerInitialAlertFetch()
        monitorDraftAlerts()
    }

    private fun checkRestaurantsCacheAndFetchIfNeeded() {
        if (hasCheckedRestaurantsCache) return
        viewModelScope.launch {
            val cachedRestaurants = homeDataRepository.nearbyRestaurantsFlow.first()
            if (cachedRestaurants.isEmpty() && connectivityMonitor.isNetworkAvailable.first()) {
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

    private fun observeLocalAlerts() {
        viewModelScope.launch {
            observeLocalAlertsUseCase()
                .catch { e ->
                    Log.e("AlertsViewModel", "Error observing local alerts", e)
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load alerts from database",
                        noAlertsMessageVisible = it.alerts.isEmpty()
                    )}
                }
                .collect { localAlerts ->
                    Log.d("AlertsViewModel", "Local alerts observed: ${localAlerts.size}")
                    _uiState.update {
                        it.copy(
                            alerts = localAlerts,
                            noAlertsMessageVisible = localAlerts.isEmpty() && !it.isLoading
                                    && initialAlertFetchAttempted
                        )
                    }

                    if (initialAlertFetchAttempted && localAlerts.isEmpty() && !_uiState.value.isLoading) {
                        _uiState.update { it.copy(noAlertsMessageVisible = true) }
                    }
                }
        }
    }

    private fun triggerInitialAlertFetch() {

        viewModelScope.launch {
            if (connectivityMonitor.isNetworkAvailable.first()) {
                refreshAlerts()
            } else {

                initialAlertFetchAttempted = true

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        noAlertsMessageVisible = state.alerts.isEmpty()
                    )
                }
                Log.d("AlertsViewModel", "No network for initial fetch, relying on local DB.")
            }
        }
    }

    fun refreshAlerts() { // Usado para pull-to-refresh o recarga manual
        _uiState.update { it.copy(isLoading = true, errorMessage = null, noAlertsMessageVisible = false) }
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                Log.w("AlertsViewModel", "Cannot fetch alerts without user.")
                _uiState.update { it.copy(errorMessage = "User not logged in", isLoading = false) }
                initialAlertFetchAttempted = true
                // Verificar si se debe mostrar "no hay alertas" si la lista actual está vacía
                if (_uiState.value.alerts.isEmpty()) {
                    _uiState.update { it.copy(noAlertsMessageVisible = true) }
                }
                return@launch
            }
            try {
                fetchRemoteAlertsUseCase() // Esto obtiene de la red y guarda en Realm
                // El Flow de observeLocalAlertsUseCase actualizará la UI
                Log.d("AlertsViewModel", "Alerts refreshed from server and saved to DB.")
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error refreshing alerts: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error refreshing alerts") }
            } finally {
                initialAlertFetchAttempted = true // Marcar que el intento de fetch ha ocurrido
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        noAlertsMessageVisible = state.alerts.isEmpty()
                    )
                }
            }
        }
    }

    fun refreshAlertsManually() {
        refreshAlerts()
    }


    fun createAlert(description: String, restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(errorMessage = "User not available to create alert", isLoading = false) }
                return@launch
            }

            try {
                if (_isNetworkAvailable.value) {
                    createAlertUseCase(description, restaurantId, currentUser)
                    // La DB se actualiza a través del repositorio, el Flow la recogerá.
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Alert created successfully"
                    )}
                } else {
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

    fun upvote(alert: AlertDomain) {

        viewModelScope.launch {
            try {
                val success = updateAlertUseCase(alert.id, alert.votes + 1)
                if (!success) {
                    // Revert optimistic update si se hizo, o mostrar error
                    _uiState.update { it.copy(errorMessage = "Failed to sync upvote with server.") }
                    // Forzar una recarga para asegurar consistencia si el update local falló
                    refreshAlerts()
                }
                // Si 'success' es true, el repositorio ya actualizó Realm, el Flow lo recogerá.
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error upvoting alert ${alert.id} on server", e)
                _uiState.update { it.copy(errorMessage = "Failed to sync upvote") }
                refreshAlerts() // Re-sincronizar con la fuente de verdad (local DB)
            }
        }
    }

    fun downvote(alert: AlertDomain) {
        // Similar a upvote
        viewModelScope.launch {
            try {
                val success = updateAlertUseCase(alert.id, alert.votes - 1)
                if (!success) {
                    _uiState.update { it.copy(errorMessage = "Failed to sync downvote with server.") }
                    refreshAlerts()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error downvoting alert ${alert.id} on server", e)
                _uiState.update { it.copy(errorMessage = "Failed to sync downvote") }
                refreshAlerts()
            }
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
                createAlertUseCase(message, restaurantId, currentUser) // Esto ya guarda en DB via repo
                Log.d("AlertsViewModel", "Alert sent successfully from draft")

                try {
                    draftAlertRepository.deleteDraftAlert(draftId)
                } catch (e: Exception) {
                    Log.e("AlertsViewModel", "Failed to delete draft, but alert was sent: $draftId", e)
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Alert sent successfully",
                    errorMessage = null
                )}
            } catch (e: Exception) {
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

    fun getLatestDraftAlert() {
        viewModelScope.launch {
            val latestDraft = draftAlertRepository.getLatestDraftAlert()

        }
    }
}

// ConnectivityUiState se mantiene igual
data class ConnectivityUiState(
    val isConnected: Boolean,
    val hasDraftAlerts: Boolean
)