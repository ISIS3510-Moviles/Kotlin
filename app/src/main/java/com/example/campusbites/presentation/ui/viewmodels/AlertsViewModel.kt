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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val fetchRemoteAlertsUseCase: GetAlertsUseCase,
    private val observeLocalAlertsUseCase: ObserveLocalAlertsUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase,
    private val authRepository: AuthRepository,
    private val draftAlertRepository: DraftAlertRepository, // Ahora es la implementación de Room
    private val connectivityMonitor: ConnectivityMonitor,
    private val notificationService: AlertNotificationService,
    private val homeDataRepository: HomeDataRepository
) : ViewModel() {

    val restaurants: StateFlow<List<RestaurantDomain>> = homeDataRepository.nearbyRestaurantsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
            initialValue = emptyList()
        )

    private val _draftAlerts = MutableStateFlow<List<DraftAlert>>(emptyList())
    val draftAlerts = _draftAlerts.asStateFlow()

    private val _editingDraft = MutableStateFlow<DraftAlert?>(null) // Cambiado a DraftAlert?
    val editingDraftId: StateFlow<String?> = _editingDraft.map { it?.id }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )


    data class AlertsUiState(
        val alerts: List<AlertDomain> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        // val latestDraftAlert: DraftAlert? = null, // Se puede obtener de _draftAlerts si es necesario
        val noAlertsMessageVisible: Boolean = false
    )

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)

    val connectivityState = combine(
        connectivityMonitor.isNetworkAvailable,
        draftAlertRepository.getAllDraftAlerts() // Sigue observando los borradores
    ) { isConnected, drafts ->
        _isNetworkAvailable.value = isConnected
        _draftAlerts.value = drafts // Actualiza la lista de borradores
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
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue = ConnectivityUiState(isConnected = false, hasDraftAlerts = false)
    )

    private var hasCheckedRestaurantsCache = false
    private var initialAlertFetchAttempted = false

    init {
        observeLocalAlerts()
        checkRestaurantsCacheAndFetchIfNeeded()
        triggerInitialAlertFetch()
        // monitorDraftAlerts() // Ya se hace en connectivityState
    }

    fun startEditingDraft(draft: DraftAlert) {
        _editingDraft.value = draft
        Log.d("AlertsViewModel", "Started editing draft: $draft")
    }

    fun clearEditingDraftState() {
        if (_editingDraft.value != null) {
            Log.d("AlertsViewModel", "Cleared editing draft state. Was: ${_editingDraft.value}")
            _editingDraft.value = null
        }
    }

    private fun checkRestaurantsCacheAndFetchIfNeeded() {
        // ... (sin cambios)
        if (hasCheckedRestaurantsCache) return
        viewModelScope.launch {
            Log.d("AlertsViewModel", "Verificando caché de restaurantes a través de homeDataRepository.nearbyRestaurantsFlow.")
            if (homeDataRepository.nearbyRestaurantsFlow.first().isEmpty() && connectivityMonitor.isNetworkAvailable.first()) {
                Log.d("AlertsViewModel", "Restaurantes cacheados vacíos y hay red. HomeDataRepository debería manejar la carga.")
            }
            hasCheckedRestaurantsCache = true
        }
    }

    private fun observeLocalAlerts() {
        // ... (sin cambios en la lógica de observación de alertas principales de Realm)
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
        // ... (sin cambios)
        viewModelScope.launch {
            if (connectivityMonitor.isNetworkAvailable.first()) {
                Log.d("AlertsViewModel", "Network available for initial fetch. Calling refreshAlerts.")
                refreshAlerts()
            } else {
                initialAlertFetchAttempted = true
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        noAlertsMessageVisible = state.alerts.isEmpty()
                    )
                }
                Log.d("AlertsViewModel", "No network for initial fetch, relying on local DB. Alerts count: ${_uiState.value.alerts.size}. isLoading set to false.")
            }
        }
    }

    fun refreshAlerts() {
        // ... (sin cambios en la lógica de refresco de alertas principales de Realm)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noAlertsMessageVisible = false) }
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
                withContext(Dispatchers.IO) { fetchRemoteAlertsUseCase() }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error refreshing alerts: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error refreshing alerts") }
            } finally {
                initialAlertFetchAttempted = true
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        noAlertsMessageVisible = currentState.alerts.isEmpty()
                    )
                }
            }
        }
    }

    fun refreshAlertsManually() { refreshAlerts() }

    fun createOrUpdateDraftAlert(description: String, restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(errorMessage = "User not available to save draft", isLoading = false) }
                return@launch
            }
            val restaurantName = restaurants.value.find { it.id == restaurantId }?.name ?: "Restaurant ID: $restaurantId"

            try {
                val draftToSave = _editingDraft.value?.copy( // Si estamos editando, usamos el ID existente
                    message = description,
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    createdAt = _editingDraft.value?.createdAt ?: System.currentTimeMillis() // Mantener createdAt si se edita
                ) ?: DraftAlert( // Si es nuevo, el ID será generado por Room (pasamos "0" o un placeholder)
                    id = "0", // Room lo ignorará y autogenerará si es 0L en la entidad
                    message = description,
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    createdAt = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    if (_editingDraft.value != null) {
                        draftAlertRepository.updateDraftAlert(draftToSave)
                        Log.d("AlertsViewModel", "Draft updated in Room: ${draftToSave.id}")
                    } else {
                        val newId = draftAlertRepository.saveDraftAlert(description, restaurantId, restaurantName)
                        Log.d("AlertsViewModel", "Draft saved to Room with new ID: $newId")
                    }
                }
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = if (_editingDraft.value != null) "Draft updated successfully" else "Draft saved successfully"
                )}
                clearEditingDraftState() // Limpiar estado de edición después de guardar/actualizar
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error saving/updating draft to Room", e)
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error saving draft",
                    isLoading = false
                )}
            }
        }
    }


    fun submitOnlineAlert(description: String, restaurantId: String) {
        // ... (sin cambios)
        viewModelScope.launch {
            if (!_isNetworkAvailable.value) {
                _uiState.update { it.copy(errorMessage = "Cannot create alert: No internet connection.", isLoading = false)}
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(errorMessage = "User not available to create alert", isLoading = false) }
                return@launch
            }
            try {
                val createdAlertDomain = withContext(Dispatchers.IO) {
                    createAlertUseCase(description, restaurantId, currentUser)
                }
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Alert created successfully"
                )}
                Log.d("AlertsViewModel", "Alert created online: ${createdAlertDomain.id}")
                clearEditingDraftState()
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error creating alert online", e)
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error creating alert",
                    isLoading = false
                )}
            }
        }
    }

    fun upvote(alert: AlertDomain) {
        // ... (sin cambios)
        if (!_isNetworkAvailable.value) {
            _uiState.update { it.copy(errorMessage = "Cannot upvote alert: No internet connection.") }
            return
        }
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    updateAlertUseCase(alert.id, alert.votes + 1)
                }
                if (success) {
                    Log.d("AlertsViewModel", "Alert ${alert.id} upvoted successfully on server.")
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to sync upvote with server.") }
                    refreshAlerts()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error upvoting alert ${alert.id} on server", e)
                _uiState.update { it.copy(errorMessage = "Failed to sync upvote: ${e.localizedMessage}") }
                refreshAlerts()
            }
        }
    }

    fun downvote(alert: AlertDomain) {
        // ... (sin cambios)
        if (!_isNetworkAvailable.value) {
            _uiState.update { it.copy(errorMessage = "Cannot downvote alert: No internet connection.") }
            return
        }
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    updateAlertUseCase(alert.id, alert.votes - 1)
                }
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
                _uiState.update { it.copy(errorMessage = "Cannot send draft: No internet connection", isLoading = false ) }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(errorMessage = "User not available to send draft alert",isLoading = false) }
                return@launch
            }
            try {
                // Crear la alerta online
                val createdAlertDomain = withContext(Dispatchers.IO) {
                    async { createAlertUseCase(message, restaurantId, currentUser) }.await()
                }
                Log.d("AlertsViewModel", "Alert sent successfully from draft: ${createdAlertDomain.id}.")

                // Eliminar el borrador de Room
                try {
                    withContext(Dispatchers.IO) {
                        draftAlertRepository.deleteDraftAlert(draftId)
                    }
                    Log.d("AlertsViewModel", "Draft $draftId deleted from Room after sending.")
                } catch (e: Exception) {
                    Log.e("AlertsViewModel", "Failed to delete draft $draftId from Room after sending, but alert was sent.", e)
                    // No actualices el mensaje de error aquí si la alerta se envió,
                    // pero registra el problema.
                }

                _uiState.update { it.copy(isLoading = false, successMessage = "Alert sent successfully from draft!")}
                if (_editingDraft.value?.id == draftId) {
                    clearEditingDraftState()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error sending draft alert: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error sending draft alert", isLoading = false)}
            }
        }
    }


    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun deleteDraftAlert(draftId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    draftAlertRepository.deleteDraftAlert(draftId) // Usa el repositorio (Room)
                }
                _uiState.update { it.copy(isLoading = false, successMessage = "Draft alert deleted successfully") }
                Log.d("AlertsViewModel", "Draft $draftId deleted by user from Room.")
                if (_editingDraft.value?.id == draftId) { // Compara con el ID del borrador en edición
                    clearEditingDraftState()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error deleting draft alert by user from Room: $draftId", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to delete draft: ${e.localizedMessage ?: "Unknown error"}")}
            }
        }
    }

    // getLatestDraftAlert ya no es necesario en uiState si se actualiza _draftAlerts
    // y la UI puede derivar el último de esa lista si es necesario.
}

// ConnectivityUiState (sin cambios)
data class ConnectivityUiState(
    val isConnected: Boolean,
    val hasDraftAlerts: Boolean
)