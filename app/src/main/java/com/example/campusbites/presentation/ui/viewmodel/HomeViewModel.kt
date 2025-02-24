package com.example.campusbites.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.domain.model.Restaurant
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.usecase.food.GetFoodTags
import com.example.campusbites.domain.usecase.restaurant.FilterRestaurants
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
    private val filterRestaurants: FilterRestaurants,
    private val getFoodTags: GetFoodTags
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRestaurants()
        loadFoodTags()
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

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    searchQuery = query,
                    filteredRestaurants = filterRestaurants()
                )
            }
        }
    }
}

data class HomeUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val filteredRestaurants: List<Restaurant> = emptyList(),
    val foodTags: List<FoodTag> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)