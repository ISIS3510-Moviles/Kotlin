package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable

data class RecommendationDTO (
    val user_id: String,
    val top_n: Int
)