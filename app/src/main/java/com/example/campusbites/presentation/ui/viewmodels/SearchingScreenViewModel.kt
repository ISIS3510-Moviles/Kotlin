package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Importar el nuevo SearchCache y SearchResults
import com.example.campusbites.data.cache.SearchCache
import com.example.campusbites.data.cache.SearchResults
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.product.SearchProductsUseCase
import com.example.campusbites.domain.usecase.restaurant.SearchRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.io.IOException

@HiltViewModel
class SearchingScreenViewModel @Inject constructor(
    private val searchProductsUseCase: SearchProductsUseCase,
    private val searchRestaurantsUseCase: SearchRestaurantsUseCase,
    private val searchCache: SearchCache // Inyectar el nuevo SearchCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchingUiState())
    val uiState: StateFlow<SearchingUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    filteredProducts = emptyList(),
                    filteredRestaurants = emptyList(),
                    isLoading = false,
                    errorMessage = null
                )
            }
            return
        }

        _uiState.update { it.copy(searchQuery = trimmedQuery, isLoading = true, errorMessage = null) }

        // 1. Consultar el caché
        val cachedResults = searchCache.get(trimmedQuery)
        if (cachedResults != null) {
            _uiState.update {
                it.copy(
                    filteredProducts = cachedResults.products,
                    filteredRestaurants = cachedResults.restaurants,
                    isLoading = false
                )
            }
            Log.d("SearchingVM", "Search results for '$trimmedQuery' loaded from SearchCache.")
            return
        }

        // 2. Si no está en caché, realizar la búsqueda en la red
        viewModelScope.launch {
            try {
                val products = searchProductsUseCase(trimmedQuery)
                val restaurants = searchRestaurantsUseCase(trimmedQuery)

                // Guardar en caché
                searchCache.put(trimmedQuery, SearchResults(products, restaurants))

                _uiState.update {
                    it.copy(
                        filteredProducts = products,
                        filteredRestaurants = restaurants,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                Log.d("SearchingVM", "Search complete: ${products.size} products, ${restaurants.size} restaurants found for '$trimmedQuery'. Results cached.")
            } catch (e: IOException) {
                Log.e("SearchingVM", "Network error during search for '$trimmedQuery': ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No internet connection. Please check your network."
                    )
                }
            } catch (e: Exception) {
                Log.e("SearchingVM", "Error during search for '$trimmedQuery': ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An error occurred during search: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}

data class SearchingUiState(
    val filteredProducts: List<ProductDomain> = emptyList(),
    val filteredRestaurants: List<RestaurantDomain> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)