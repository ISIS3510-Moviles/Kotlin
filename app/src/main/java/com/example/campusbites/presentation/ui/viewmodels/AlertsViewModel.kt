package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.repository.AlertRepository
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        fetchAlerts()
    }

    private fun fetchAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val alerts = getAlertsUseCase()

                _uiState.value = _uiState.value.copy(alerts = alerts, isLoading = false)
            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Error desconocido",
                    isLoading = false
                )
            }
        }
    }

    fun upvote(alert: AlertDomain) {
        viewModelScope.launch {

            alertRepository.updateAlertVotes(alert.id, alert.votes + 1)
            fetchAlerts()
        }
    }

    fun downvote(alert: AlertDomain) {
        viewModelScope.launch {

            alertRepository.updateAlertVotes(alert.id, alert.votes - 1)
            fetchAlerts()
        }
    }

}

data class AlertsUiState(
    val alerts: List<AlertDomain> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)