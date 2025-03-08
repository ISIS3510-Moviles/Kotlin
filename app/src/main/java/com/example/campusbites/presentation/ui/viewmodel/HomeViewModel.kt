package com.example.campusbites.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.Product
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.usecase.food.GetFoodTags
import com.example.campusbites.domain.usecase.food.GetFoods
import com.example.campusbites.domain.usecase.restaurant.GetRestaurants
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantById
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
    private val getFoodTags: GetFoodTags,
    private val getFoods: GetFoods,
    private val getRestaurantById: GetRestaurantById
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRestaurants()
        loadFoodTags()
        loadFoods()
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading restaurants",
                    isLoading = false
                )
            }
        }
    }

    private fun loadFoodTags() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val foodTags = getFoodTags()
                _uiState.value = _uiState.value.copy(
                    foodTags = foodTags,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading food tags",
                    isLoading = false
                )
            }
        }
    }

    private fun loadFoods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val foods = getFoods()
                _uiState.value = _uiState.value.copy(
                    foods = foods,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading foods",
                    isLoading = false
                )
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

    fun loadRestaurantDetails(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val restaurant = getRestaurantById(id)
                _uiState.value = _uiState.value.copy(
                    selectedRestaurant = restaurant,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error loading restaurant details",
                    isLoading = false
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
