package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductsUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadRestaurants()
        loadIngredients()
        loadProducts()
    }

    private fun loadRestaurants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val restaurants = getRestaurantsUseCase()
                _uiState.value = _uiState.value.copy(
                    restaurants = restaurants,
                    isLoading = false
                )
                Log.d("API_TEXT", "Loaded restaurants: $restaurants")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading restaurants",
                    isLoading = false
                )
                Log.e("API_TEST", "Error: ${e.message}", e)
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = getUserByIdUseCase("ps8ntqSGvzgilhqlXKNP")
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoading = false
                )
                Log.d("API_TEXT", "Loaded user: $user")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading user",
                    isLoading = false
                )
                Log.e("API_TEST", "Error: ${e.message}", e)
            }
        }
    }

    private fun loadIngredients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val ingredients = getIngredientsUseCase()
                _uiState.value = _uiState.value.copy(
                    ingredients = ingredients,
                    isLoading = false
                )
                Log.d("API_TEXT", "Loaded ingredients: $ingredients")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading ingredients",
                    isLoading = false
                )
                Log.e("API_TEST", "Error: ${e.message}", e)
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val products = getProductsUseCase()
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false
                )
                Log.d("API_TEXT", "Loaded products: $products")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading products",
                    isLoading = false
                )
                Log.e("API_TEST", "Error: ${e.message}", e)
            }
        }
    }




    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    searchQuery = query
                )
            }
        }
    }

}



data class HomeUiState(
    val user: UserDomain? = null,
    val restaurants: List<RestaurantDomain> = emptyList(),
    val ingredients: List<IngredientDomain> = emptyList(),
    val products: List<ProductDomain> = emptyList(),
    val filteredRestaurants: List<RestaurantDomain> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedRestaurantDomain: RestaurantDomain? = null
)