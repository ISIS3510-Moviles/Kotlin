package com.example.campusbites.domain.repository

import com.example.campusbites.data.local.entity.DraftAlertEntity
import kotlinx.coroutines.flow.Flow

interface DraftAlertRepository {
    suspend fun saveDraftAlert(message: String, restaurantId: String, restaurantName: String): Long
    suspend fun getLatestDraftAlert(): DraftAlertEntity?
    fun getAllDraftAlerts(): Flow<List<DraftAlertEntity>>
    suspend fun deleteDraftAlert(id: Long)
    suspend fun hasDraftAlerts(): Boolean
}