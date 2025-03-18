package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.usecase.alert.GetAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase
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

                Log.i("prueba", "Se han obtenido ${alerts.size} alertas")

                _uiState.value = _uiState.value.copy(alerts = alerts, isLoading = false)
            } catch (e: Exception) {
                Log.i("prueba", "No funciono lo del state")
                Log.i("prueba", "Error al obtener alertas: ${e.localizedMessage}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Error desconocido",
                    isLoading = false
                )
            }
        }
    }
}

data class AlertsUiState(
    val alerts: List<AlertDomain> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)