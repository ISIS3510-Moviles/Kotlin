package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.repository.HomeDataRepository
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductsUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.example.campusbites.domain.usecase.GetRecommendationsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getRecommendationRestaurantsUseCase: GetRecommendationsUseCase,
    private val homeDataRepository: HomeDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialCache()
        triggerNetworkFetches()
    }

    private fun loadInitialCache() {
        viewModelScope.launch {
            combine(
                homeDataRepository.nearbyRestaurantsFlow,
                homeDataRepository.recommendedRestaurantsFlow,
                homeDataRepository.allProductsFlow,
                homeDataRepository.allIngredientsFlow
            ) { nearby, recommended, products, ingredients ->
                Log.d("HomeViewModel", "Cache loaded: Nearby=${nearby.size}, Recommended=${recommended.size}, Products=${products.size}, Ingredients=${ingredients.size}")
                _uiState.update { currentState ->
                    currentState.copy(
                        restaurants = nearby,
                        recommendationRestaurants = recommended,
                        products = products,
                        ingredients = ingredients,
                        isLoadingInitial = false
                    )
                }
            }.catch { e ->
                Log.e("HomeViewModel", "Error loading initial cache", e)
                _uiState.update { it.copy(isLoadingInitial = false, errorMessage = "Failed to load cached data") }
            }.collect()
        }
    }

    private fun triggerNetworkFetches() {
        _uiState.update { it.copy(isLoadingNetwork = true) }
        viewModelScope.launch {
            try {
                fetchAndSaveRestaurants()
                fetchAndSaveIngredients()
                fetchAndSaveProducts()
                Log.d("HomeViewModel", "Initial network fetches triggered (excluding recommendations).")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during initial network fetches", e)
                _uiState.update { it.copy(errorMessage = "Failed to update data from network") }
            } finally {
                _uiState.update { it.copy(isLoadingNetwork = false) }
            }
        }
    }

    private suspend fun fetchAndSaveRestaurants() {
        try {
            val restaurants = getRestaurantsUseCase()
            homeDataRepository.saveNearbyRestaurants(restaurants)
            Log.d("HomeViewModel", "Fetched and saved ${restaurants.size} nearby restaurants.")
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            FirebaseCrashlytics.getInstance().setCustomKey("error_screen", "home_restaurants_fetch_save")
            Log.e("HomeViewModel", "Error fetching/saving restaurants: ${e.message}", e)

        }
    }

    private suspend fun fetchAndSaveIngredients() {
        try {
            val ingredients = getIngredientsUseCase()
            homeDataRepository.saveAllIngredients(ingredients)
            Log.d("HomeViewModel", "Fetched and saved ${ingredients.size} ingredients.")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching/saving ingredients: ${e.message}", e)

        }
    }

    private suspend fun fetchAndSaveProducts() {
        try {
            val products = getProductsUseCase()
            homeDataRepository.saveAllProducts(products)
            Log.d("HomeViewModel", "Fetched and saved ${products.size} products.")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching/saving products: ${e.message}", e)

        }
    }

    fun loadRecommendationRestaurants(user: UserDomain) {
        if (_uiState.value.isLoadingNetwork) return

        _uiState.update { it.copy(isLoadingNetwork = true) }
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Fetching and saving recommendations for user: ${user.id}")
                val recommendations = getRecommendationRestaurantsUseCase(user.id, 10)


                Log.d("HomeViewModel", "Fetched and saved ${recommendations.size} recommendations.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching/saving recommendations: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Failed to update recommendations") }
            } finally {
                _uiState.update { it.copy(isLoadingNetwork = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}

data class HomeUiState(
    val restaurants: List<RestaurantDomain> = emptyList(),
    val ingredients: List<IngredientDomain> = emptyList(),
    val products: List<ProductDomain> = emptyList(),
    val searchQuery: String = "",
    val isLoadingInitial: Boolean = true,
    val isLoadingNetwork: Boolean = false,
    val errorMessage: String? = null,
    val recommendationRestaurants: List<RestaurantDomain> = emptyList()
)