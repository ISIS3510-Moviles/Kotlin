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
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase // Puede renombrarse a FetchRemoteAlertsUseCase
import com.example.campusbites.domain.usecase.alert.ObserveLocalAlertsUseCase
import com.example.campusbites.domain.usecase.alert.UpdateAlertUseCase
// Eliminado getRestaurantsUseCase si no se usa directamente para popular 'restaurants' stateflow
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
    private val homeDataRepository: HomeDataRepository // Usado para restaurants
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
        _draftAlerts.value = drafts // Asegurarse de que _draftAlerts también se actualice aquí
        if (isConnected && drafts.isNotEmpty() && notificationService.hasNotificationPermission()) {
            notificationService.showDraftAlertAvailableNotification(drafts.size)
        }
        ConnectivityUiState(
            isConnected = isConnected,
            hasDraftAlerts = drafts.isNotEmpty()
        )
    }.catch { e ->
        Log.e("AlertsViewModel", "Error monitoring connectivity or drafts", e)
        // Emitir estado actual en caso de error para no romper el flow
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
        checkRestaurantsCacheAndFetchIfNeeded() // Esto principalmente asegura que 'restaurants' esté poblado
        triggerInitialAlertFetch()
        monitorDraftAlerts()
    }

    private fun checkRestaurantsCacheAndFetchIfNeeded() {
        if (hasCheckedRestaurantsCache) return
        viewModelScope.launch {
            // homeDataRepository.nearbyRestaurantsFlow se encarga de la lógica de caché y fetch
            // Esta función solo se asegura que el flujo se observe si es necesario para poblar 'restaurants'
            // para el dropdown en AlertCreateScreen. La lógica de fetch de restaurantes ya está en HomeDataRepository.
            Log.d("AlertsViewModel", "Verificando caché de restaurantes a través de homeDataRepository.nearbyRestaurantsFlow.")
            // Si nearbyRestaurantsFlow está vacío y hay red, HomeDataRepository debería intentar cargar.
            // No necesitamos duplicar la lógica de getRestaurantsUseCase() aquí si HomeDataRepository lo maneja.
            if (homeDataRepository.nearbyRestaurantsFlow.first().isEmpty() && connectivityMonitor.isNetworkAvailable.first()) {
                Log.d("AlertsViewModel", "Restaurantes cacheados vacíos y hay red. HomeDataRepository debería manejar la carga.")
                // Si HomeDataRepository no lo hace automáticamente al ser observado, aquí se podría forzar:
                // homeDataRepository.refreshNearbyRestaurantsIfNeeded() o similar (método hipotético)
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
                        alert.datetime // Ordenar por fecha descendente
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            alerts = sortedAlerts,
                            //isLoading = false, // isLoading se maneja en refreshAlerts/triggerInitialAlertFetch
                            noAlertsMessageVisible = sortedAlerts.isEmpty() && !currentState.isLoading && initialAlertFetchAttempted
                        )
                    }

                    // Re-evaluar noAlertsMessageVisible después de que initialAlertFetchAttempted sea true y no estemos cargando
                    if (initialAlertFetchAttempted && sortedAlerts.isEmpty() && !_uiState.value.isLoading) {
                        _uiState.update { it.copy(noAlertsMessageVisible = true) }
                    }
                }
        }
    }

    private fun triggerInitialAlertFetch() {
        viewModelScope.launch { // Corre en Main por defecto
            if (connectivityMonitor.isNetworkAvailable.first()) {
                Log.d("AlertsViewModel", "Network available for initial fetch. Calling refreshAlerts.")
                refreshAlerts() // refreshAlerts manejará isLoading
            } else {
                // No hay red, no estamos cargando desde el servidor
                initialAlertFetchAttempted = true // Marcar que el intento (fallido por falta de red) ha ocurrido
                _uiState.update { state ->
                    state.copy(
                        isLoading = false, // No estamos cargando activamente desde la red
                        noAlertsMessageVisible = state.alerts.isEmpty() // Mostrar si no hay alertas locales
                    )
                }
                Log.d("AlertsViewModel", "No network for initial fetch, relying on local DB. Alerts count: ${_uiState.value.alerts.size}. isLoading set to false.")
            }
        }
    }

    fun refreshAlerts() {
        // Estrategia 3: IO para fetch, Main para UI updates.
        // Estrategia 1: fetchRemoteAlertsUseCase se ejecuta en un contexto IO.
        viewModelScope.launch { // Corre en Dispatchers.Main (por defecto para viewModelScope)
            _uiState.update { it.copy(isLoading = true, errorMessage = null, noAlertsMessageVisible = false) }
            Log.d("ThreadingStrategy", "[Strategy 3] RefreshAlerts: UI update (isLoading=true) on ${Thread.currentThread().name}")

            val currentUser = authRepository.currentUser.first() // Suspend, se reanuda en Main

            if (currentUser == null) {
                Log.w("AlertsViewModel", "Cannot fetch alerts, user not logged in.")
                _uiState.update { it.copy(errorMessage = "User not logged in", isLoading = false) }
                initialAlertFetchAttempted = true
                if (_uiState.value.alerts.isEmpty()) { // Re-check after isLoading=false
                    _uiState.update { it.copy(noAlertsMessageVisible = true) }
                }
                Log.d("ThreadingStrategy", "[Strategy 3] RefreshAlerts: User null, UI update on ${Thread.currentThread().name}")
                return@launch
            }

            try {
                // Cambio explícito a Dispatchers.IO para la operación de red/DB
                withContext(Dispatchers.IO) {
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] RefreshAlerts: Calling fetchRemoteAlertsUseCase on ${Thread.currentThread().name}")
                    fetchRemoteAlertsUseCase() // Esta función es suspend y el repo/api service maneja su IO
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] RefreshAlerts: fetchRemoteAlertsUseCase completed on ${Thread.currentThread().name}")
                }
                // De vuelta en Dispatchers.Main automáticamente después de withContext
                Log.d("ThreadingStrategy", "[Strategy 3] RefreshAlerts: Back to Main for final UI logic on ${Thread.currentThread().name}")
                // El Flow de observeLocalAlertsUseCase actualizará la UI con los nuevos datos.
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error refreshing alerts: ${e.message}", e)
                // Estamos en Main context aquí
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error refreshing alerts") }
                Log.d("ThreadingStrategy", "[Strategy 3] RefreshAlerts: Exception, UI update on ${Thread.currentThread().name}")
            } finally {
                initialAlertFetchAttempted = true
                _uiState.update { currentState -> // Se ejecuta en Main
                    currentState.copy(
                        isLoading = false,
                        noAlertsMessageVisible = currentState.alerts.isEmpty() // Reevaluar después de que el flow emita
                    )
                }
                Log.d("ThreadingStrategy", "[Strategy 3] RefreshAlerts: Finally block, UI update (isLoading=false) on ${Thread.currentThread().name}")
            }
        }
    }

    fun refreshAlertsManually() {
        refreshAlerts()
    }

    fun createAlert(description: String, restaurantId: String) {
        // Estrategia 3: IO para crear/guardar borrador, Main para UI.
        // Estrategia 2: Múltiples corrutinas anidadas en IO (usando async para la creación).
        // Estrategia 1: createAlertUseCase y saveDraftAlert se ejecutan en un contexto IO.
        viewModelScope.launch { // Corre en Dispatchers.Main
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            Log.d("ThreadingStrategy", "[Strategy 3] CreateAlert: Start, UI update (isLoading=true) on ${Thread.currentThread().name}")

            val currentUser = authRepository.currentUser.first() // Suspend, reanuda en Main
            if (currentUser == null) {
                _uiState.update { it.copy(errorMessage = "User not available to create alert", isLoading = false) }
                Log.w("ThreadingStrategy", "[Strategy 3] CreateAlert: User null, UI update on ${Thread.currentThread().name}")
                return@launch
            }

            val restaurantName = restaurants.value.find { it.id == restaurantId }?.name ?: run {
                Log.w("AlertsViewModel", "Restaurant name not found for ID: $restaurantId for draft. Using ID.")
                "Restaurant ID: $restaurantId"
            }

            try {
                if (_isNetworkAvailable.value) { // Online
                    // Estrategia 3: Parte IO
                    val createdAlertResult = withContext(Dispatchers.IO) {
                        Log.d("ThreadingStrategy", "[Strategy 3 & 1] CreateAlert (Online): Switched to IO on ${Thread.currentThread().name}")

                        // Estrategia 2: Corrutina anidada (async) para la creación real.
                        // Esto es un poco para demostración; para una sola llamada, no es estrictamente necesario anidar un async.
                        // Sería más útil si hubiera múltiples pasos IO independientes que pudieran correr en paralelo.
                        async { // Hereda Dispatchers.IO del withContext externo
                            Log.d("ThreadingStrategy", "[Strategy 2] CreateAlert (Online): Inner async for createAlertUseCase started on ${Thread.currentThread().name}")
                            val alert = createAlertUseCase(description, restaurantId, currentUser)
                            Log.d("ThreadingStrategy", "[Strategy 2] CreateAlert (Online): Inner async for createAlertUseCase completed on ${Thread.currentThread().name}")
                            alert // Retorna la alerta creada
                        }.await() // Espera el resultado de la corrutina anidada
                    }

                    // Estrategia 3: De vuelta a Main para UI update
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Alert created successfully"
                        )
                    }
                    Log.d("ThreadingStrategy", "[Strategy 3] CreateAlert (Online): Back to Main, UI update (success) on ${Thread.currentThread().name}")
                    Log.d("AlertsViewModel", "Alert created online: ${createdAlertResult.id}")

                } else { // Offline - Guardar borrador
                    // Estrategia 3: Parte IO para guardar borrador
                    withContext(Dispatchers.IO) {
                        Log.d("ThreadingStrategy", "[Strategy 1 & 3] CreateAlert (Offline): Saving draft on IO thread ${Thread.currentThread().name}")
                        draftAlertRepository.saveDraftAlert(description, restaurantId, restaurantName)
                        Log.d("ThreadingStrategy", "[Strategy 1 & 3] CreateAlert (Offline): Draft saved on IO thread ${Thread.currentThread().name}")
                    }
                    // Estrategia 3: De vuelta a Main
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Alert saved as draft. Will be sent when you're back online."
                        )
                    }
                    Log.d("ThreadingStrategy", "[Strategy 3] CreateAlert (Offline): Back to Main, UI update (draft saved) on ${Thread.currentThread().name}")
                    Log.d("AlertsViewModel", "Alert saved as draft.")
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error creating alert or saving draft", e)
                val errorMsg = if (_isNetworkAvailable.value) "Error creating alert" else "Error saving draft"
                // Estrategia 3: De vuelta a Main para UI update en caso de error
                _uiState.update {
                    it.copy(
                        errorMessage = e.localizedMessage ?: errorMsg,
                        isLoading = false
                    )
                }
                Log.d("ThreadingStrategy", "[Strategy 3] CreateAlert: Exception, UI update on ${Thread.currentThread().name}")
            }
        }
    }


    fun upvote(alert: AlertDomain) {
        // Estrategia 3: IO para update, Main para UI.
        // Estrategia 1: updateAlertUseCase se ejecuta en un contexto IO.
        if (!_isNetworkAvailable.value) {
            _uiState.update { it.copy(errorMessage = "Cannot upvote alert: No internet connection.") }
            return
        }
        viewModelScope.launch { // Dispatchers.Main por defecto
            Log.d("ThreadingStrategy", "[Strategy 3] Upvote: Started on ${Thread.currentThread().name}")
            try {
                val success = withContext(Dispatchers.IO) { // Estrategia 3: Parte IO
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] Upvote: Calling updateAlertUseCase on IO thread ${Thread.currentThread().name}")
                    val result = updateAlertUseCase(alert.id, alert.votes + 1)
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] Upvote: updateAlertUseCase completed on IO thread ${Thread.currentThread().name}")
                    result
                }

                // Estrategia 3: De vuelta a Main
                if (success) {
                    Log.d("ThreadingStrategy", "[Strategy 3] Upvote: Success, back on Main ${Thread.currentThread().name}")
                    Log.d("AlertsViewModel", "Alert ${alert.id} upvoted successfully on server.")
                    // La UI se actualizará por el Flow de observeLocalAlertsUseCase al cambiar la DB
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to sync upvote with server.") }
                    Log.d("ThreadingStrategy", "[Strategy 3] Upvote: Failed, UI update on Main ${Thread.currentThread().name}")
                    refreshAlerts() // Esto lanzará su propia lógica de threading para resincronizar
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error upvoting alert ${alert.id} on server", e)
                // Estrategia 3: De vuelta a Main para UI update en error
                _uiState.update { it.copy(errorMessage = "Failed to sync upvote: ${e.localizedMessage}") }
                Log.d("ThreadingStrategy", "[Strategy 3] Upvote: Exception, UI update on Main ${Thread.currentThread().name}")
                refreshAlerts()
            }
        }
    }

    fun downvote(alert: AlertDomain) {
        // Estrategia 3: IO para update, Main para UI.
        // Estrategia 1: updateAlertUseCase se ejecuta en un contexto IO.
        if (!_isNetworkAvailable.value) {
            _uiState.update { it.copy(errorMessage = "Cannot downvote alert: No internet connection.") }
            return
        }
        viewModelScope.launch { // Dispatchers.Main por defecto
            Log.d("ThreadingStrategy", "[Strategy 3] Downvote: Started on ${Thread.currentThread().name}")
            try {
                val success = withContext(Dispatchers.IO) { // Estrategia 3: Parte IO
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] Downvote: Calling updateAlertUseCase on IO thread ${Thread.currentThread().name}")
                    val result = updateAlertUseCase(alert.id, alert.votes - 1)
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] Downvote: updateAlertUseCase completed on IO thread ${Thread.currentThread().name}")
                    result
                }

                // Estrategia 3: De vuelta a Main
                if (success) {
                    Log.d("ThreadingStrategy", "[Strategy 3] Downvote: Success, back on Main ${Thread.currentThread().name}")
                    Log.d("AlertsViewModel", "Alert ${alert.id} downvoted successfully on server.")
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to sync downvote with server.") }
                    Log.d("ThreadingStrategy", "[Strategy 3] Downvote: Failed, UI update on Main ${Thread.currentThread().name}")
                    refreshAlerts()
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error downvoting alert ${alert.id} on server", e)
                _uiState.update { it.copy(errorMessage = "Failed to sync downvote: ${e.localizedMessage}") }
                Log.d("ThreadingStrategy", "[Strategy 3] Downvote: Exception, UI update on Main ${Thread.currentThread().name}")
                refreshAlerts()
            }
        }
    }


    fun sendDraftAlert(draftId: String, message: String, restaurantId: String) {
        // Implementación similar a createAlert (online) en términos de threading
        // Estrategia 3: IO para enviar, Main para UI.
        // Estrategia 2: Múltiples corrutinas IO (async para crear, luego IO para borrar draft).
        // Estrategia 1: createAlertUseCase y deleteDraftAlert en contexto IO.
        viewModelScope.launch { // Main
            if (!_isNetworkAvailable.value) {
                _uiState.update { it.copy(
                    errorMessage = "Cannot send draft: No internet connection",
                    successMessage = null,
                    isLoading = false
                ) }
                Log.w("ThreadingStrategy", "[Strategy 3] SendDraftAlert: No network, UI update on ${Thread.currentThread().name}")
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            Log.d("ThreadingStrategy", "[Strategy 3] SendDraftAlert: Start, UI update (isLoading=true) on ${Thread.currentThread().name}")

            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { it.copy(
                    errorMessage = "User not available to send draft alert",
                    isLoading = false,
                    successMessage = null
                ) }
                Log.w("ThreadingStrategy", "[Strategy 3] SendDraftAlert: User null, UI update on ${Thread.currentThread().name}")
                return@launch
            }

            try {
                // Estrategia 3: Parte IO
                withContext(Dispatchers.IO) {
                    Log.d("ThreadingStrategy", "[Strategy 3] SendDraftAlert: Switched to IO on ${Thread.currentThread().name}")

                    // Estrategia 2: Primera operación IO (crear alerta)
                    val createdAlertDomain = async { // Hereda Dispatchers.IO
                        Log.d("ThreadingStrategy", "[Strategy 2 & 1] SendDraftAlert: Inner async for createAlertUseCase started on ${Thread.currentThread().name}")
                        val alert = createAlertUseCase(message, restaurantId, currentUser)
                        Log.d("ThreadingStrategy", "[Strategy 2 & 1] SendDraftAlert: Inner async for createAlertUseCase completed on ${Thread.currentThread().name}")
                        alert
                    }.await()
                    Log.d("AlertsViewModel", "Alert sent successfully from draft: ${createdAlertDomain.id}")

                    // Estrategia 2: Segunda operación IO (borrar borrador)
                    // No necesita ser 'async' si es secuencial a la anterior.
                    Log.d("ThreadingStrategy", "[Strategy 2 & 1] SendDraftAlert: Deleting draft $draftId on ${Thread.currentThread().name}")
                    try {
                        draftAlertRepository.deleteDraftAlert(draftId)
                        Log.d("AlertsViewModel", "Draft $draftId deleted after sending.")
                    } catch (e: Exception) {
                        Log.e("AlertsViewModel", "Failed to delete draft $draftId after sending, but alert was sent.", e)
                        // Considerar si se debe propagar este error o solo loguearlo.
                    }
                    Log.d("ThreadingStrategy", "[Strategy 2 & 1] SendDraftAlert: Draft deletion process completed on ${Thread.currentThread().name}")
                }

                // Estrategia 3: De vuelta a Main
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Alert sent successfully from draft!",
                    errorMessage = null
                )}
                Log.d("ThreadingStrategy", "[Strategy 3] SendDraftAlert: Back to Main, UI update (success) on ${Thread.currentThread().name}")

            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error sending draft alert: ${e.message}", e)
                // Estrategia 3: De vuelta a Main
                _uiState.update { it.copy(
                    errorMessage = e.localizedMessage ?: "Error sending draft alert",
                    isLoading = false,
                    successMessage = null
                )}
                Log.d("ThreadingStrategy", "[Strategy 3] SendDraftAlert: Exception, UI update on ${Thread.currentThread().name}")
            }
        }
    }


    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun deleteDraftAlert(draftId: String) {
        // Estrategia 3: IO para borrar, Main para UI.
        // Estrategia 1: deleteDraftAlert en contexto IO.
        viewModelScope.launch { // Main
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            Log.d("ThreadingStrategy", "[Strategy 3] DeleteDraftAlert: Start, UI update (isLoading=true) on ${Thread.currentThread().name}")
            try {
                // Estrategia 3: Parte IO
                withContext(Dispatchers.IO) {
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] DeleteDraftAlert: Deleting draft $draftId on IO thread ${Thread.currentThread().name}")
                    draftAlertRepository.deleteDraftAlert(draftId)
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] DeleteDraftAlert: Draft deleted on IO thread ${Thread.currentThread().name}")
                }

                // Estrategia 3: De vuelta a Main
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Draft alert deleted successfully",
                    errorMessage = null
                )}
                Log.d("ThreadingStrategy", "[Strategy 3] DeleteDraftAlert: Back to Main, UI update (success) on ${Thread.currentThread().name}")
                Log.d("AlertsViewModel", "Draft $draftId deleted by user.")
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error deleting draft alert by user: $draftId", e)
                // Estrategia 3: De vuelta a Main
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete draft: ${e.localizedMessage ?: "Unknown error"}",
                    successMessage = null
                )}
                Log.d("ThreadingStrategy", "[Strategy 3] DeleteDraftAlert: Exception, UI update on ${Thread.currentThread().name}")
            }
        }
    }

    fun getLatestDraftAlert() {
        // Estrategia 3: IO para obtener, Main para UI.
        // Estrategia 1: getLatestDraftAlert en contexto IO.
        viewModelScope.launch { // Main
            Log.d("ThreadingStrategy", "[Strategy 3] GetLatestDraftAlert: Started on ${Thread.currentThread().name}")
            try {
                // Estrategia 3: Parte IO
                val latestDraft = withContext(Dispatchers.IO) {
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] GetLatestDraftAlert: Fetching latest draft on IO thread ${Thread.currentThread().name}")
                    val draft = draftAlertRepository.getLatestDraftAlert() // Asumo que esto es suspend
                    Log.d("ThreadingStrategy", "[Strategy 1 & 3] GetLatestDraftAlert: Fetched draft on IO thread ${Thread.currentThread().name}")
                    draft
                }

                // Estrategia 3: De vuelta a Main
                _uiState.update { it.copy(latestDraftAlert = latestDraft) }
                if (latestDraft != null) {
                    Log.d("ThreadingStrategy", "[Strategy 3] GetLatestDraftAlert: Back to Main, latest draft loaded: ${latestDraft.id} on ${Thread.currentThread().name}")
                } else {
                    Log.d("ThreadingStrategy", "[Strategy 3] GetLatestDraftAlert: Back to Main, no latest draft found on ${Thread.currentThread().name}")
                }
            } catch (e: Exception) {
                Log.e("AlertsViewModel", "Error getting latest draft alert.", e)
                // Estrategia 3: De vuelta a Main
                _uiState.update { it.copy(latestDraftAlert = null) }
                Log.d("ThreadingStrategy", "[Strategy 3] GetLatestDraftAlert: Exception, UI update on ${Thread.currentThread().name}")
            }
        }
    }
}

// ConnectivityUiState se mantiene igual
data class ConnectivityUiState(
    val isConnected: Boolean,
    val hasDraftAlerts: Boolean
)