package com.example.campusbites.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.campusbites.data.local.entity.DraftAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftAlert(draftAlert: DraftAlertEntity): Long // Devuelve el ID generado

    @Update
    suspend fun updateDraftAlert(draftAlert: DraftAlertEntity)

    @Query("SELECT * FROM draft_alerts ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestDraftAlert(): DraftAlertEntity?

    @Query("SELECT * FROM draft_alerts ORDER BY createdAt DESC")
    fun getAllDraftAlerts(): Flow<List<DraftAlertEntity>>

    @Query("SELECT * FROM draft_alerts WHERE id = :id")
    suspend fun getDraftAlertById(id: Long): DraftAlertEntity?

    @Delete
    suspend fun deleteDraftAlert(draftAlert: DraftAlertEntity)

    @Query("DELETE FROM draft_alerts WHERE id = :id")
    suspend fun deleteDraftAlertById(id: Long)

    @Query("SELECT COUNT(*) FROM draft_alerts")
    suspend fun getDraftAlertsCount(): Int
}