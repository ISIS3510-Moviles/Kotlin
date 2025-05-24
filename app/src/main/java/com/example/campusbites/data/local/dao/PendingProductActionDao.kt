package com.example.campusbites.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.campusbites.data.local.entity.PendingProductActionEntity

@Dao
interface PendingProductActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingProductActionEntity): Long

    @Query("SELECT * FROM pending_product_actions ORDER BY timestamp ASC")
    suspend fun getAllActions(): List<PendingProductActionEntity>

    @Query("DELETE FROM pending_product_actions WHERE id = :actionId")
    suspend fun deleteActionById(actionId: Long)

    @Query("DELETE FROM pending_product_actions")
    suspend fun clearAllActions()
}