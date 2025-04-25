package com.example.campusbites.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draft_alerts")
data class DraftAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val restaurantId: String,
    val restaurantName: String, // Store name for display purposes
    val createdAt: Long = System.currentTimeMillis()
)