package com.example.campusbites.domain.model

data class Institution(
    val id: String,
    val name: String,
    val description: String,
    val members: List<User>  = emptyList(),
    val buildings: List<CampusBuilding>  = emptyList()
)
