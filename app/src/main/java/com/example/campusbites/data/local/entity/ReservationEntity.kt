package com.example.campusbites.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val restaurantId: String,
    val datetime: String,
    val time: String,
    val numberCommensals: Int,
    val isCompleted: Boolean,
    val hasBeenCancelled: Boolean?,
)