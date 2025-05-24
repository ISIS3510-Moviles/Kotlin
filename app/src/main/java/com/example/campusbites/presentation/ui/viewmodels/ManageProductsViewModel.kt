package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository // Importar
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.usecase.product.DeleteProductUseCase
import com.example.campusbites.domain.usecase.product.GetProductsByRestaurantUseCase
import com.example.campusbites.domain.usecase.product.ObserveProductsByRestaurantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageProductsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeProductsByRestaurantUseCase: ObserveProductsByRestaurantUseCase,
    private val getProductsByRestaurantUseCase: GetProductsByRestaurantUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val homeDataRepository: HomeDataRepository // Inyectar
) : ViewModel() {

    val restaurantId: String = savedStateHandle.get<String>("restaurantId") ?: ""

    private val _uiState = MutableStateFlow(ManageProductsUiState())
    val uiState: StateFlow<ManageProductsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    val isNetworkAvailable: StateFlow<Boolean> = connectivityMonitor.isNetworkAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)


    init {
        if (restaurantId.isNotBlank()) {
            observeProductChanges() // Esto ahora cargará desde el caché primero
            tryInitialRefresh()    // Intentar un refresh si hay red
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Restaurant ID not provided.") }
        }
    }

    private fun observeProductChanges() {
        viewModelScope.launch {
            observeProductsByRestaurantUseCase(restaurantId)
                .catch { e ->
                    Log.e("ManageProductsVM", "Error observing products for $restaurantId from cache", e)
                    // Si falla la observación del caché, es un problema mayor, pero
                    // intentamos manejarlo para que la UI no se quede en loading infinito.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error loading products from local storage: ${e.localizedMessage}"
                        )
                    }
                }
                .collectLatest { products ->
                    _uiState.update {
                        it.copy(
                            products = products,
                            // isLoading se maneja en tryInitialRefresh o cuando la lista está vacía y no hay red
                            isLoading = it.isLoading && products.isEmpty(), // Seguir cargando si la lista está vacía y aún no se ha intentado refresh
                            errorMessage = if (products.isNotEmpty()) null else it.errorMessage // Limpiar error si llegan productos
                        )
                    }
                    // Si después de cargar del caché, la lista está vacía y no hay red, mostrar mensaje
                    if (products.isEmpty() && !_uiState.value.isLoading && !isNetworkAvailable.value) {
                        _uiState.update { it.copy(errorMessage = "No products found in cache and no internet connection.") }
                    }
                }
        }
    }

    // Renombrado para claridad, y se llama desde init
    private fun tryInitialRefresh() {
        if (restaurantId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Indicar carga

        viewModelScope.launch {
            if (isNetworkAvailable.first()) {
                try {
                    // Esta llamada es para refrescar el caché subyacente (HomeDataRepository)
                    // ProductRepositoryImpl.getProductsByRestaurant ya actualiza HomeDataRepository
                    getProductsByRestaurantUseCase(restaurantId)
                    Log.d("ManageProductsVM", "Initial product data refresh successful for $restaurantId.")
                    // El observador (observeProductChanges) recogerá los cambios y actualizará isLoading.
                    // Si el caché estaba vacío, isLoading seguirá true hasta que el observador emita.
                    // Si el observador emite una lista vacía, isLoading se pondrá en false allí.
                    _uiState.update { it.copy(isLoading = false) } // Asegurar que isLoading se ponga a false si la red tuvo éxito
                } catch (e: Exception) {
                    Log.e("ManageProductsVM", "Error refreshing products for $restaurantId from network", e)
                    val currentProducts = homeDataRepository.allProductsFlow.first().filter { it.restaurantId == restaurantId }
                    if (currentProducts.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load products. Check your internet connection.") }
                    } else {
                        // Ya hay datos en caché, el error de red no es crítico para la visualización inicial.
                        _uiState.update { it.copy(isLoading = false) }
                        viewModelScope.launch {
                            _uiEvent.emit(UiEvent.ShowMessage("Could not refresh products from server. Displaying cached data."))
                        }
                    }
                }
            } else {
                // Offline: confiamos en que observeProductChanges cargará desde el caché.
                // Si el caché está vacío, el observador se encargará de mostrar el mensaje apropiado.
                val cachedProducts = homeDataRepository.allProductsFlow.first().filter {it.restaurantId == restaurantId}
                if (cachedProducts.isEmpty()){
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No internet connection and no products found in cache.") }
                } else {
                    _uiState.update { it.copy(isLoading = false) } // Ya se cargó de caché.
                }
            }
        }
    }

    // La función loadProducts ahora es tryInitialRefresh
    fun refreshProducts() { // Para pull-to-refresh o botón de refresco manual
        tryInitialRefresh()
    }


    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            // No cambiaremos isLoading aquí para evitar parpadeos si la lista se actualiza rápido
            // _uiState.update { it.copy(isLoading = true) }
            try {
                val success = deleteProductUseCase(productId)
                if (success) {
                    _uiEvent.emit(UiEvent.ShowMessage("Product deleted successfully."))
                    // La lista se actualizará a través del observador
                } else {
                    // Esto podría ocurrir si el servidor devuelve un error explícito,
                    // pero la lógica offline se maneja con la excepción.
                    _uiEvent.emit(UiEvent.ShowMessage("Failed to delete product on server."))
                }
            } catch (e: Exception) {
                if (e.message?.contains("Offline") == true || e.message?.contains("queued") == true) {
                    _uiEvent.emit(UiEvent.ShowMessage("Offline: Product deletion queued."))
                    // La UI aún no reflejará esto inmediatamente como "pendiente" a menos que
                    // ObserveProductsByRestaurantUseCase tenga lógica para mostrar productos pendientes.
                    // Por ahora, la lista se actualizará cuando la acción se procese.
                } else {
                    _uiEvent.emit(UiEvent.ShowMessage("Error deleting product: ${e.localizedMessage}"))
                }
            }
            // _uiState.update { it.copy(isLoading = false) }
        }
    }

    data class ManageProductsUiState(
        val products: List<ProductDomain> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
    }
}