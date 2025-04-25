package com.example.campusbites.presentation.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.comment.CreateCommentUseCase
import com.example.campusbites.domain.usecase.product.GetProductsByRestaurantUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.comment.GetCommentsUseCase
import com.example.campusbites.domain.usecase.reservation.CancelReservationUseCase
import com.example.campusbites.domain.usecase.reservation.CreateReservationUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.take
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase,
    private val getProductsByRestaurantUseCase: GetProductsByRestaurantUseCase,
    private val getReviewsByRestaurantUseCase: GetCommentsUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val createReservationUseCase: CreateReservationUseCase,
    private val createCommentUseCase: CreateCommentUseCase,
    private val homeDataRepository: HomeDataRepository,
    private val connectivityManager: ConnectivityManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState

    private val _restaurants = MutableStateFlow<List<RestaurantDomain>>(emptyList())
    val restaurants: StateFlow<List<RestaurantDomain>> = _restaurants

    private val _restaurantId = MutableStateFlow<String?>(null)

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
        viewModelScope.launch {
            combine(_restaurantId, isOnline) { restaurantId, online ->
                Pair(restaurantId, online)
            }.collectLatest { (restaurantId, online) ->
                _uiState.update { it.copy(isOnline = online) }
                if (restaurantId != null) {
                    loadRestaurantDetails(restaurantId)
                    if (online) {
                        fetchRestaurantDetails(restaurantId)
                    }
                }
            }
        }

        viewModelScope.launch {
            _restaurantId.filterNotNull().collectLatest { restaurantId ->
                getReviewsByRestaurantUseCase(restaurantId)
                    .catch { e -> Log.e("RestaurantDetailViewModel", "Error fetching reviews: ${e.message}") }
                    .collect { reviews ->
                        _uiState.update { currentState ->
                            currentState.copy(reviews = reviews)
                        }
                    }
            }
        }

        viewModelScope.launch {
            isOnline
                .filter { it }
                .collect {
                    _restaurantId.value?.let { restaurantId ->
                        syncReviews(restaurantId)
                    }
                }
        }
    }

    fun loadRestaurantDetails(restaurantId: String) {
        _restaurantId.value = restaurantId
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val cachedRestaurant = homeDataRepository.nearbyRestaurantsFlow.firstOrNull()?.find { it.id == restaurantId }
                if (cachedRestaurant != null) {
                    val cachedProducts = homeDataRepository.allProductsFlow.firstOrNull()?.filter { product ->
                        cachedRestaurant.id.contains(product.restaurantId)
                    } ?: emptyList()
                    Log.d("RestaurantDetailViewModel", "Restaurant and products loaded from cache")
                    Log.d("RestaurantDetailViewModel", "Restaurant: $cachedRestaurant")
                    Log.d("RestaurantDetailViewModel", "Products: $cachedProducts")
                    _uiState.update { currentState ->
                        currentState.copy(
                            restaurant = cachedRestaurant,
                            products = cachedProducts,
                            popularProducts = cachedProducts.sortedByDescending { it.rating }.take(5),
                            under20Products = cachedProducts.filter { it.price <= 20000 }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error loading cached details: ${e.message}")
            }
        }
    }

    private suspend fun fetchRestaurantDetails(restaurantId: String) {
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(isLoadingNetwork = true) }
        }
        try {
            val restaurant = getRestaurantByIdUseCase(restaurantId)
            val products = getProductsByRestaurantUseCase(restaurantId)

            withContext(Dispatchers.Main) {
                _uiState.update { currentState ->
                    currentState.copy(
                        restaurant = restaurant,
                        products = products,
                        popularProducts = products.sortedByDescending { it.rating }.take(5),
                        under20Products = products.filter { it.price <= 20000 }
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("RestaurantDetailViewModel", "Error fetching restaurant details from network: ${e.message}")
        } finally {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoadingNetwork = false) }
            }
        }
    }

    private fun syncReviews(restaurantId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getReviewsByRestaurantUseCase(restaurantId).firstOrNull()
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error syncing reviews: ${e.message}")
            }
        }
    }


    suspend fun onSaveClick(user: UserDomain) {
        try {
            updateUserUseCase(user.id, user)
        } catch (e: Exception) {
            Log.e("RestaurantDetailViewModel", "Error updating user: ${e.message}")
        }
    }

    fun loadAllRestaurants() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allRestaurants = getRestaurantsUseCase()
                withContext(Dispatchers.Main) {
                    _restaurants.value = allRestaurants
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error loading all restaurants: ${e.message}")
            }
        }
    }

    fun createReservation(reservation: ReservationDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                createReservationUseCase(reservation)
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error creating reservation: ${e.message}")
            }
        }
    }

    fun createReview(comment: CommentDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val created = createCommentUseCase(comment)
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error creating review: ${e.message}")
            }
        }
    }
}

data class RestaurantDetailUiState(
    val restaurant: RestaurantDomain? = null,
    val products: List<ProductDomain> = emptyList(),
    val reviews: List<CommentDomain> = emptyList(),
    val popularProducts: List<ProductDomain> = emptyList(),
    val under20Products: List<ProductDomain> = emptyList(),
    val isLoadingNetwork: Boolean = false,
    val isOnline: Boolean = false
)