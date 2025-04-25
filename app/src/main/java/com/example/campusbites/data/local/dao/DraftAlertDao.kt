package com.example.campusbites.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.campusbites.data.local.entity.DraftAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftAlertDao {
    @Insert
    suspend fun insertDraftAlert(draftAlert: DraftAlertEntity): Long

    @Query("SELECT * FROM draft_alerts ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestDraftAlert(): DraftAlertEntity?

    @Query("SELECT * FROM draft_alerts ORDER BY createdAt DESC")
    fun getAllDraftAlerts(): Flow<List<DraftAlertEntity>>

    @Delete
    suspend fun deleteDraftAlert(draftAlert: DraftAlertEntity)

    @Query("DELETE FROM draft_alerts WHERE id = :id")
    suspend fun deleteDraftAlertById(id: Long)
}