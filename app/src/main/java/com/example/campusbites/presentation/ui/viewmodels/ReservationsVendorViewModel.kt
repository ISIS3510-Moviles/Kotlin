package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.local.realm.PendingCancellationLocalDataSource
import com.example.campusbites.data.local.realm.PendingCompletionLocalDataSource
import com.example.campusbites.data.mapper.UserMapper
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.repository.UserRepository
// Asegúrate que las importaciones de UseCases sean correctas según tu estructura de paquetes
import com.example.campusbites.domain.usecase.reservation.vendor.CancelReservationVendorUseCase
import com.example.campusbites.domain.usecase.reservation.vendor.GetReservationsForRestaurantUseCase
import com.example.campusbites.domain.usecase.reservation.vendor.MarkReservationAsCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReservationsVendorViewModel @Inject constructor(
    private val getReservationsForRestaurantUseCase: GetReservationsForRestaurantUseCase,
    private val markReservationAsCompletedUseCase: MarkReservationAsCompletedUseCase,
    private val cancelReservationVendorUseCase: CancelReservationVendorUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userMapper: UserMapper, // <-- Inyecta tu UserMapper
    private val pendingCompletionDataSource: PendingCompletionLocalDataSource,
    private val pendingCancellationDataSource: PendingCancellationLocalDataSource,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    // Define un TAG para tus logs, usualmente el nombre de la clase.
    private val TAG = "ReservationsVendorVM"

    private val _reservationsWithUserDetails = MutableStateFlow<List<ReservationWithUser>>(emptyList())
    val reservationsWithUserDetails: StateFlow<List<ReservationWithUser>> = _reservationsWithUserDetails.asStateFlow()

    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent: SharedFlow<UIEvent> = _uiEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val displayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        Log.d(TAG, "ViewModel initialized") // Log de inicialización del ViewModel
        viewModelScope.launch {
            authRepository.currentUser.first()?.let { user ->
                Log.d(TAG, "Current user found: ${user.id}, Email: ${user.email}")
                val vendorRestaurantId = user.vendorRestaurantId
                Log.d(TAG, "VendorRestaurantId from user: '$vendorRestaurantId'")
                Log.d(TAG, user.toString())

                if (!vendorRestaurantId.isNullOrBlank()) {
                    _isLoading.value = true
                    Log.d(TAG, "Fetching reservations for restaurant ID: $vendorRestaurantId")
                    getReservationsForRestaurantUseCase(vendorRestaurantId)
                        // Log para lo que emite getReservationsForRestaurantUseCase
                        .onEach { reservationsList ->
                            Log.d(TAG, "Reservations received from UseCase for $vendorRestaurantId: Count=${reservationsList.size}")
                            if (reservationsList.isNotEmpty()) {
                                Log.d(TAG, "First reservation example: ID=${reservationsList.first().id}, DateTime=${reservationsList.first().datetime}")
                            }
                        }
                        .flatMapLatest { reservations: List<ReservationDomain> ->
                            if (reservations.isEmpty()) {
                                Log.d(TAG, "No reservations found for $vendorRestaurantId, returning empty list of ReservationWithUser.")
                                flowOf(emptyList<ReservationWithUser>())
                            } else {
                                Log.d(TAG, "Processing ${reservations.size} reservations to fetch user details.")
                                flow<List<ReservationWithUser>> {
                                    val userIds = reservations.map { it.userId }.distinct()
                                    val usersDomainMap = mutableMapOf<String, UserDomain>()

                                    for (userId in userIds) {
                                        try {
                                            val userDTO = userRepository.getUserById(userId)
                                            // Usa tu UserMapper aquí. mapDtoToDomain es suspend.
                                            val userDomain = userMapper.mapDtoToDomain(userDTO)
                                            usersDomainMap[userId] = userDomain
                                        } catch (e: Exception) {
                                            println("ReservationsVendorViewModel: Error mapping DTO to Domain for user ID '$userId': ${e.message}")
                                            // Considera usar userMapper.createFallbackUser(userId) si quieres un placeholder
                                            // o userMapper.mapDtoToDomainFallback(userDTO) si tienes el DTO pero falló el mapeo completo
                                            // Por ahora, si falla, no se añade al mapa y la reserva no mostrará info de usuario.
                                            // Ejemplo de fallback:
                                            // try {
                                            //     val userDTO = userRepository.getUserById(userId)
                                            //     usersDomainMap[userId] = userMapper.mapDtoToDomain(userDTO)
                                            // } catch (eOuter: Exception) {
                                            //     println("ReservationsVendorViewModel: Could not get UserDTO or map for ID '$userId': ${eOuter.message}")
                                            //     // Si getUserById falla, no puedes usar mapDtoToDomainFallback.
                                            //     // Solo createFallbackUser tiene sentido si no tienes DTO.
                                            //     usersDomainMap[userId] = userMapper.createFallbackUser(userId)
                                            // }
                                        }
                                    }

                                    val reservationsWithUsers = reservations.mapNotNull { res ->
                                        usersDomainMap[res.userId]?.let { reservationUserDomain ->
                                            ReservationWithUser(
                                                reservation = res,
                                                userName = reservationUserDomain.name.ifBlank { "Unknown User" }
                                            )
                                        }
                                    }
                                    emit(reservationsWithUsers)
                                }
                            }
                        }
                        .catch { e ->
                            _uiEvent.emit(UIEvent.ShowMessage("Error loading reservations: ${e.localizedMessage ?: "Unknown error"}"))
                            _isLoading.value = false
                        }
                        .collect { detailedReservations ->
                            _reservationsWithUserDetails.value = detailedReservations
                            _isLoading.value = false
                        }
                } else {
                    _uiEvent.emit(UIEvent.ShowMessage("Vendor restaurant not identified. Cannot load reservations."))
                    _isLoading.value = false
                }
            } ?: run {
                _uiEvent.emit(UIEvent.ShowMessage("User not logged in or vendor details missing."))
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            connectivityMonitor.isNetworkAvailable
                .distinctUntilChanged()
                .collect { available ->
                    _networkAvailable.value = available
                    if (available) {
                        flushPendingActions()
                    }
                }
        }
    }

    fun markAsCompleted(reservationId: String) {
        viewModelScope.launch {
            if (connectivityMonitor.isNetworkAvailable.first()) {
                markReservationAsCompletedUseCase(reservationId)
                    .onSuccess {
                        _uiEvent.emit(UIEvent.ShowMessage("Reservation marked as completed."))
                    }
                    .onFailure { e ->
                        pendingCompletionDataSource.add(reservationId)
                        _uiEvent.emit(UIEvent.ShowMessage("Network error. Action queued. ${e.localizedMessage}"))
                    }
            } else {
                pendingCompletionDataSource.add(reservationId)
                _uiEvent.emit(UIEvent.ShowMessage("Offline: Action will be processed when online."))
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            if (connectivityMonitor.isNetworkAvailable.first()) {
                cancelReservationVendorUseCase(reservationId)
                    .onSuccess {
                        _uiEvent.emit(UIEvent.ShowMessage("Reservation cancelled."))
                    }
                    .onFailure { e ->
                        pendingCancellationDataSource.add(reservationId)
                        _uiEvent.emit(UIEvent.ShowMessage("Network error. Cancellation queued. ${e.localizedMessage}"))
                    }
            } else {
                pendingCancellationDataSource.add(reservationId)
                _uiEvent.emit(UIEvent.ShowMessage("Offline: Cancellation will occur when online."))
            }
        }
    }

    private suspend fun flushPendingActions() {
        val pendingCompletions = pendingCompletionDataSource.getAll()
        pendingCompletions.forEach { item ->
            markReservationAsCompletedUseCase(item.reservationId)
                .onSuccess {
                    pendingCompletionDataSource.remove(item)
                    _uiEvent.emit(UIEvent.ShowMessage("Queued completion for ${item.reservationId} processed."))
                }
                .onFailure {
                    _uiEvent.emit(UIEvent.ShowMessage("Failed to process queued completion for ${item.reservationId}."))
                }
        }

        val pendingCancellations = pendingCancellationDataSource.getAll()
        pendingCancellations.forEach { item ->
            cancelReservationVendorUseCase(item.reservationId)
                .onSuccess {
                    pendingCancellationDataSource.remove(item)
                    _uiEvent.emit(UIEvent.ShowMessage("Queued cancellation for ${item.reservationId} processed."))
                }
                .onFailure {
                    _uiEvent.emit(UIEvent.ShowMessage("Failed to process queued cancellation for ${item.reservationId}."))
                }
        }
    }

    data class ReservationWithUser(
        val reservation: ReservationDomain,
        val userName: String
    )

    sealed class UIEvent {
        data class ShowMessage(val message: String) : UIEvent()
    }
}