package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.local.dao.DraftAlertDao
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.domain.model.DraftAlert
import com.example.campusbites.domain.repository.DraftAlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DraftAlertRepositoryImpl @Inject constructor(
    private val draftAlertDao: DraftAlertDao // Inyectar el DAO de Room
) : DraftAlertRepository {

    override suspend fun saveDraftAlert(message: String, restaurantId: String, restaurantName: String): String {
        val draftAlertEntity = DraftAlertEntity(
            message = message,
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            createdAt = System.currentTimeMillis()
        )
        val generatedId = draftAlertDao.insertDraftAlert(draftAlertEntity)
        Log.d("DraftAlertRepository", "Draft saved to Room with ID: $generatedId")
        return generatedId.toString() // Convertir Long a String
    }

    override suspend fun updateDraftAlert(draftAlert: DraftAlert) {
        val entity = draftAlert.toEntity()
        draftAlertDao.updateDraftAlert(entity)
        Log.d("DraftAlertRepository", "Draft updated in Room: ID ${draftAlert.id}")
    }

    override suspend fun getLatestDraftAlert(): DraftAlert? {
        return draftAlertDao.getLatestDraftAlert()?.toDomain()
    }

    override fun getAllDraftAlerts(): Flow<List<DraftAlert>> {
        return draftAlertDao.getAllDraftAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDraftAlertById(id: String): DraftAlert? {
        return try {
            draftAlertDao.getDraftAlertById(id.toLong())?.toDomain()
        } catch (e: NumberFormatException) {
            Log.e("DraftAlertRepository", "Invalid ID format for getDraftAlertById: $id", e)
            null
        }
    }

    override suspend fun deleteDraftAlert(id: String) {
        try {
            val longId = id.toLong()
            draftAlertDao.deleteDraftAlertById(longId)
            Log.d("DraftAlertRepository", "Draft deleted from Room by ID: $longId")
        } catch (e: NumberFormatException) {
            Log.e("DraftAlertRepository", "Invalid ID format for deleteDraftAlert: $id", e)
            // Opcionalmente, podrías intentar buscar por otros campos si el ID no es numérico,
            // pero dado que ahora el ID es Long, esto no debería ocurrir si se usa consistentemente.
        }
    }

    override suspend fun hasDraftAlerts(): Boolean {
        return draftAlertDao.getDraftAlertsCount() > 0
    }

    // Funciones de mapeo
    private fun DraftAlertEntity.toDomain(): DraftAlert {
        return DraftAlert(
            id = this.id.toString(), // Convertir Long a String
            message = this.message,
            restaurantId = this.restaurantId,
            restaurantName = this.restaurantName,
            createdAt = this.createdAt
        )
    }

    private fun DraftAlert.toEntity(): DraftAlertEntity {
        return DraftAlertEntity(
            id = this.id.toLongOrNull() ?: 0L, // Convertir String a Long, 0L si es nuevo o inválido
            message = this.message,
            restaurantId = this.restaurantId,
            restaurantName = this.restaurantName,
            createdAt = this.createdAt
        )
    }
}