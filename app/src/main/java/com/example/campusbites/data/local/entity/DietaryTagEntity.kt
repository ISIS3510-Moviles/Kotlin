package com.example.campusbites.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dietary_tags")
data class DietaryTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)