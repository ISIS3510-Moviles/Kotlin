package com.example.campusbites.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CampusBuildingDomain(
    val id: Int,
    val name: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double,
    val institutionDomain: InstitutionDomain
)
