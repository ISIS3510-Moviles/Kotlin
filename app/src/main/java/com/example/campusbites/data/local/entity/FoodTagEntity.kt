package com.example.campusbites.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_tags")
data class FoodTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)