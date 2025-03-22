package com.example.campusbites.domain.model

data class InstitutionDomain(
    val id: String,
    val name: String,
    val description: String,
    val members: List<UserDomain>  = emptyList(),
    val buildings: List<CampusBuildingDomain>  = emptyList()
)
