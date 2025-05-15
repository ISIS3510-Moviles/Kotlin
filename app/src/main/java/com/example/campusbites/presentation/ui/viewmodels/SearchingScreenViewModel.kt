package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.io.IOException // Import IOException for network errors

@HiltViewModel
class SearchingScreenViewModel @Inject constructor(
    private val searchProductsUseCase: SearchProductsUseCase,
    private val searchRestaurantsUseCase: SearchRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchingUiState())
    val uiState: StateFlow<SearchingUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, isLoading = true, errorMessage = null) } // Clear previous error message
        viewModelScope.launch {
            try {
                val products = searchProductsUseCase(query)
                val restaurants = searchRestaurantsUseCase(query)
                _uiState.update {
                    it.copy(
                        filteredProducts = products,
                        filteredRestaurants = restaurants,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                Log.d("SearchingVM", "Search complete: ${products.size} products, ${restaurants.size} restaurants found for '$query'")
            } catch (e: IOException) { // Catch specific network exceptions
                Log.e("SearchingVM", "Network error during search for '$query'")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No internet connection. Please check your network."
                    )
                }
            } catch (e: Exception) { // Catch other potential exceptions
                Log.e("SearchingVM", "Error during search for '$query'")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An error occurred during search"
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