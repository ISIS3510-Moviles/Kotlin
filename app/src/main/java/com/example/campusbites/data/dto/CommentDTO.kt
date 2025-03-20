package com.example.campusbites.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentDTO(
    val id: String,
    val message: String,
    val rating: Int,
    val likes: Int? = 0,
    val isVisible: Boolean,
    val photos: List<String>? = emptyList(),
    val responseToId: String? = null,
    val restaurantId: String? = null,
    val productId: String? = null,
    val reportsIds: List<String>? = emptyList(),
    val responsesIds: List<String>? = emptyList(),
    val authorId: String,
    val datetime: String? = null
)
