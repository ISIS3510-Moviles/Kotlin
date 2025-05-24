package com.example.campusbites.data.local

import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.campusbites.data.local.dao.DraftAlertDao
import com.example.campusbites.data.local.dao.PendingProductActionDao // Nuevo
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.data.local.entity.PendingProductActionEntity // Nuevo
import com.example.campusbites.data.local.entity.ReservationEntity

@Database(
    entities = [
        ReservationEntity::class,
        DraftAlertEntity::class,
        PendingProductActionEntity::class // Nuevo
    ],
    version = 4, // Incrementar versión por nuevo entity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reservationDao(): ReservationDao
    abstract fun draftAlertDao(): DraftAlertDao
    abstract fun pendingProductActionDao(): PendingProductActionDao
}