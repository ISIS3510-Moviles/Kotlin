package com.example.campusbites.domain.model

data class FoodScheduleDomain(
    val id: String,
    val name: String,
    val isActual: Boolean,
    val userDomain: UserDomain,
    val freetimeDomains: List<FreetimeDomain>  = emptyList()
)
