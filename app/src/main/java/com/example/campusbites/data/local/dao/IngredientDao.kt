package com.example.campusbites.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.campusbites.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<IngredientEntity>)

    @Query("SELECT * FROM ingredients")
    fun getAll(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients")
    suspend fun getAllOnce(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getById(id: String): IngredientEntity?

    @Update
    suspend fun update(ingredient: IngredientEntity)

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()
}