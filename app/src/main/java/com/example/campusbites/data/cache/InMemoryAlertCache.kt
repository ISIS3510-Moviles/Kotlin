package com.example.campusbites.data.cache

import android.util.ArrayMap
import android.util.Log
import com.example.campusbites.domain.model.AlertDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@Singleton
class InMemoryAlertCache @Inject constructor(
    private val applicationScope: CoroutineScope
) {

    private val _alerts = MutableStateFlow<ArrayMap<String, AlertDomain>>(ArrayMap())

    val alerts: StateFlow<List<AlertDomain>> =
        _alerts.asStateFlow()
            .map { it.values.toList() }
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    fun updateAlerts(newAlerts: List<AlertDomain>) {
        Log.d("InMemoryAlertCache", "Updating cache with ${newAlerts.size} alerts.")
        val newMap = ArrayMap<String, AlertDomain>()
        newAlerts.forEach { alert ->
            newMap[alert.id] = alert
        }
        _alerts.value = newMap
    }

    fun getAlerts(): List<AlertDomain> {
        return _alerts.value.values.toList()
    }

    fun getAlertById(alertId: String): AlertDomain? {
        return _alerts.value[alertId]
    }

    fun clearCache() {
        Log.d("InMemoryAlertCache", "Clearing alert cache.")
        _alerts.value = ArrayMap()
    }

    init {
        Log.d("InMemoryAlertCache", "InMemoryAlertCache Singleton instance created: ${this.hashCode()}")
    }
}