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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val draftAlertRepository: DraftAlertRepository,
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

    private val _editingDraftId = MutableStateFlow<String?>(null)
    val editingDraftId: StateFlow<String?> = _editingDraftId.asStateFlow()


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
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L), // CORREGIDO
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

    fun startEditingDraft(draft: DraftAlert) {
        _editingDraftId.value = draft.id
        Log.d("AlertsViewModel", "Started editing draft ID: ${draft.id}")
    }

    fun clearEditingDraftState() {
        if (_editingDraftId.value != null) {
            Log.d("AlertsViewModel", "Cleared editing draft state. Was ID: ${_editingDraftId.value}")
            _editingDraftId.value = null
        }
    }


    private fun checkRestaurantsCacheAndFetchIfNeeded() {
        if (hasCheckedRestaurantsCache) return
        viewModelScope.launch {
            Log.d("AlertsViewModel", "Verificando caché de restaurantes a través de homeDataRepository.nearbyRestaurantsFlow.")
            if (homeDataRepository.nearbyRestaurantsFlow.first().isEmpty() && connectivityMonitor.isNetworkAvailable.first()) {
                Log.d("AlertsViewModel", "Restaurantes cacheados vacíos y hay red. HomeDataRepository debería manejar la carga.")
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
                val currentEditingIdSnapshot = _editingDraftId.value
                withContext(Dispatchers.IO) {
                    if (currentEditingIdSnapshot != null) {
                        draftAlertRepository.deleteDraftAlert(currentEditingIdSnapshot)
                        Log.d("AlertsViewModel", "Deleted old draft $currentEditingIdSnapshot to save updated version.")
                    }
                    val newDraftId = draftAlertRepository.saveDraftAlert(description, restaurantId, restaurantName)
                    Log.d("AlertsViewModel", "Draft saved/updated with ID: $newDraftId")
                }
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = if (currentEditingIdSnapshot != null) "Draft updated successfully" else "Draft saved successfully"
                )}
                if (currentEditingIdSnapshot != null) {
                    clearEditingDraftState()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error saving/updating draft", e)
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error saving draft",
                    isLoading = false
                )}
            }
        }
    }

    fun submitOnlineAlert(description: String, restaurantId: String) {
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
                withContext(Dispatchers.IO) {
                    val createdAlertDomain = async { createAlertUseCase(message, restaurantId, currentUser) }.await()
                    Log.d("AlertsViewModel", "Alert sent successfully from draft: ${createdAlertDomain.id}.")
                    try {
                        draftAlertRepository.deleteDraftAlert(draftId)
                        Log.d("AlertsViewModel", "Draft $draftId deleted after sending.")
                    } catch (e: Exception) {
                        Log.e("AlertsViewModel", "Failed to delete draft $draftId after sending, but alert was sent.", e)
                    }
                }
                _uiState.update { it.copy(isLoading = false, successMessage = "Alert sent successfully from draft!")}
                if (_editingDraftId.value == draftId) {
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
                    draftAlertRepository.deleteDraftAlert(draftId)
                }
                _uiState.update { it.copy(isLoading = false, successMessage = "Draft alert deleted successfully") }
                Log.d("AlertsViewModel", "Draft $draftId deleted by user.")
                if (_editingDraftId.value == draftId) {
                    clearEditingDraftState()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error deleting draft alert by user: $draftId", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to delete draft: ${e.localizedMessage ?: "Unknown error"}")}
            }
        }
    }

    fun getLatestDraftAlert() {
        viewModelScope.launch {
            try {
                val latestDraft = withContext(Dispatchers.IO) { draftAlertRepository.getLatestDraftAlert() }
                _uiState.update { it.copy(latestDraftAlert = latestDraft) }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error getting latest draft alert.", e)
                _uiState.update { it.copy(latestDraftAlert = null) }
            }
        }
    }
}

data class ConnectivityUiState(
    val isConnected: Boolean,
    val hasDraftAlerts: Boolean
)