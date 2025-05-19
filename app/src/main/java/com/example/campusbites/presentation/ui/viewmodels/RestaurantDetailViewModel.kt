package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.cache.RestaurantLruCache
import com.example.campusbites.data.local.realm.PendingReservationLocalDataSource
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.data.preferences.RestaurantPreferencesRepository
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.usecase.comment.CreateCommentUseCase
import com.example.campusbites.domain.usecase.comment.GetCommentsUseCase
import com.example.campusbites.domain.usecase.product.GetProductsByRestaurantUseCase
import com.example.campusbites.domain.usecase.reservation.CreateReservationUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.take


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase,
    private val getProductsByRestaurantUseCase: GetProductsByRestaurantUseCase,
    private val getReviewsByRestaurantUseCase: GetCommentsUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase, // No se usa directamente aquí, pero se mantiene por si acaso
    private val updateUserUseCase: UpdateUserUseCase, // Para guardar/desguardar restaurantes
    private val createReservationUseCase: CreateReservationUseCase,
    private val createCommentUseCase: CreateCommentUseCase,
    private val homeDataRepository: HomeDataRepository,
    private val connectivityMonitorProvider: ConnectivityMonitor,
    private val authRepository: AuthRepository,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val restaurantPreferencesRepository: RestaurantPreferencesRepository,
    private val pendingReservationDataSource: PendingReservationLocalDataSource,
    private val restaurantLruCache: RestaurantLruCache // Inyectar el nuevo LRU Cache
) : ViewModel() {

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val isOnline: StateFlow<Boolean> = connectivityMonitorProvider.isNetworkAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = combine(
        _uiState,
        isOnline,
        restaurantPreferencesRepository.lastSelectedTabIndexFlow
    ) { state, onlineStatus, tabIndex ->
        state.copy(isOnline = onlineStatus, lastSelectedTabIndex = tabIndex)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RestaurantDetailUiState(isOnline = isOnline.value)
    )

    private val _restaurantId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            _restaurantId.filterNotNull().collect { id ->
                logVisitEvent(id)
            }
        }
        viewModelScope.launch {
            combine(_restaurantId, isOnline) { restaurantId, online ->
                Pair(restaurantId, online)
            }.collectLatest { (restaurantId, online) ->
                _uiState.update { it.copy(isOnline = online) }
                if (restaurantId != null) {
                    loadRestaurantDetails(restaurantId) // Carga inicial desde caches
                    if (online) { // Si hay red, intentar refrescar desde la API
                        fetchRestaurantDetailsFromServer(restaurantId) // Renombrado para claridad
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
                        syncReviews(restaurantId) // Sincronizar reviews
                    }
                    Log.d("RestaurantDetailVM", "Network is available. Flushing pending reservations.")
                    flushPendingReservations() // Sincronizar reservas pendientes
                }
        }
    }

    fun saveSelectedTabIndex(index: Int) {
        viewModelScope.launch {
            restaurantPreferencesRepository.saveLastSelectedTabIndex(index)
        }
    }

    fun loadRestaurantDetails(restaurantId: String) {
        _restaurantId.value = restaurantId
        viewModelScope.launch {
            // 1. Intentar desde LRU Cache
            val lruCachedRestaurant = restaurantLruCache.get(restaurantId)
            if (lruCachedRestaurant != null) {
                Log.d("RestaurantDetailVM", "Restaurant $restaurantId loaded from LRU Cache.")
                // Asumimos que si está en LRU, sus productos también están "actuales" o se recargarán si es necesario.
                // Podríamos cargar productos desde HomeDataRepository aquí si LRU solo guarda RestaurantDomain.
                // Por simplicidad, actualizamos el estado y dejamos que fetchRestaurantDetailsFromServer actualice si hay red.
                _uiState.update { currentState ->
                    currentState.copy(
                        restaurant = lruCachedRestaurant,
                        // Los productos se cargarán/actualizarán en fetchRestaurantDetailsFromServer o desde HomeDataRepository
                    )
                }
                // Si se quiere cargar productos asociados inmediatamente:
                loadProductsForRestaurant(lruCachedRestaurant)
            }

            // 2. Intentar desde HomeDataRepository (DataStore) si no está en LRU
            if (lruCachedRestaurant == null) {
                val homeCachedRestaurant = homeDataRepository.nearbyRestaurantsFlow.firstOrNull()?.find { it.id == restaurantId }
                if (homeCachedRestaurant != null) {
                    Log.d("RestaurantDetailVM", "Restaurant $restaurantId loaded from HomeDataRepository.")
                    restaurantLruCache.put(restaurantId, homeCachedRestaurant) // Añadir a LRU
                    _uiState.update { currentState ->
                        currentState.copy(restaurant = homeCachedRestaurant)
                    }
                    loadProductsForRestaurant(homeCachedRestaurant)
                }
            }

            // 3. Si hay red, siempre intentar obtener la versión más reciente (se llama desde el colector de _restaurantId e isOnline)
            // fetchRestaurantDetailsFromServer(restaurantId) se llamará si hay red.
            // Si no hay red y no se encontró en ningún caché, el estado de carga se manejará.
            if (_uiState.value.restaurant == null && !isOnline.value) {
                _uiState.update { it.copy(errorMessage = "Restaurant details not found in cache and you are offline.") }
            }
        }
    }

    private suspend fun loadProductsForRestaurant(restaurant: RestaurantDomain) {
        val cachedProducts = homeDataRepository.allProductsFlow.firstOrNull()?.filter { product ->
            restaurant.productsIds.contains(product.id) || product.restaurantId == restaurant.id // Mejorar filtro
        } ?: emptyList()
        Log.d("RestaurantDetailVM", "Products for ${restaurant.id} loaded from HomeDataRepository: ${cachedProducts.size}")
        _uiState.update { currentState ->
            currentState.copy(
                products = cachedProducts,
                popularProducts = cachedProducts.sortedByDescending { it.rating }.take(5),
                under20Products = cachedProducts.filter { it.price <= 20000 }
            )
        }
    }


    private suspend fun fetchRestaurantDetailsFromServer(restaurantId: String) {
        if (!isOnline.value) {
            Log.d("RestaurantDetailVM", "Offline, skipping fetchRestaurantDetailsFromServer for $restaurantId")
            return
        }
        _uiState.update { it.copy(isLoadingNetwork = true) }
        try {
            val restaurant = getRestaurantByIdUseCase(restaurantId) // Puede ser null si no existe
            val products = if (restaurant != null) getProductsByRestaurantUseCase(restaurantId) else emptyList()

            if (restaurant != null) {
                restaurantLruCache.put(restaurantId, restaurant) // Actualizar LRU Cache
                Log.d("RestaurantDetailVM", "Restaurant $restaurantId fetched from network and updated in LRU.")
                // Opcional: Actualizar HomeDataRepository si es necesario (podría ser complejo manejar listas)
                val currentNearby = homeDataRepository.nearbyRestaurantsFlow.first().toMutableList()
                val index = currentNearby.indexOfFirst { it.id == restaurantId }
                if (index != -1) {
                    currentNearby[index] = restaurant
                    homeDataRepository.saveNearbyRestaurants(currentNearby)
                }


                val currentAllProducts = homeDataRepository.allProductsFlow.first().toMutableList()
                currentAllProducts.removeAll { it.restaurantId == restaurantId }
                currentAllProducts.addAll(products)
                homeDataRepository.saveAllProducts(currentAllProducts)

            } else {
                Log.w("RestaurantDetailVM", "Restaurant $restaurantId not found on server.")
            }

            _uiState.update { currentState ->
                currentState.copy(
                    restaurant = restaurant, // Puede ser null si la API devuelve 404, etc.
                    products = products,
                    popularProducts = products.sortedByDescending { it.rating }.take(5),
                    under20Products = products.filter { it.price <= 20000 },
                    isLoadingNetwork = false
                )
            }
        } catch (e: Exception) {
            Log.e("RestaurantDetailVM", "Error fetching restaurant details from network for $restaurantId: ${e.message}", e)
            _uiState.update { it.copy(isLoadingNetwork = false, errorMessage = "Failed to refresh restaurant details.") }
        }
    }


    private fun syncReviews(restaurantId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Esta llamada actualiza el InMemoryReviewCache, que es observado por GetCommentsUseCase
                getReviewsByRestaurantUseCase(restaurantId).firstOrNull()
                Log.d("RestaurantDetailVM", "Reviews synced for restaurant $restaurantId")
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error syncing reviews: ${e.message}")
            }
        }
    }

    suspend fun onSaveClick(user: UserDomain) { // Este método se usa para suscribir/desuscribir
        try {
            updateUserUseCase(user.id, user)
            // El AuthViewModel es la fuente de verdad del usuario,
            // así que el cambio se reflejará a través de su StateFlow.
        } catch (e: Exception) {
            Log.e("RestaurantDetailViewModel", "Error updating user for subscription: ${e.message}")
            _uiEventFlow.emit(UiEvent.ShowMessage("Error updating subscription status."))
        }
    }

    fun createReservation(reservation: ReservationDomain) {
        viewModelScope.launch {
            if (isOnline.value) {
                try {
                    Log.d("RestaurantDetailVM", "Online. Creating reservation directly for user ${reservation.userId}")
                    createReservationUseCase(reservation)
                    _uiEventFlow.emit(UiEvent.ShowMessage("Reservation confirmed!"))
                } catch (e: Exception) {
                    Log.e("RestaurantDetailVM", "Error creating reservation online, queueing: ${e.message}", e)
                    pendingReservationDataSource.addPendingReservation(reservation)
                    _uiEventFlow.emit(UiEvent.ShowMessage("Network error. Your reservation has been queued."))
                }
            } else {
                Log.d("RestaurantDetailVM", "Offline. Queueing reservation for user ${reservation.userId}")
                pendingReservationDataSource.addPendingReservation(reservation)
                _uiEventFlow.emit(UiEvent.ShowMessage("You're offline. Reservation will be made when you're back online."))
            }
        }
    }

    private suspend fun flushPendingReservations() {
        if (!isOnline.value) return

        val pending = pendingReservationDataSource.getAllPendingReservations()
        if (pending.isEmpty()) {
            Log.d("RestaurantDetailVM", "No pending reservations to flush.")
            return
        }
        Log.d("RestaurantDetailVM", "Found ${pending.size} pending reservations to flush.")

        for (pendingItem in pending) {
            val reservationToCreate = ReservationDomain(
                id = pendingItem.reservationIdAttempt,
                restaurantId = pendingItem.restaurantId,
                userId = pendingItem.userId,
                datetime = pendingItem.datetime,
                time = pendingItem.time,
                numberCommensals = pendingItem.numberCommensals,
                isCompleted = false,
                hasBeenCancelled = false
            )
            try {
                Log.d("RestaurantDetailVM", "Flushing: Creating pending reservation for user ${reservationToCreate.userId}")
                createReservationUseCase(reservationToCreate)
                pendingReservationDataSource.removePendingReservation(pendingItem.id)
                _uiEventFlow.emit(UiEvent.ShowMessage("Pending reservation for ${pendingItem.datetime.substringBefore('T')} processed successfully."))
                Log.i("RestaurantDetailVM", "Successfully processed pending reservation: ${pendingItem.id}")
            } catch (e: Exception) {
                Log.e("RestaurantDetailVM", "Failed to process pending reservation ${pendingItem.id}: ${e.message}", e)
                break
            }
        }
    }

    fun createReview(comment: CommentDomain) {
        viewModelScope.launch { // Usar Dispatchers.IO si la operación es de red/DB
            if (!isOnline.value) {
                // TODO: Implementar guardado de reviews offline si se desea
                _uiEventFlow.emit(UiEvent.ShowMessage("You are offline. Review cannot be submitted now."))
                Log.w("RestaurantDetailVM", "Offline. Review submission for ${comment.restaurantDomain?.id} skipped.")
                return@launch
            }
            try {
                val created = createCommentUseCase(comment)
                // El CommentRepository debería actualizar el InMemoryReviewCache,
                // lo que debería refrescar la lista de reviews observada.
                _uiEventFlow.emit(UiEvent.ShowMessage("Review submitted successfully!"))
                Log.d("RestaurantDetailVM", "Review created: ${created.id}")
            } catch (e: Exception) {
                Log.e("RestaurantDetailViewModel", "Error creating review: ${e.message}")
                _uiEventFlow.emit(UiEvent.ShowMessage("Failed to submit review."))
            }
        }
    }

    private fun logVisitEvent(restaurantId: String) {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { value ->
            when (value) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                Calendar.SUNDAY -> "Sunday"
                else -> "Unknown"
            }
        }
        val params = bundleOf(
            "restaurant_id" to restaurantId,
            "day_of_week" to dayOfWeek
        )
        firebaseAnalytics.logEvent("restaurant_visit", params)
        Log.d("RestaurantDetailVM", "Logged visit event: $params")
    }

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
    }
}

data class RestaurantDetailUiState(
    val restaurant: RestaurantDomain? = null,
    val products: List<ProductDomain> = emptyList(),
    val reviews: List<CommentDomain> = emptyList(),
    val popularProducts: List<ProductDomain> = emptyList(),
    val under20Products: List<ProductDomain> = emptyList(),
    val isLoadingNetwork: Boolean = false,
    val isOnline: Boolean = true, // Default to true, will be updated by connectivity monitor
    val lastSelectedTabIndex: Int = 0,
    val errorMessage: String? = null // Para mostrar errores generales
)