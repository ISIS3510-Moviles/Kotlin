package com.example.campusbites.domain.model

data class DraftAlert(
    val id: String,
    val message: String,
    val restaurantId: String,
    val restaurantName: String,
    val createdAt: Long = System.currentTimeMillis()
)