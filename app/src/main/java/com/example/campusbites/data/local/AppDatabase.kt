package com.example.campusbites.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.data.local.entity.ReservationEntity

@Database(entities = [ReservationEntity::class, DraftAlertEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reservationDao(): ReservationDao
}