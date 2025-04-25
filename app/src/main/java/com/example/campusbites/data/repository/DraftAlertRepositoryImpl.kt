package com.example.campusbites.data.repository

import com.example.campusbites.data.local.dao.DraftAlertDao
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.domain.repository.DraftAlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DraftAlertRepositoryImpl @Inject constructor(
    private val draftAlertDao: DraftAlertDao
) : DraftAlertRepository {

    override suspend fun saveDraftAlert(message: String, restaurantId: String, restaurantName: String): Long {
        val draftAlert = DraftAlertEntity(
            message = message,
            restaurantId = restaurantId,
            restaurantName = restaurantName
        )
        return draftAlertDao.insertDraftAlert(draftAlert)
    }

    override suspend fun getLatestDraftAlert(): DraftAlertEntity? {
        return draftAlertDao.getLatestDraftAlert()
    }

    override fun getAllDraftAlerts(): Flow<List<DraftAlertEntity>> {
        return draftAlertDao.getAllDraftAlerts()
    }

    override suspend fun deleteDraftAlert(id: Long) {
        draftAlertDao.deleteDraftAlertById(id)
    }

    override suspend fun hasDraftAlerts(): Boolean {
        return draftAlertDao.getAllDraftAlerts().first().isNotEmpty()
    }
}