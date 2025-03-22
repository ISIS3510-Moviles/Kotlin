package com.example.campusbites.domain.model

data class CampusBuildingDomain(
    val id: Int,
    val name: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double,
    val institutionDomain: InstitutionDomain
)
