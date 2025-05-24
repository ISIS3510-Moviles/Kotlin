package com.example.campusbites.presentation.ui.viewmodels

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RecommendationRestaurantDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.usecase.GetRecommendationsUseCase
import com.example.campusbites.domain.usecase.ingredient.IncrementIngredientClicksUseCase
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductsUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose

fun RecommendationRestaurantDomain.toRestaurantDomain(): RestaurantDomain {
    return RestaurantDomain(
        id = this.id,
        name = this.name,
        description = "Recommended based on similarity: ${this.similarity}",
        latitude = 0.0,
        longitude = 0.0,
        routeIndications = "",
        openingTime = "",
        closingTime = "",
        opensHolidays = false,
        opensWeekends = false,
        isActive = true,
        rating = this.rating,
        address = "",
        phone = "",
        email = "",
        overviewPhoto = "",
        profilePhoto = "",
        photos = emptyList(),
        foodTags = emptyList(),
        dietaryTags = emptyList(),
        alertsIds = emptyList(),
        reservationsIds = this.reservations.map { it.id },
        suscribersIds = this.subscribers.map { it.id },
        visitsIds = emptyList(),
        commentsIds = this.comments.map { it.id },
        productsIds = emptyList()
    )
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getRecommendationRestaurantsUseCase: GetRecommendationsUseCase,
    private val homeDataRepository: HomeDataRepository,
    private val authRepository: AuthRepository,
    private val connectivityManager: ConnectivityManager,
    private val incrementIngredientClicksUseCase: IncrementIngredientClicksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val isOnline = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                trySend(true)
            }

            override fun onLost(network: android.net.Network) {
                trySend(false)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        trySend(isConnected)
        awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }.distinctUntilChanged()

    init {
        observeCache()
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    triggerNetworkFetches()
                }
            }
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            combine(
                homeDataRepository.nearbyRestaurantsFlow,
                homeDataRepository.recommendedRestaurantsFlow,
                homeDataRepository.allProductsFlow,
                homeDataRepository.allIngredientsFlow
            ) { nearby, recommended, products, ingredients ->
                _uiState.update { currentState ->
                    // Get top 5 ingredients by click count
                    val topIngredients = ingredients
                        .sortedByDescending { it.clicks }
                        .take(5)

                    val newState = currentState.copy(
                        restaurants = nearby,
                        recommendationRestaurants = recommended,
                        products = products,
                        ingredients = ingredients,
                        popularIngredients = topIngredients,
                        isLoadingInitial = false
                    )
                    Log.d("HomeViewModel", "Cache observed and UI updated: Nearby=${newState.restaurants.size}, Recommended=${newState.recommendationRestaurants.size}, Products=${newState.products.size}, Ingredients=${newState.ingredients.size}, PopularIngredients=${newState.popularIngredients.size}")
                    newState
                }
            }.catch { e ->
                Log.e("HomeViewModel", "Error observing cache", e)
                _uiState.update { it.copy(isLoadingInitial = false, errorMessage = "Failed to load cached data") }
            }.collect()
        }
    }


    private fun triggerNetworkFetches() {
        if (_uiState.value.isLoadingNetwork) return
        _uiState.update { it.copy(isLoadingNetwork = true) }

        viewModelScope.launch {
            try {
                coroutineScope {
                    val nearbyRestaurantsDeferred: Deferred<List<RestaurantDomain>?> = async { fetchRestaurants() }
                    val ingredientsDeferred: Deferred<List<IngredientDomain>?> = async { fetchIngredients() }
                    val productsDeferred: Deferred<List<ProductDomain>?> = async { fetchProducts() }

                    launch {
                        val fetchedNearby = nearbyRestaurantsDeferred.await()
                        fetchedNearby?.let { homeDataRepository.saveNearbyRestaurants(it) }
                        Log.d("HomeViewModel", "Fetched nearby restaurants and updated cache.")
                    }

                    launch {
                        val fetchedIngredients = ingredientsDeferred.await()
                        fetchedIngredients?.let { homeDataRepository.saveAllIngredients(it) }
                        Log.d("HomeViewModel", "Fetched ingredients and updated cache.")
                    }

                    launch {
                        val fetchedProducts = productsDeferred.await()
                        fetchedProducts?.let { homeDataRepository.saveAllProducts(it) }
                        Log.d("HomeViewModel", "Fetched products and updated cache.")
                    }

                    val currentUser = authRepository.currentUser.first { it != null }
                    val fetchedRecommendations = currentUser?.let { user ->
                        val currentNearby = nearbyRestaurantsDeferred.await() ?: homeDataRepository.nearbyRestaurantsFlow.first()
                        fetchAndSaveRecommendationRestaurants(user, currentNearby)
                    }

                    Log.d("HomeViewModel", "All network fetches launched. UI will react via observeCache as data becomes available.")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during network fetches", e)
                _uiState.update { it.copy(errorMessage = "Failed to update data from network") }
            } finally {
                _uiState.update { it.copy(isLoadingNetwork = false) }
            }
        }
    }


    private suspend fun fetchRestaurants(): List<RestaurantDomain>? {
        return try {
            val restaurants = getRestaurantsUseCase()
            Log.d("HomeViewModel", "Fetched ${restaurants.size} nearby restaurants.")
            restaurants
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            FirebaseCrashlytics.getInstance().setCustomKey("error_screen", "home_restaurants_fetch")
            Log.e("HomeViewModel", "Error fetching restaurants: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchIngredients(): List<IngredientDomain>? {
        return try {
            val ingredients = getIngredientsUseCase()
            Log.d("HomeViewModel", "Fetched ${ingredients.size} ingredients.")
            ingredients
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching ingredients: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchProducts(): List<ProductDomain>? {
        return try {
            val products = getProductsUseCase()
            Log.d("HomeViewModel", "Fetched ${products.size} products.")
            products
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching products: ${e.message}", e)
            null
        }
    }


    private suspend fun fetchAndSaveRecommendationRestaurants(user: UserDomain, nearbyRestaurants: List<RestaurantDomain>): List<RestaurantDomain>? {
        return try {
            Log.d("HomeViewModel", "Fetching recommendations for user: ${user.id}")
            val recommendationsRaw = getRecommendationRestaurantsUseCase(user.id, 5)

            val nearbyRestaurantMap = nearbyRestaurants.associateBy { it.id }

            val validRecommendations = recommendationsRaw.mapNotNull { recRaw ->
                nearbyRestaurantMap[recRaw.id]
            }

            if (validRecommendations.size < recommendationsRaw.size) {
                Log.w("HomeViewModel", "Could not find full data for ${recommendationsRaw.size - validRecommendations.size} recommended restaurants.")
            }

            homeDataRepository.saveRecommendedRestaurants(validRecommendations)
            Log.d("HomeViewModel", "Fetched and saved ${validRecommendations.size} recommendations with full data.")
            validRecommendations
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching/saving recommendations: ${e.message}", e)
            null
        }
    }


    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun incrementIngredientClicks(ingredientId: String) {
        viewModelScope.launch {
            try {
                incrementIngredientClicksUseCase(ingredientId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error al incrementar clics para $ingredientId: ${e.message}")
                _uiState.update { it.copy(errorMessage = "No se pudo actualizar el contador de clics.") }
            }
        }
    }
}

data class HomeUiState(
    val restaurants: List<RestaurantDomain> = emptyList(),
    val ingredients: List<IngredientDomain> = emptyList(),
    val popularIngredients: List<IngredientDomain> = emptyList(),
    val products: List<ProductDomain> = emptyList(),
    val searchQuery: String = "",
    val isLoadingInitial: Boolean = true,
    val isLoadingNetwork: Boolean = false,
    val errorMessage: String? = null,
    val recommendationRestaurants: List<RestaurantDomain> = emptyList()
)