package com.example.campusbites.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.campusbites.data.local.dao.DraftAlertDao
import com.example.campusbites.data.local.dao.PendingProductActionDao
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.dao.FoodTagDao
import com.example.campusbites.data.local.dao.DietaryTagDao
import com.example.campusbites.data.local.dao.IngredientDao
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.data.local.entity.PendingProductActionEntity
import com.example.campusbites.data.local.entity.ReservationEntity
import com.example.campusbites.data.local.entity.FoodTagEntity
import com.example.campusbites.data.local.entity.DietaryTagEntity
import com.example.campusbites.data.local.entity.IngredientEntity

@Database(
    entities = [
        ReservationEntity::class,
        DraftAlertEntity::class,
        PendingProductActionEntity::class,
        FoodTagEntity::class, // Nuevo
        DietaryTagEntity::class, // Nuevo
        IngredientEntity::class // Nuevo
    ],
    version = 6, // Incrementar versión por nuevas entidades
    exportSchema = false
)
@TypeConverters(ListStringConverter::class) // Asegúrate que el TypeConverter esté aquí si es usado por alguna entidad
abstract class AppDatabase : RoomDatabase() {
    abstract fun reservationDao(): ReservationDao
    abstract fun draftAlertDao(): DraftAlertDao
    abstract fun pendingProductActionDao(): PendingProductActionDao
    abstract fun foodTagDao(): FoodTagDao // Nuevo
    abstract fun dietaryTagDao(): DietaryTagDao // Nuevo
    abstract fun ingredientDao(): IngredientDao // Nuevo
}