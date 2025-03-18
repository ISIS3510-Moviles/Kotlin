package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.product.GetProductsUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchingScreenViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchingUiState())
    val uiState: StateFlow<SearchingUiState> = _uiState

    fun onSearchQueryChanged(query: String) {
        _uiState.update { currentState ->
            val filteredProducts = currentState.products.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }

            val filteredRestaurants = currentState.restaurants.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }

            currentState.copy(
                searchQuery = query,
                filteredProducts = filteredProducts,
                filteredRestaurants = filteredRestaurants
            )
        }
    }

    init {
        fetchSearchData()
    }

    private fun fetchSearchData() {
        viewModelScope.launch {
            val products = getProductsUseCase()
            val restaurants = getRestaurantsUseCase()
            _uiState.update { currentState ->
                currentState.copy(
                    products = products,
                    restaurants = restaurants
                )
            }
        }
    }
}

data class SearchingUiState(
    val products: List<ProductDomain> = emptyList(),
    val restaurants: List<RestaurantDomain> = emptyList(),
    val filteredProducts: List<ProductDomain> = emptyList(),
    val filteredRestaurants: List<RestaurantDomain> = emptyList(),
    val searchQuery: String = ""
)
