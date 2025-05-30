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

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    private val _saveErrorMessage = MutableStateFlow<String?>(null)
    val saveErrorMessage: StateFlow<String?> = _saveErrorMessage.asStateFlow()

    private var syncJob: Job? = null

    private val uiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val isoDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    init {
        viewModelScope.launch {
            _restaurant.collect { restaurant ->
                restaurant?.let {
                    editableName.value = it.name
                    editableDescription.value = it.description
                    editableAddress.value = it.address
                    editablePhone.value = it.phone
                    editableEmail.value = it.email
                    editableOpeningTime.value = formatIsoToUiTime(it.openingTime)
                    editableClosingTime.value = formatIsoToUiTime(it.closingTime)
                    editableOpensWeekends.value = it.opensWeekends
                    editableOpensHolidays.value = it.opensHolidays
                    editableIsActive.value = it.isActive
                }
            }
        }

        viewModelScope.launch {
            isNetworkAvailable.collect { isConnected ->
                if (isConnected) {
                    Log.d("VendorViewModel", "Network available. Attempting to sync pending updates.")
                    syncPendingRestaurantUpdates()
                } else {
                    Log.d("VendorViewModel", "Network lost. Pausing sync attempts.")
                    syncJob?.cancel()
                }
            }
        }
    }

    private fun formatIsoToUiTime(isoTime: String): String {
        return try {
            val dateTime = OffsetDateTime.parse(isoTime, isoDateTimeFormatter)
            dateTime.toLocalTime().format(uiTimeFormatter)
        } catch (e: DateTimeParseException) {
            Log.e("VendorViewModel", "Error parsing ISO time for UI: $isoTime", e)
            ""
        }
    }

    private fun formatUiTimeToIso(uiTime: String): String {
        return try {
            val localTime = LocalTime.parse(uiTime, uiTimeFormatter)
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
            var cachedRestaurant = restaurantLruCache.get(restaurantId)
            if (cachedRestaurant != null) {
                _restaurant.value = cachedRestaurant
                _isLoading.value = false
                Log.d("VendorViewModel", "Restaurant $restaurantId loaded from LRU Cache.")
                if (isNetworkAvailable.value) {
                    fetchRestaurantFromServer(restaurantId, true)
                }
                return@launch
            }

            cachedRestaurant = homeDataRepository.nearbyRestaurantsFlow.first().find { it.id == restaurantId }
            if (cachedRestaurant != null) {
                _restaurant.value = cachedRestaurant
                restaurantLruCache.put(restaurantId, cachedRestaurant)
                _isLoading.value = false
                Log.d("VendorViewModel", "Restaurant $restaurantId loaded from HomeDataRepository.")
                if (isNetworkAvailable.value) {
                    fetchRestaurantFromServer(restaurantId, true)
                }
                return@launch
            }

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

                _restaurant.update { current ->
                    current?.copy(
                        name = updateDTO.name ?: current.name,
                        description = updateDTO.description ?: current.description,
                        address = updateDTO.address ?: current.address,
                        phone = updateDTO.phone ?: current.phone,
                        email = updateDTO.email ?: current.email,
                        openingTime = updateDTO.openingTime ?: current.openingTime,
                        closingTime = updateDTO.closingTime ?: current.closingTime,
                        opensWeekends = updateDTO.opensWeekends ?: current.opensWeekends,
                        opensHolidays = updateDTO.opensHolidays ?: current.opensHolidays,
                        isActive = updateDTO.isActive ?: current.isActive
                    )
                }
                _restaurant.value?.let {
                    editableName.value = it.name
                    editableDescription.value = it.description
                    editableAddress.value = it.address
                    editablePhone.value = it.phone
                    editableEmail.value = it.email
                    editableOpeningTime.value = formatIsoToUiTime(it.openingTime)
                    editableClosingTime.value = formatIsoToUiTime(it.closingTime)
                    editableOpensWeekends.value = it.opensWeekends
                    editableOpensHolidays.value = it.opensHolidays
                    editableIsActive.value = it.isActive
                }

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

                val success = updateRestaurantUseCase(currentRestaurantId, updateDTO)

                if (success) {
                    _saveSuccess.value = true
                    _saveErrorMessage.value = null
                    Log.d("VendorViewModel", "Restaurant update successful (API or local pending).")
                } else {
                    _saveSuccess.value = false
                    _saveErrorMessage.value = "Failed to save changes. Will retry when online."
                    Log.w("VendorViewModel", "Restaurant update failed, saved locally for retry.")
                    syncPendingRestaurantUpdates()
                }

            } catch (e: Exception) {
                _saveSuccess.value = false
                _saveErrorMessage.value = "An error occurred: ${e.localizedMessage}"
                Log.e("VendorViewModel", "Error saving restaurant changes: ${e.message}", e)
                syncPendingRestaurantUpdates()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun syncPendingRestaurantUpdates() {
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            if (!isNetworkAvailable.value) {
                Log.d("VendorViewModel", "No network available for sync. Waiting...")
                return@launch
            }

            Log.d("VendorViewModel", "Starting pending restaurant updates sync...")
            var hasMoreUpdates = true
            var retryCount = 0
            val maxRetries = 3
            val initialDelayMs = 1000L
            val maxDelayMs = 10000L

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
                        val success = updateRestaurantUseCase(update.restaurantId, updateDTO)

                        if (success) {
                            Log.d("VendorViewModel", "Successfully synced update for restaurant: ${update.restaurantId}. Deleting from local storage.")
                            localRestaurantDataSource.deletePendingUpdate(update._id.toHexString())
                        } else {
                            Log.w("VendorViewModel", "Failed to sync update for restaurant: ${update.restaurantId}. Will retry.")
                            currentBatchSuccessful = false
                        }
                    } catch (e: Exception) {
                        Log.e("VendorViewModel", "Exception during sync for restaurant: ${update.restaurantId}", e)
                        currentBatchSuccessful = false
                    }
                }

                if (!currentBatchSuccessful) {
                    retryCount++
                    val delayTime = (initialDelayMs * (1 shl (retryCount - 1))).coerceAtMost(maxDelayMs)
                    Log.w("VendorViewModel", "Batch failed. Retrying in ${delayTime / 1000} seconds. Retry count: $retryCount")
                    delay(delayTime)
                } else {
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
        syncJob?.cancel()
    }
}