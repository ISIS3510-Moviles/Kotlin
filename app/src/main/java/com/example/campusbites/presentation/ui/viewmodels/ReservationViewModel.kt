package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.usecase.reservation.GetReservationsForUserUseCase
import com.example.campusbites.domain.usecase.reservation.CancelReservationUseCase
import com.example.campusbites.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val getReservationsForUserUseCase: GetReservationsForUserUseCase,
    private val cancelReservationUseCase: CancelReservationUseCase,
    private val authRepository: AuthRepository
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
}