package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.DraftAlert
import kotlinx.coroutines.flow.Flow

interface DraftAlertRepository {
    suspend fun saveDraftAlert(message: String, restaurantId: String, restaurantName: String): String
    suspend fun getLatestDraftAlert(): DraftAlert?
    fun getAllDraftAlerts(): Flow<List<DraftAlert>>
    suspend fun deleteDraftAlert(id: String)
    suspend fun hasDraftAlerts(): Boolean
}