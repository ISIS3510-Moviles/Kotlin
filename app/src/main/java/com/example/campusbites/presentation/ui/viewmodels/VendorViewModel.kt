package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.cache.RestaurantLruCache
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorViewModel @Inject constructor(
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase,
    private val restaurantLruCache: RestaurantLruCache, // Para caché en memoria
    private val homeDataRepository: HomeDataRepository, // Para caché en DataStore
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _restaurant = MutableStateFlow<RestaurantDomain?>(null)
    val restaurant: StateFlow<RestaurantDomain?> = _restaurant.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isNetworkAvailable: StateFlow<Boolean> = connectivityMonitor.isNetworkAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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
            _isLoading.value = true // Mostrar carga solo si no es actualización en segundo plano
        }
        try {
            val fetchedRestaurant = getRestaurantByIdUseCase(restaurantId)
            if (fetchedRestaurant != null) {
                _restaurant.value = fetchedRestaurant
                restaurantLruCache.put(restaurantId, fetchedRestaurant) // Actualizar LRU
                // Actualizar HomeDataRepository
                val currentNearby = homeDataRepository.nearbyRestaurantsFlow.first().toMutableList()
                val index = currentNearby.indexOfFirst { it.id == restaurantId }
                if (index != -1) {
                    currentNearby[index] = fetchedRestaurant
                } else {
                    currentNearby.add(fetchedRestaurant) // Si no estaba, añadirlo
                }
                homeDataRepository.saveNearbyRestaurants(currentNearby)
                Log.d("VendorViewModel", "Restaurant $restaurantId fetched/updated from server.")
            } else {
                if (!isBackgroundUpdate) { // Solo mostrar error si no se pudo cargar nada
                    _errorMessage.value = "Restaurant not found on server."
                }
                Log.w("VendorViewModel", "Restaurant $restaurantId not found on server during fetch.")
            }
        } catch (e: Exception) {
            Log.e("VendorViewModel", "Error fetching restaurant $restaurantId from server: ${e.message}", e)
            if (!isBackgroundUpdate && _restaurant.value == null) { // Solo mostrar error si no había nada en caché
                _errorMessage.value = "Failed to load restaurant details: ${e.localizedMessage}"
            }
        } finally {
            if (!isBackgroundUpdate) {
                _isLoading.value = false
            }
        }
    }
}