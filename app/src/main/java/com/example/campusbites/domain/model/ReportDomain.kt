package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class ReportDomain(
    val id: Int,
    val datetime: LocalDateTime,
    val message: String,
    val isOpen: Boolean,
    val commentDomain: CommentDomain
)