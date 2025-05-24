package com.example.campusbites.presentation.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.cache.RestaurantLruCache
import com.example.campusbites.data.dto.UpdateRestaurantDTO
import com.example.campusbites.data.local.LocalRestaurantDataSource
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.restaurant.UpdateRestaurantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class VendorViewModel @Inject constructor(
    application: Application,
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase,
    private val updateRestaurantUseCase: UpdateRestaurantUseCase,
    private val restaurantLruCache: RestaurantLruCache,
    private val homeDataRepository: HomeDataRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val localRestaurantDataSource: LocalRestaurantDataSource
) : AndroidViewModel(application) {

    private val _restaurant = MutableStateFlow<RestaurantDomain?>(null)
    val restaurant: StateFlow<RestaurantDomain?> = _restaurant.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isNetworkAvailable: StateFlow<Boolean> = connectivityMonitor.isNetworkAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Estados para los campos editables
    val editableName = MutableStateFlow("")
    val editableDescription = MutableStateFlow("")
    val editableAddress = MutableStateFlow("")
    val editablePhone = MutableStateFlow("")
    val editableEmail = MutableStateFlow("")
    val editableOpeningTime = MutableStateFlow("")
    val editableClosingTime = MutableStateFlow("")
    val editableOpensWeekends = MutableStateFlow(false)
    val editableOpensHolidays = MutableStateFlow(false)
    val editableIsActive = MutableStateFlow(false)

    // Estados para el proceso de guardado
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    private val _saveErrorMessage = MutableStateFlow<String?>(null)
    val saveErrorMessage: StateFlow<String?> = _saveErrorMessage.asStateFlow()

    private var syncJob: Job? = null // Para controlar la corrutina de sincronización

    // Formateador para mostrar la hora en la UI (HH:mm)
    private val uiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    // Formateador para parsear el formato ISO 8601 completo (ej. 2024-03-08T10:00:00.000Z)
    private val isoDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME // Para OffsetDateTime

    init {
        viewModelScope.launch {
            _restaurant.collect { restaurant ->
                restaurant?.let {
                    editableName.value = it.name
                    editableDescription.value = it.description
                    editableAddress.value = it.address
                    editablePhone.value = it.phone
                    editableEmail.value = it.email
                    // ¡Aquí es donde cambiamos la inicialización para la UI!
                    editableOpeningTime.value = formatIsoToUiTime(it.openingTime)
                    editableClosingTime.value = formatIsoToUiTime(it.closingTime)
                    editableOpensWeekends.value = it.opensWeekends
                    editableOpensHolidays.value = it.opensHolidays
                    editableIsActive.value = it.isActive
                }
            }
        }

        // Observar la conectividad para iniciar la sincronización cuando la red esté disponible
        viewModelScope.launch {
            isNetworkAvailable.collect { isConnected ->
                if (isConnected) {
                    Log.d("VendorViewModel", "Network available. Attempting to sync pending updates.")
                    syncPendingRestaurantUpdates()
                } else {
                    Log.d("VendorViewModel", "Network lost. Pausing sync attempts.")
                    syncJob?.cancel() // Cancelar cualquier intento de sincronización en curso
                }
            }
        }
    }

    // Función de utilidad para formatear la cadena ISO 8601 a HH:mm para la UI
    private fun formatIsoToUiTime(isoTime: String): String {
        return try {
            // Parsear como OffsetDateTime para manejar la 'Z' (zona horaria UTC)
            val dateTime = OffsetDateTime.parse(isoTime, isoDateTimeFormatter)
            dateTime.toLocalTime().format(uiTimeFormatter)
        } catch (e: DateTimeParseException) {
            Log.e("VendorViewModel", "Error parsing ISO time for UI: $isoTime", e)
            "" // Devolver cadena vacía si hay un error de parseo
        }
    }

    // Función de utilidad para convertir HH:mm de la UI a un formato ISO 8601 completo para el backend
    private fun formatUiTimeToIso(uiTime: String): String {
        return try {
            val localTime = LocalTime.parse(uiTime, uiTimeFormatter)
            // Combinar con la fecha actual y zona horaria UTC para formar un ISO 8601 completo
            OffsetDateTime.now().withHour(localTime.hour).withMinute(localTime.minute).withSecond(0).withNano(0)
                .format(isoDateTimeFormatter)
        } catch (e: DateTimeParseException) {
            Log.e("VendorViewModel", "Error parsing UI time to ISO: $uiTime", e)
            ""
        }
    }

    fun loadVendorRestaurant(restaurantId: String) {
        if (restaurantId.isBlank()) {
            _errorMessage.value = "Vendor restaurant ID is missing."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // 1. Intentar desde LRU Cache
            var cachedRestaurant = restaurantLruCache.get(restaurantId)
            if (cachedRestaurant != null) {
                _restaurant.value = cachedRestaurant
                _isLoading.value = false
                Log.d("VendorViewModel", "Restaurant $restaurantId loaded from LRU Cache.")
                // Si hay red, intentar actualizar en segundo plano
                if (isNetworkAvailable.value) {
                    fetchRestaurantFromServer(restaurantId, true)
                }
                return@launch
            }

            // 2. Intentar desde HomeDataRepository (DataStore) si no está en LRU
            cachedRestaurant = homeDataRepository.nearbyRestaurantsFlow.first().find { it.id == restaurantId }
            if (cachedRestaurant != null) {
                _restaurant.value = cachedRestaurant
                restaurantLruCache.put(restaurantId, cachedRestaurant) // Añadir a LRU
                _isLoading.value = false
                Log.d("VendorViewModel", "Restaurant $restaurantId loaded from HomeDataRepository.")
                if (isNetworkAvailable.value) {
                    fetchRestaurantFromServer(restaurantId, true)
                }
                return@launch
            }

            // 3. Si no está en caché, intentar desde la red si hay conexión
            if (isNetworkAvailable.value) {
                fetchRestaurantFromServer(restaurantId, false)
            } else {
                _errorMessage.value = "No internet connection and restaurant details not found in cache."
                _isLoading.value = false
                Log.w("VendorViewModel", "Offline and no cached data for restaurant $restaurantId.")
            }
        }
    }

    private suspend fun fetchRestaurantFromServer(restaurantId: String, isBackgroundUpdate: Boolean) {
        if (!isBackgroundUpdate) {
            _isLoading.value = true
        }
        try {
            val fetchedRestaurant = getRestaurantByIdUseCase(restaurantId)
            if (fetchedRestaurant != null) {
                _restaurant.value = fetchedRestaurant
                restaurantLruCache.put(restaurantId, fetchedRestaurant)
                val currentNearby = homeDataRepository.nearbyRestaurantsFlow.first().toMutableList()
                val index = currentNearby.indexOfFirst { it.id == restaurantId }
                if (index != -1) {
                    currentNearby[index] = fetchedRestaurant
                } else {
                    currentNearby.add(fetchedRestaurant)
                }
                homeDataRepository.saveNearbyRestaurants(currentNearby)
                Log.d("VendorViewModel", "Restaurant $restaurantId fetched/updated from server.")
            } else {
                if (!isBackgroundUpdate) {
                    _errorMessage.value = "Restaurant not found on server."
                }
                Log.w("VendorViewModel", "Restaurant $restaurantId not found on server during fetch.")
            }
        } catch (e: Exception) {
            Log.e("VendorViewModel", "Error fetching restaurant $restaurantId from server: ${e.message}", e)
            if (!isBackgroundUpdate && _restaurant.value == null) {
                _errorMessage.value = "Failed to load restaurant details: ${e.localizedMessage}"
            }
        } finally {
            if (!isBackgroundUpdate) {
                _isLoading.value = false
            }
        }
    }

    fun saveChanges() {
        val currentRestaurantId = _restaurant.value?.id
        if (currentRestaurantId.isNullOrBlank()) {
            _saveErrorMessage.value = "Cannot save changes: Restaurant ID is missing."
            _saveSuccess.value = false
            return
        }

        _isSaving.value = true
        _saveSuccess.value = null
        _saveErrorMessage.value = null

        viewModelScope.launch {
            try {
                val updateDTO = UpdateRestaurantDTO(
                    name = editableName.value.takeIf { it != _restaurant.value?.name },
                    description = editableDescription.value.takeIf { it != _restaurant.value?.description },
                    address = editableAddress.value.takeIf { it != _restaurant.value?.address },
                    phone = editablePhone.value.takeIf { it != _restaurant.value?.phone },
                    email = editableEmail.value.takeIf { it != _restaurant.value?.email },
                    // Convertir de HH:mm a ISO 8601 para el DTO
                    openingTime = formatUiTimeToIso(editableOpeningTime.value).takeIf { it != _restaurant.value?.openingTime },
                    closingTime = formatUiTimeToIso(editableClosingTime.value).takeIf { it != _restaurant.value?.closingTime },
                    opensWeekends = editableOpensWeekends.value.takeIf { it != _restaurant.value?.opensWeekends },
                    opensHolidays = editableOpensHolidays.value.takeIf { it != _restaurant.value?.opensHolidays },
                    isActive = editableIsActive.value.takeIf { it != _restaurant.value?.isActive }
                )

                if (updateDTO.name == null && updateDTO.description == null && updateDTO.address == null &&
                    updateDTO.phone == null && updateDTO.email == null && updateDTO.openingTime == null &&
                    updateDTO.closingTime == null && updateDTO.opensWeekends == null &&
                    updateDTO.opensHolidays == null && updateDTO.isActive == null) {
                    _saveErrorMessage.value = "No changes detected."
                    _saveSuccess.value = false
                    _isSaving.value = false
                    return@launch
                }

                // 1. Actualizar el estado local del restaurante inmediatamente
                _restaurant.update { current ->
                    current?.copy(
                        name = updateDTO.name ?: current.name,
                        description = updateDTO.description ?: current.description,
                        address = updateDTO.address ?: current.address,
                        phone = updateDTO.phone ?: current.phone,
                        email = updateDTO.email ?: current.email,
                        // Asegúrate de que el _restaurant.value siga teniendo el formato ISO completo
                        openingTime = updateDTO.openingTime ?: current.openingTime,
                        closingTime = updateDTO.closingTime ?: current.closingTime,
                        opensWeekends = updateDTO.opensWeekends ?: current.opensWeekends,
                        opensHolidays = updateDTO.opensHolidays ?: current.opensHolidays,
                        isActive = updateDTO.isActive ?: current.isActive
                    )
                }
                // Actualizar también los campos editables para reflejar el nuevo estado (en formato HH:mm)
                _restaurant.value?.let {
                    editableName.value = it.name
                    editableDescription.value = it.description
                    editableAddress.value = it.address
                    editablePhone.value = it.phone
                    editableEmail.value = it.email
                    editableOpeningTime.value = formatIsoToUiTime(it.openingTime) // Usar la función de formateo
                    editableClosingTime.value = formatIsoToUiTime(it.closingTime) // Usar la función de formateo
                    editableOpensWeekends.value = it.opensWeekends
                    editableOpensHolidays.value = it.opensHolidays
                    editableIsActive.value = it.isActive
                }

                // 2. PERSISTIR LA ACTUALIZACIÓN EN CACHÉ LRU Y DATASTORE
                _restaurant.value?.let { updatedRestaurant ->
                    restaurantLruCache.put(updatedRestaurant.id, updatedRestaurant)
                    val currentNearby = homeDataRepository.nearbyRestaurantsFlow.first().toMutableList()
                    val index = currentNearby.indexOfFirst { it.id == updatedRestaurant.id }
                    if (index != -1) {
                        currentNearby[index] = updatedRestaurant
                    } else {
                        currentNearby.add(updatedRestaurant)
                    }
                    homeDataRepository.saveNearbyRestaurants(currentNearby)
                    Log.d("VendorViewModel", "Restaurant ${updatedRestaurant.id} updated in LRU Cache and DataStore.")
                }

                // 3. Luego, intenta enviar al servidor
                val success = updateRestaurantUseCase(currentRestaurantId, updateDTO)

                if (success) {
                    _saveSuccess.value = true
                    _saveErrorMessage.value = null
                    Log.d("VendorViewModel", "Restaurant update successful (API or local pending).")
                } else {
                    _saveSuccess.value = false
                    _saveErrorMessage.value = "Failed to save changes. Will retry when online."
                    Log.w("VendorViewModel", "Restaurant update failed, saved locally for retry.")
                    // Si falló, iniciar o reanudar la sincronización
                    syncPendingRestaurantUpdates()
                }

            } catch (e: Exception) {
                _saveSuccess.value = false
                _saveErrorMessage.value = "An error occurred: ${e.localizedMessage}"
                Log.e("VendorViewModel", "Error saving restaurant changes: ${e.message}", e)
                syncPendingRestaurantUpdates() // Asegurarse de que se intente sincronizar
            } finally {
                _isSaving.value = false
            }
        }
    }

    // Lógica de sincronización sin WorkManager
    fun syncPendingRestaurantUpdates() {
        // Cancelar cualquier trabajo de sincronización anterior para evitar duplicados
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            // Solo intentar sincronizar si hay red
            if (!isNetworkAvailable.value) {
                Log.d("VendorViewModel", "No network available for sync. Waiting...")
                return@launch
            }

            Log.d("VendorViewModel", "Starting pending restaurant updates sync...")
            var hasMoreUpdates = true
            var retryCount = 0
            val maxRetries = 3 // Limitar los reintentos para evitar bucles infinitos
            val initialDelayMs = 1000L // 1 segundo
            val maxDelayMs = 10000L // 10 segundos

            while (hasMoreUpdates && retryCount < maxRetries) {
                val pendingUpdates = localRestaurantDataSource.getAllPendingUpdates().firstOrNull() ?: emptyList()

                if (pendingUpdates.isEmpty()) {
                    Log.d("VendorViewModel", "No more pending restaurant updates found. Sync complete.")
                    hasMoreUpdates = false
                    break
                }

                var currentBatchSuccessful = true
                for (update in pendingUpdates) {
                    try {
                        val updateDTO = localRestaurantDataSource.deserializeUpdatePayload(update.updatePayloadJson)
                        Log.d("VendorViewModel", "Attempting to sync update for restaurant: ${update.restaurantId}")
                        val success = updateRestaurantUseCase(update.restaurantId, updateDTO) // Esto ya maneja el guardado local si falla

                        if (success) {
                            Log.d("VendorViewModel", "Successfully synced update for restaurant: ${update.restaurantId}. Deleting from local storage.")
                            localRestaurantDataSource.deletePendingUpdate(update._id.toHexString())
                        } else {
                            Log.w("VendorViewModel", "Failed to sync update for restaurant: ${update.restaurantId}. Will retry.")
                            currentBatchSuccessful = false
                            // No eliminamos la actualización, se reintentará en el próximo ciclo
                        }
                    } catch (e: Exception) {
                        Log.e("VendorViewModel", "Exception during sync for restaurant: ${update.restaurantId}", e)
                        currentBatchSuccessful = false
                        // No eliminamos la actualización
                    }
                }

                if (!currentBatchSuccessful) {
                    retryCount++
                    val delayTime = (initialDelayMs * (1 shl (retryCount - 1))).coerceAtMost(maxDelayMs)
                    Log.w("VendorViewModel", "Batch failed. Retrying in ${delayTime / 1000} seconds. Retry count: $retryCount")
                    delay(delayTime) // Esperar antes de reintentar
                } else {
                    // Si el batch fue exitoso, intentar el siguiente inmediatamente
                    Log.d("VendorViewModel", "Batch successful. Checking for more updates.")
                }
            }

            if (retryCount >= maxRetries && hasMoreUpdates) {
                Log.e("VendorViewModel", "Max retries reached for pending restaurant updates. Some updates may remain unsynced.")
            }
        }
    }

    fun resetSaveStatus() {
        _saveSuccess.value = null
        _saveErrorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel() // Asegurarse de cancelar el trabajo de sincronización cuando el ViewModel se destruye
    }
}