package com.example.campusbites.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.entity.ReservationEntity

@Database(entities = [ReservationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reservationDao(): ReservationDao
}