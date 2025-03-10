package com.example.campusbites.presentation.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.Product
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.usecase.restaurant.GetRestaurants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRestaurants: GetRestaurants,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRestaurants()
    }

    private fun loadRestaurants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val restaurants = getRestaurants()
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
    val restaurants: List<Restaurant> = emptyList(),
    val filteredRestaurants: List<Restaurant> = emptyList(),
    val selectedRestaurant: Restaurant? = null, // Nuevo
    val foods: List<Product> = emptyList(),
    val foodTags: List<FoodTag> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
