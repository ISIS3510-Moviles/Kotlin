package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InstitutionDomain(
    val id: String,
    val name: String,
    val description: String,
    val members: List<UserDomain>  = emptyList(),
    val buildings: List<CampusBuildingDomain>  = emptyList()
)
