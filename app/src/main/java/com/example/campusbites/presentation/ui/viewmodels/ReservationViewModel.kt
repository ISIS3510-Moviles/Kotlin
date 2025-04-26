package com.example.campusbites.presentation.ui.viewmodels

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.usecase.reservation.GetReservationsForUserUseCase
import com.example.campusbites.domain.usecase.reservation.CancelReservationUseCase
import com.example.campusbites.domain.repository.AuthRepository
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import java.time.DayOfWeek
import java.time.LocalDate

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val getReservationsForUserUseCase: GetReservationsForUserUseCase,
    private val cancelReservationUseCase: CancelReservationUseCase,
    private val authRepository: AuthRepository,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    private val _reservations = MutableStateFlow<List<ReservationDomain>>(emptyList())
    val reservations: StateFlow<List<ReservationDomain>> = _reservations.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser
                .filterNotNull()
                .collectLatest { user ->
                    getReservationsForUserUseCase(user.id).collect {
                        _reservations.value = it
                        sendCancellationAnalytics(it)
                    }
                }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            try {
                cancelReservationUseCase(reservationId)
            } catch (e: Exception) {
            }
        }
    }

    private fun sendCancellationAnalytics(reservations: List<ReservationDomain>) {
        val cancelledReservations = reservations.filter { it.hasBeenCancelled == true }

        // Agrupar por día de la semana
        val dayCounts = cancelledReservations.groupBy {
            LocalDate.parse(it.datetime).dayOfWeek
        }.mapValues { it.value.size }

        // Mapear a nombres en español
        val params = Bundle().apply {
            DayOfWeek.values().forEach { day ->
                val spanishDayName = when (day) {
                    DayOfWeek.MONDAY -> "Lunes"
                    DayOfWeek.TUESDAY -> "Martes"
                    DayOfWeek.WEDNESDAY -> "Miercoles"
                    DayOfWeek.THURSDAY -> "Jueves"
                    DayOfWeek.FRIDAY -> "Viernes"
                    DayOfWeek.SATURDAY -> "Sabado"
                    DayOfWeek.SUNDAY -> "Domingo"
                }
                putInt(spanishDayName, dayCounts[day] ?: 0)
            }
        }

        // Enviar evento a Analytics
        analytics.logEvent("cancelled_reservations_per_day_of_week", params)
    }
}