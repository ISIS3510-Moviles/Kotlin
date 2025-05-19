package com.example.campusbites.presentation.ui.viewmodels

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.data.preferences.RestaurantPreferencesRepository
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.comment.CreateCommentUseCase
import com.example.campusbites.domain.usecase.product.GetProductsByRestaurantUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.comment.GetCommentsUseCase
import com.example.campusbites.domain.usecase.reservation.CreateReservationUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.domain.repository.AuthRepository
import com.google.firebase.analytics.FirebaseAnalytics
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
import java.util.Calendar
import kotlinx.coroutines.flow.first
import com.example.campusbites.data.local.realm.PendingReservationLocalDataSource // IMPORTAR
import com.example.campusbites.data.local.realm.model.PendingReservationRealmModel // IMPORTAR
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.presentation.ui.viewmodels.FoodDetailViewModel.UiEvent

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
    private val connectivityMonitorProvider: ConnectivityMonitor,
    private val authRepository: AuthRepository,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val restaurantPreferencesRepository: RestaurantPreferencesRepository,
    private val pendingReservationDataSource: PendingReservationLocalDataSource
) : ViewModel() {

    // Para eventos de UI como Snackbars/Toasts desde el ViewModel
    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    // El isOnline que ya tenías, ahora usando el ConnectivityMonitor inyectado
    private val isOnline: StateFlow<Boolean> = connectivityMonitorProvider.isNetworkAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Asumir online inicialmente, se actualizará
        )

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = combine(
        _uiState, // El MutableStateFlow interno
        isOnline, // El StateFlow de conectividad
        restaurantPreferencesRepository.lastSelectedTabIndexFlow
    ) { state, onlineStatus, tabIndex ->
        state.copy(isOnline = onlineStatus, lastSelectedTabIndex = tabIndex)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RestaurantDetailUiState(isOnline = isOnline.value) // Valor inicial de isOnline
    )

    private val _restaurants = MutableStateFlow<List<RestaurantDomain>>(emptyList())
    val restaurants: StateFlow<List<RestaurantDomain>> = _restaurants

    private val _restaurantId = MutableStateFlow<String?>(null)

    init {
        // Observamos cuando cambia el restaurantId y registramos la visita
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

        // Observar la conexión para procesar pendientes al reconectar
        viewModelScope.launch {
            isOnline
                .filter { it } // Solo cuando la red está disponible (true)
                .distinctUntilChanged()
                .collect {
                    // _restaurantId.value?.let { restaurantId -> syncReviews(restaurantId) } // Esto ya lo tenías
                    Log.d("RestaurantDetailVM", "Network is available. Flushing pending reservations.")
                    flushPendingReservations() // LLAMAR A LA NUEVA FUNCIÓN
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
        viewModelScope.launch { // Usar el dispatcher por defecto o IO según la naturaleza de la operación
            if (isOnline.value) {
                try {
                    Log.d("RestaurantDetailVM", "Online. Creating reservation directly for user ${reservation.userId}")
                    createReservationUseCase(reservation)
                    _uiEventFlow.emit(UiEvent.ShowMessage("Reservation confirmed!"))
                } catch (e: Exception) {
                    Log.e("RestaurantDetailVM", "Error creating reservation online, queueing: ${e.message}", e)
                    // Si falla la red a pesar de que isOnline.value era true, encolamos
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
        if (!isOnline.value) return // Doble chequeo

        val pending = pendingReservationDataSource.getAllPendingReservations()
        if (pending.isEmpty()) {
            Log.d("RestaurantDetailVM", "No pending reservations to flush.")
            return
        }
        Log.d("RestaurantDetailVM", "Found ${pending.size} pending reservations to flush.")

        for (pendingItem in pending) {
            val reservationToCreate = ReservationDomain(
                id = pendingItem.reservationIdAttempt, // Usar el ID de intento, backend asignará el final
                restaurantId = pendingItem.restaurantId,
                userId = pendingItem.userId,
                datetime = pendingItem.datetime,
                time = pendingItem.time,
                numberCommensals = pendingItem.numberCommensals,
                isCompleted = false, // Valores por defecto para una nueva reserva
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
                // La reserva permanece en la cola para el próximo intento.
                // Podrías implementar un contador de reintentos o una estrategia de backoff.
                // Si un error es persistente (ej. datos inválidos), podría quedarse encolada indefinidamente.
                break // Detener el flush actual si una falla, para reintentar todo en la próxima conexión.
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

    /**
     * Registra un evento "restaurant_visit" en Firebase Analytics
     * con el id del restaurante y el día de la semana actual.
     */
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
        Log.d("RestaurantDetailVM", "Logged visit event: \$params")
    }

    // Clase sellada para eventos de UI (si no la tienes ya global)
    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        // Podrías añadir más tipos de eventos si es necesario
    }
}

data class RestaurantDetailUiState(
    val restaurant: RestaurantDomain? = null,
    val products: List<ProductDomain> = emptyList(),
    val reviews: List<CommentDomain> = emptyList(),
    val popularProducts: List<ProductDomain> = emptyList(),
    val under20Products: List<ProductDomain> = emptyList(),
    val isLoadingNetwork: Boolean = false,
    val isOnline: Boolean = false,
    val lastSelectedTabIndex: Int = 0 // Añadir estado para el tab
)