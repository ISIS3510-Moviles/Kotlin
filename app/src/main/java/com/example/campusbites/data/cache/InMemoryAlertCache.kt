package com.example.campusbites.data.cache // O donde prefieras ponerlo

import android.util.Log
import com.example.campusbites.domain.model.AlertDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryAlertCache @Inject constructor() {

    private val _alerts = MutableStateFlow<List<AlertDomain>>(emptyList())
    val alerts: StateFlow<List<AlertDomain>> = _alerts.asStateFlow()

    fun updateAlerts(newAlerts: List<AlertDomain>) {
        Log.d("InMemoryAlertCache", "Updating cache with ${newAlerts.size} alerts.")
        _alerts.value = newAlerts
    }

    fun getAlerts(): List<AlertDomain> {
        return _alerts.value
    }

    fun clearCache() {
        Log.d("InMemoryAlertCache", "Clearing alert cache.")
        _alerts.value = emptyList()
    }

    init {
        Log.d("InMemoryAlertCache", "InMemoryAlertCache Singleton instance created: ${this.hashCode()}")
    }
}