package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.campusbites.domain.usecase.alert.ObserveLocalAlertsUseCase
import com.example.campusbites.domain.usecase.alert.UpdateAlertUseCase
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
        val isLoading: Boolean = true,
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
        Log.e("AlertsViewModel", "Error monitoring connectivity or drafts", e)
        emit(ConnectivityUiState(isConnected = _isNetworkAvailable.value, hasDraftAlerts = _draftAlerts.value.isNotEmpty()))
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
                    Log.d("AlertsViewModel", "No cached restaurants, fetching from network via HomeDataRepository's mechanism or directly if needed")

                } catch (e: Exception) {
                    Log.e("AlertsViewModel", "Error fetching restaurants for cache: ${e.message}", e)
                    _uiState.update { it.copy(errorMessage = "Failed to load restaurants for context") }
                }
            }
            hasCheckedRestaurantsCache = true
        }
    }

    private fun monitorDraftAlerts() {

        viewModelScope.launch {
            draftAlertRepository.getAllDraftAlerts()
                .distinctUntilChanged()
                .catch { e -> Log.e("AlertsViewModel", "Error monitoring drafts", e) }
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
                        noAlertsMessageVisible = it.alerts.isEmpty() && !it.isLoading && initialAlertFetchAttempted
                    )}
                }
                .collect { localAlerts ->
                    Log.d("AlertsViewModel", "Local alerts observed: ${localAlerts.size}")
                    val sortedAlerts = localAlerts.sortedByDescending { alert ->
                        alert.datetime
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            alerts = sortedAlerts,
                            noAlertsMessageVisible = sortedAlerts.isEmpty() && !currentState.isLoading && initialAlertFetchAttempted
                        )
                    }

                    if (initialAlertFetchAttempted && sortedAlerts.isEmpty() && !_uiState.value.isLoading) {
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
                        isLoading = false, // No hay red, no estamos cargando desde el servidor
                        noAlertsMessageVisible = state.alerts.isEmpty() // Mostrar si no hay alertas locales
                    )
                }
                Log.d("AlertsViewModel", "No network for initial fetch, relying on local DB. Alerts count: ${_uiState.value.alerts.size}")
            }
        }
    }

    fun refreshAlerts() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, noAlertsMessageVisible = false) }
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                Log.w("AlertsViewModel", "Cannot fetch alerts, user not logged in.")
                _uiState.update { it.copy(errorMessage = "User not logged in", isLoading = false) }
                initialAlertFetchAttempted = true
                if (_uiState.value.alerts.isEmpty()) {
                    _uiState.update { it.copy(noAlertsMessageVisible = true) }
                }
                return@launch
            }
            try {
                fetchRemoteAlertsUseCase() // Obtiene de red y guarda en Realm. El Flow de observeLocalAlertsUseCase actualizará la UI.
                Log.d("AlertsViewModel", "Alerts refreshed from server and saved to DB.")
                // No es necesario actualizar explícitamente `alerts` aquí, el Flow lo hará.
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error refreshing alerts: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error refreshing alerts") }
            } finally {
                initialAlertFetchAttempted = true
                // El estado de isLoading se actualiza, y noAlertsMessageVisible se basa en si la lista (que será emitida por el Flow) está vacía.
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        noAlertsMessageVisible = currentState.alerts.isEmpty() // Se reevaluará cuando el flow emita
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

            // Obtener el nombre del restaurante para el borrador, incluso si estamos offline
            val restaurantName = restaurants.value.find { it.id == restaurantId }?.name ?: run {
                Log.w("AlertsViewModel", "Restaurant name not found for ID: $restaurantId for draft. Using ID.")
                "Restaurant ID: $restaurantId" // Fallback si el nombre no está en la lista cargada
            }

            try {
                if (_isNetworkAvailable.value) { // Usar el valor de _isNetworkAvailable que se actualiza por el flow
                    val createdAlertDomain = createAlertUseCase(description, restaurantId, currentUser)
                    // El repositorio ya debería haber guardado en la DB local, el Flow la recogerá.
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Alert created successfully"
                    )}
                    Log.d("AlertsViewModel", "Alert created online: ${createdAlertDomain.id}")
                } else {
                    draftAlertRepository.saveDraftAlert(description, restaurantId, restaurantName)
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Alert saved as draft. Will be sent when you're back online."
                    )}
                    Log.d("AlertsViewModel", "Alert saved as draft.")
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error creating alert or saving draft", e)
                val errorMsg = if (_isNetworkAvailable.value) "Error creating alert" else "Error saving draft"
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: errorMsg,
                    isLoading = false
                )}
            }
        }
    }

    fun upvote(alert: AlertDomain) {
        if (!_isNetworkAvailable.value) {
            _uiState.update { it.copy(errorMessage = "Cannot upvote alert: No internet connection.") }
            return
        }
        viewModelScope.launch {
            try {

                val success = updateAlertUseCase(alert.id, alert.votes + 1)
                if (success) {
                    Log.d("AlertsViewModel", "Alert ${alert.id} upvoted successfully on server.")

                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to sync upvote with server.") }
                    refreshAlerts()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error upvoting alert ${alert.id} on server", e)
                _uiState.update { it.copy(errorMessage = "Failed to sync upvote: ${e.localizedMessage}") }
                refreshAlerts() // Re-sincronizar
            }
        }
    }

    fun downvote(alert: AlertDomain) {
        if (!_isNetworkAvailable.value) {
            _uiState.update { it.copy(errorMessage = "Cannot downvote alert: No internet connection.") }
            return
        }
        viewModelScope.launch {
            try {
                val success = updateAlertUseCase(alert.id, alert.votes - 1)
                if (success) {
                    Log.d("AlertsViewModel", "Alert ${alert.id} downvoted successfully on server.")
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to sync downvote with server.") }
                    refreshAlerts()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error downvoting alert ${alert.id} on server", e)
                _uiState.update { it.copy(errorMessage = "Failed to sync downvote: ${e.localizedMessage}") }
                refreshAlerts()
            }
        }
    }


    fun sendDraftAlert(draftId: String, message: String, restaurantId: String) {
        viewModelScope.launch {
            if (!_isNetworkAvailable.value) {
                _uiState.update { it.copy(
                    errorMessage = "Cannot send draft: No internet connection",
                    successMessage = null, // Limpiar success anterior
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
                val createdAlertDomain = createAlertUseCase(message, restaurantId, currentUser) // Esto ya guarda en DB via repo
                Log.d("AlertsViewModel", "Alert sent successfully from draft: ${createdAlertDomain.id}")

                try {
                    draftAlertRepository.deleteDraftAlert(draftId)
                    Log.d("AlertsViewModel", "Draft $draftId deleted after sending.")
                } catch (e: Exception) {
                    // Esto es un problema menor si la alerta se envió, pero hay que loggearlo.
                    Log.e("AlertsViewModel", "Failed to delete draft $draftId after sending, but alert was sent.", e)
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Alert sent successfully from draft!",
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
                    successMessage = "Draft alert deleted successfully",
                    errorMessage = null
                )}
                Log.d("AlertsViewModel", "Draft $draftId deleted by user.")
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error deleting draft alert by user: $draftId", e)
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
            try {
                val latestDraft = draftAlertRepository.getLatestDraftAlert() // Asumo que esto es suspend
                _uiState.update { it.copy(latestDraftAlert = latestDraft) }
                if (latestDraft != null) {
                    Log.d("AlertsViewModel", "Latest draft loaded: ${latestDraft.id}")
                } else {
                    Log.d("AlertsViewModel", "No latest draft found.")
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error getting latest draft alert.", e)
                _uiState.update { it.copy(latestDraftAlert = null) } // Limpiar si hay error
            }
        }
    }
}

// ConnectivityUiState se mantiene igual
data class ConnectivityUiState(
    val isConnected: Boolean,
    val hasDraftAlerts: Boolean
)