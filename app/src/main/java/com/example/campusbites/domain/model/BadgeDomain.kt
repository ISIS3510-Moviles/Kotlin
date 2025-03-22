package com.example.campusbites.domain.model

import java.time.LocalDate

data class BadgeDomain(
    val id: String,
    val date: LocalDate,
    val name: String,
    val photoDomain: PhotoDomain,
    val description: String,
    val userDomains: List<UserDomain>  = emptyList()
)
