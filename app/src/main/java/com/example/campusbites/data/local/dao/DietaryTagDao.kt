package com.example.campusbites.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.campusbites.data.local.entity.DietaryTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DietaryTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<DietaryTagEntity>)

    @Query("SELECT * FROM dietary_tags")
    fun getAll(): Flow<List<DietaryTagEntity>>

    @Query("SELECT * FROM dietary_tags")
    suspend fun getAllOnce(): List<DietaryTagEntity>

    @Query("SELECT * FROM dietary_tags WHERE id = :id")
    suspend fun getById(id: String): DietaryTagEntity?

    @Query("DELETE FROM dietary_tags")
    suspend fun deleteAll()
}