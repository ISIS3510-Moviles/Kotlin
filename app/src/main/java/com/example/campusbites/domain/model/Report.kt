package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class Report(
    val id: Int,
    val datetime: LocalDateTime,
    val message: String,
    val isOpen: Boolean,
    val comment: Comment
)