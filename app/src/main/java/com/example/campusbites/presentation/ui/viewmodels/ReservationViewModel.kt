package com.example.campusbites.presentation.ui.viewmodels

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.local.realm.PendingCancellationLocalDataSource
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.usecase.reservation.GetReservationsForUserUseCase
import com.example.campusbites.domain.usecase.reservation.CancelReservationUseCase
import com.example.campusbites.domain.repository.AuthRepository
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import java.time.OffsetDateTime
import kotlin.collections.filter

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val getReservationsForUserUseCase: GetReservationsForUserUseCase,
    private val cancelReservationUseCase: CancelReservationUseCase,
    private val authRepository: AuthRepository,
    private val analytics: FirebaseAnalytics,
    private val pendingDataSource: PendingCancellationLocalDataSource,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    // Estado de reservas
    private val _reservations = MutableStateFlow<List<ReservationDomain>>(emptyList())
    val reservations: StateFlow<List<ReservationDomain>> = _reservations.asStateFlow()

    // Estado de red para la UI
    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    // UI events (una sola vez)
    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent: SharedFlow<UIEvent> = _uiEvent

    init {
        // 1) Cargar reservas actuales y Analytics
        viewModelScope.launch {
            authRepository.currentUser
                .filterNotNull()
                .flatMapLatest { user ->
                    getReservationsForUserUseCase(user.id)
                }
                .collect { list ->
                    _reservations.value = list
                    sendCancellationAnalytics(list)
                }
        }

        // 2) Observar conexión y procesar pendientes al reconectar
        viewModelScope.launch {
            connectivityMonitor.isNetworkAvailable
                .distinctUntilChanged()
                .collect { available ->
                    _networkAvailable.value = available
                    if (available) flushPendingCancellations()
                }
        }
    }

    /** Cancela o encola si no hay red */
    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            val hasNetwork = connectivityMonitor.isNetworkAvailable.first()
            if (hasNetwork) {
                try {
                    cancelReservationUseCase(reservationId)
                } catch (e: Exception) {
                    pendingDataSource.add(reservationId)
                    _uiEvent.emit(UIEvent.ShowMessage("Network error: Your cancellation has been queued."))
                }
            } else {
                pendingDataSource.add(reservationId)
                _uiEvent.emit(UIEvent.ShowMessage("Offline: Cancellation will occur when you return online."))
            }
        }
    }

    /** Reintenta todas las cancelaciones que quedaron en Realm */
    private suspend fun flushPendingCancellations() {
        val pending = pendingDataSource.getAll()
        pending.forEach { item ->
            try {
                cancelReservationUseCase(item.reservationId)
                pendingDataSource.remove(item)
                _uiEvent.emit(UIEvent.ShowMessage("Reservation cancellation “${item.reservationId}” processed successfully."))
            } catch (_: Exception) {
                // si sigue fallando, lo dejaremos en Realm
            }
        }
    }

    /** Envia a Firebase Analytics conteo de cancelaciones por día */
    private fun sendCancellationAnalytics(reservations: List<ReservationDomain>) {
        val cancelled = reservations.filter { it.hasBeenCancelled == true }
        val counts = cancelled.groupBy {
            // Línea modificada:
            OffsetDateTime.parse(it.datetime).toLocalDate().dayOfWeek
        }.mapValues { it.value.size }

        val params = Bundle().apply {
            DayOfWeek.values().forEach { day ->
                val name = when (day) {
                    DayOfWeek.MONDAY    -> "Lunes"
                    DayOfWeek.TUESDAY   -> "Martes"
                    DayOfWeek.WEDNESDAY -> "Miercoles"
                    DayOfWeek.THURSDAY  -> "Jueves"
                    DayOfWeek.FRIDAY    -> "Viernes"
                    DayOfWeek.SATURDAY  -> "Sabado"
                    DayOfWeek.SUNDAY    -> "Domingo"
                }
                putInt(name, counts[day] ?: 0)
            }
        }
        analytics.logEvent("cancelled_reservations_per_day_of_week", params)
    }

    /** Eventos de UI unidireccionales */
    sealed class UIEvent {
        data class ShowMessage(val message: String) : UIEvent()
    }
}
