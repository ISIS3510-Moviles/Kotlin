package com.example.campusbites.domain.model

import java.time.LocalTime

data class Freetime(
    val id: String,
    val startHour: LocalTime,
    val endHour: LocalTime,
    val schedule: FoodSchedule,
    val foodpreferences: List<FoodTag>  = emptyList()
)
