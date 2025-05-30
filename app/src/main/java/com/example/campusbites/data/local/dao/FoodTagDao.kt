package com.example.campusbites.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.campusbites.data.local.entity.FoodTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<FoodTagEntity>)

    @Query("SELECT * FROM food_tags")
    fun getAll(): Flow<List<FoodTagEntity>>

    @Query("SELECT * FROM food_tags")
    suspend fun getAllOnce(): List<FoodTagEntity>


    @Query("SELECT * FROM food_tags WHERE id = :id")
    suspend fun getById(id: String): FoodTagEntity?

    @Query("DELETE FROM food_tags")
    suspend fun deleteAll()
}