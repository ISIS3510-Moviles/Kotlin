package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class InstitutionDTO (
    val id: String,
    val name: String,
    val description: String,
    val buildingsIds: List<String>,
    val membersIds: List<String>
)