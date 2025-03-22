package com.example.campusbites.domain.model

import java.time.LocalTime

data class FreetimeDomain(
    val id: String,
    val startHour: LocalTime,
    val endHour: LocalTime,
    val schedule: FoodScheduleDomain,
    val foodpreferences: List<FoodTagDomain>  = emptyList()
)
