package com.example.campusbites.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.campusbites.data.local.dao.DraftAlertDao // Importar el DAO correcto
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.data.local.entity.ReservationEntity

@Database(entities = [ReservationEntity::class, DraftAlertEntity::class], version = 3, exportSchema = false) // Mantener version = 2 o incrementarla si hay cambios estructurales
abstract class AppDatabase : RoomDatabase() {
    abstract fun reservationDao(): ReservationDao
    abstract fun draftAlertDao(): DraftAlertDao // Añadir el DAO de DraftAlert
}