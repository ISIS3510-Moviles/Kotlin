package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.AlertDomain
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    suspend fun fetchAndSaveRemoteAlerts(): List<AlertDomain>
    fun getLocalAlertsFlow(): Flow<List<AlertDomain>>

    suspend fun getAlertById(id: String): AlertDomain?
    suspend fun updateAlertVotes(id: String, votes: Int): Boolean
    suspend fun createAlert(
        datetime: String,
        icon: String,
        message: String,
        publisherId: String,
        restaurantId: String
    ): AlertDomain
}