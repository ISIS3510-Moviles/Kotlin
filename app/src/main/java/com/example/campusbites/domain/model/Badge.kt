package com.example.campusbites.domain.model

import java.time.LocalDate

data class Badge(
    val id: String,
    val date: LocalDate,
    val name: String,
    val photo: Photo,
    val description: String,
    val users: List<User>  = emptyList()
)
