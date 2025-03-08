package com.example.campusbites.domain.model

data class FoodSchedule(
    val id: String,
    val name: String,
    val isActual: Boolean,
    val user: User,
    val freetimes: List<Freetime>  = emptyList()
)
